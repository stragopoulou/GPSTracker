package com.example.gpstracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service implements LocationListener {

	private LocationManager locationManager;
	private String gps_provider;
	private Location _location = null;
	private Location old_location = null;
	private Location mLastLocation = null;
	double mLastLocationMillis = 0.0, startLat = 0.0, startLon = 0.0;
	boolean flag = false;
	boolean GPSfixed = false;
	private String isGpsFix = "no";
	private String filename;

	final Handler handler = new Handler();
	Runnable runnable;

	ArrayList<String> DataToSend = new ArrayList<String>();

	private static boolean isRunning = false;

	private List<Messenger> mClients = new ArrayList<Messenger>();
	private int minCount = 0;
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_SET_INT_VALUE = 3;
	public static final int MSG_SET_STRING_VALUE = 4;

	private final Messenger mMessenger = new Messenger(
			new IncomingMessageHandler()); // Target we publish for clients to
											// send messages to IncomingHandler.

	private static final String LOGTAG = "MyService";
	private RandomForest classifier;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOGTAG, "Service Started.");
		isRunning = true;
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		gps_provider = LocationManager.GPS_PROVIDER;
		locationManager.requestLocationUpdates(gps_provider, 1000, 1, this);
		locationManager.addGpsStatusListener(onGpsStatusChange);
		onGpsStatusChange
				.onGpsStatusChanged(GpsStatus.GPS_EVENT_SATELLITE_STATUS);

		DataToSend = new ArrayList<String>();

		Calendar c = new GregorianCalendar();
		Date d = c.getTime();
		filename = String.valueOf(d);
		createFileRoute();// it creates the file
		trainSystem();
		runnable = new Runnable() {
			public void run() {
				_location = locationManager.getLastKnownLocation(gps_provider);
				if (_location != null)
					onLocationChanged(_location);
				addLocationToList(_location);
				minCount++;
				if (minCount == 20) {
					writeDataToFile();
					minCount = 0;
				}
				handler.removeCallbacks(this);
				handler.postDelayed(this, 30000);
			}
		};
		handler.removeCallbacks(runnable);
		handler.postDelayed(runnable, 30000);
	}

	GpsStatus.Listener onGpsStatusChange = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int event) {
			switch (event) {
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				if (mLastLocation != null)
					GPSfixed = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 5000;

				if (GPSfixed) // A fix has been acquired.
					isGpsFix = "yes";
				else
					// The fix has been lost.
					isGpsFix = "no";
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				isGpsFix = "yes"; // First fix has been acquired.
				break;
			}
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
		return START_STICKY; // Run until explicitly stopped.
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(LOGTAG, "onBind");
		return mMessenger.getBinder();
	}

	/**
	 * Send the data to all clients.
	 * 
	 * @param intvaluetosend
	 *            The value to send.
	 */
	private void sendLocationToMap(String instance) {
		Iterator<Messenger> messengerIterator = mClients.iterator();
		while (messengerIterator.hasNext()) {
			Messenger messenger = messengerIterator.next();
			try {
				// Send data as a String
				Bundle bundle = new Bundle();
				bundle.putString("str", instance);
				Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
				msg.setData(bundle);
				messenger.send(msg);

			} catch (RemoteException e) {
				// The client is dead. Remove it from the list.
				mClients.remove(messenger);
			}
		}
	}

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks(runnable);
		writeDataToFile();
		Log.i("MyService", "Service Stopped.");
		isRunning = false;
	}

	// ////////////////////////////////////////
	// Nested classes
	// ///////////////////////////////////////

	/**
	 * Handle incoming messages from MainActivity
	 */
	private class IncomingMessageHandler extends Handler { // Handler of
															// incoming messages
															// from clients.
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOGTAG, "handleMessage: " + msg.what);
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void addLocationToList(Location cur_location) {

		if (!flag) {
			old_location = cur_location;
			startLat = cur_location.getLatitude();
			startLon = cur_location.getLongitude();
			flag = true;
		} else {
			double destLat = cur_location.getLatitude();
			double destLon = cur_location.getLongitude();
			double distance = getDistance(startLat, startLon, destLat, destLon);
			double dt = (cur_location.getTime() - old_location.getTime()) / 1000;
			double current_speed = distance / dt;
			if (Double.isNaN(current_speed)||(current_speed < 0.001))
				current_speed = 0.0;	
			Date currentDate = new Date();
			Calendar cal = Calendar.getInstance();
			int hour = currentDate.getHours();
			int minutes = currentDate.getMinutes();
			int seconds = currentDate.getSeconds();
			int year = currentDate.getYear() + 1900;
			int month = currentDate.getMonth() + 1;
			int day = cal.get(Calendar.DAY_OF_MONTH);
			String timestamp = String.valueOf(year) + "-"
					+ String.valueOf(month) + "-" + String.valueOf(day) + " "
					+ String.valueOf(hour) + ":" + String.valueOf(minutes)
					+ ":" + String.valueOf(seconds);
			String move_type = "?";
			String record = String.valueOf(destLat) + ","
					+ String.valueOf(destLon) + ","
					+ String.valueOf(current_speed) + "," + "\""
					+ String.valueOf(timestamp) + "\"" + ","
					+ String.valueOf(move_type) + ","
					+ String.valueOf(isGpsFix);
			String type = classifyInstance(record);
			record = record.replace("?", type);
			DataToSend.add(record);
			String curPos = String.valueOf(destLat) + ","
					+ String.valueOf(destLon) + "," + type;
			sendLocationToMap(curPos);
			startLat = cur_location.getLatitude();
			startLon = cur_location.getLongitude();
			old_location = cur_location;
		}

	}

	public static float getDistance(double startLati, double startLongi,
			double goalLati, double goalLongi) {
		float[] resultArray = new float[99];
		Location.distanceBetween(startLati, startLongi, goalLati, goalLongi,
				resultArray);
		return resultArray[0];
	}

	public void trainSystem() {

		BufferedReader reader = null;
		Instances data = null;
		try {
			reader = new BufferedReader(new InputStreamReader(getResources().getAssets().open("training_data.arff")));
			data = new Instances(reader);
			reader.close();
			data.setClassIndex(data.numAttributes() - 2);
			classifier = new RandomForest();
			classifier.buildClassifier(data);			
		} catch (FileNotFoundException e) {
			Toast.makeText(getApplicationContext(), "File not Found.!",
					Toast.LENGTH_LONG).show();
			 Log.e("GPSTracker", "fnfexception", e);
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "IO Exception.!",
					Toast.LENGTH_LONG).show();
			 Log.e("GPSTracker", "ioexception", e);
		} catch (Exception e) {
			Log.e("GPSTracker", "exception", e);
			Toast.makeText(getApplicationContext(), "Exception.!",
					Toast.LENGTH_LONG).show();
		}
	}

	public String classifyInstance(String newInst) {

		File f = null;
		String type = null;
		try {
			f = new File("/data/data/com.example.gpstracker/tmp.arff");
			f.createNewFile();

			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("@relation gps_tracking");
			bw.newLine();
			bw.newLine();
			bw.write("@attribute Longtitude numeric");
			bw.newLine();
			bw.write("@attribute Latitude numeric");
			bw.newLine();
			bw.write("@attribute CurrentSpeed numeric");
			bw.newLine();
			bw.write("@attribute Timestamp date \"yyyy-MM-dd HH:mm:ss\"");
			bw.newLine();
			bw.write("@attribute MoveType {Walking,Running,Biking,Driving,Metro,Bus,Motionless}");
			bw.newLine();
			bw.write("@attribute IsGpsFixed {yes,no}");
			bw.newLine();
			bw.newLine();
			bw.write("@data");
			bw.newLine();
			bw.write(newInst);
			bw.close();

			// load unlabeled data
			Instances unlabeled = new Instances(new BufferedReader(new FileReader("/data/data/com.example.gpstracker/tmp.arff")));
			// set class attribute
			unlabeled.setClassIndex(unlabeled.numAttributes() - 2);

			// label instances
			double clsLabel = classifier.classifyInstance(unlabeled.instance(0));
			type = unlabeled.classAttribute().value((int) clsLabel);
			boolean deleted = f.delete();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return type;
	}

	public void createFileRoute() {

		File f = null;
		try {
			f = new File("/data/data/com.example.gpstracker/" + filename
					+ ".arff");
			f.createNewFile();

			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("@relation gps_tracking");
			bw.newLine();
			bw.newLine();
			bw.write("@attribute Longtitude numeric");
			bw.newLine();
			bw.write("@attribute Latitude numeric");
			bw.newLine();
			bw.write("@attribute CurrentSpeed numeric");
			bw.newLine();
			bw.write("@attribute Timestamp date \"yyyy-MM-dd HH:mm:ss\"");
			bw.newLine();
			bw.write("@attribute MoveType {Walking,Running,Biking,Driving,Metro,Bus,Motionless}");
			bw.newLine();
			bw.write("@attribute IsGpsFixed {yes,no}");
			bw.newLine();
			bw.newLine();
			bw.write("@data");
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(LocationService.this, "Error creating file.!",
					Toast.LENGTH_LONG).show();
		}
	}

	public void writeDataToFile() {

		try {
			File f = new File("/data/data/com.example.gpstracker/", filename
					+ ".arff");
			BufferedWriter bW = new BufferedWriter(new FileWriter(f, true));
			// bW.flush();
			for (int i = 0; i < DataToSend.size(); i++) {
				bW.write(DataToSend.get(i));
				bW.newLine();
			}
			bW.close();
			DataToSend = new ArrayList<String>();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(LocationService.this, "Error writing file.!",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onLocationChanged(Location location) {

		if (location == null)
			return;
		mLastLocationMillis = SystemClock.elapsedRealtime();
		mLastLocation = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}
}