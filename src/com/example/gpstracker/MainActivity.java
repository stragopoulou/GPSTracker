package com.example.gpstracker;

import com.example.gpstracker.GPSChecker.Resource;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements LocationListener,
View.OnClickListener, ServiceConnection{
	
	private GoogleMap mMap;
	private ImageButton start_button, stop_button, maps_button, upload_button;

	private Messenger mServiceMessenger = null;
	boolean mIsBound;

	private static final String LOGTAG = "MainActivity";
	private final Messenger mMessenger = new Messenger(
			new IncomingMessageHandler());

	private ServiceConnection mConnection = this;
	private int lineColor = 0;
	private boolean firstMarker = true;
	private double prevLat = 0.0, prevLon = 0.0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		start_button = (ImageButton) findViewById(R.id.start_tracking);
		stop_button = (ImageButton) findViewById(R.id.stop_tracking);
		maps_button = (ImageButton) findViewById(R.id.show_on_map);
		upload_button = (ImageButton) findViewById(R.id.upload_files);

		start_button.setOnClickListener(this);
		stop_button.setOnClickListener(this);
		maps_button.setOnClickListener(this);
		upload_button.setOnClickListener(this);
		
		FragmentManager fragmentManager = getSupportFragmentManager();
	    SupportMapFragment mapFragment =  (SupportMapFragment)
	            fragmentManager.findFragmentById(R.id.main_map);
	    mMap = mapFragment.getMap();

		automaticBind();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Check if the service is running. If the service is running when the
	 * activity starts, we want to automatically bind to it.
	 */
	private void automaticBind() {
		if (LocationService.isRunning()) {
			doBindService();
		}
	}
	@Override protected void onResume() {
		  super.onResume();
		  new GPSChecker(this).pass(new GPSChecker.Pass() {
		     @Override public void pass() {
		        //do your stuff here, do nothing outside here
		     }
		  }).check(Resource.GPS);
		}

	/**
	 * Bind this Activity to MyService
	 */
	private void doBindService() {
		bindService(new Intent(this, LocationService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.i(LOGTAG, "Binding.");
	}

	/**
	 * Un-bind this Activity to MyService
	 */
	private void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mServiceMessenger != null) {
				try {
					Message msg = Message.obtain(null,
							LocationService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mServiceMessenger.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			Log.i(LOGTAG, "Unbinding.");
		}
	}

	/**
	 * Handle button clicks
	 */
	@Override
	public void onClick(View v) {
		if (v.equals(start_button)) {
			start_button.setImageResource(R.drawable.pressed_rec);
			startService(new Intent(MainActivity.this, LocationService.class));
			doBindService();
		} else if (v.equals(stop_button)) {
			doUnbindService();
			stopService(new Intent(MainActivity.this, LocationService.class));
			start_button.setImageResource(R.drawable.rec_button);
		} else if (v.equals(maps_button)) {
			if (!isMyServiceRunning()) {
				Intent chooseIntent = new Intent(this, SelectFile.class);
				startActivity(chooseIntent);
			} else
				Toast.makeText(getApplicationContext(),
						"Stop tracking to load file on map", Toast.LENGTH_LONG)
						.show();
		}else if (v.equals(upload_button)){
			if (!isMyServiceRunning()) {
				Intent chooseIntent = new Intent(this, SelectMultiFiles.class);
				startActivity(chooseIntent);
			} else{
				Toast.makeText(getApplicationContext(),
						"Stop tracking to upload file to dropbox", Toast.LENGTH_LONG)
						.show();
			}
		}

	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (LocationService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mServiceMessenger = new Messenger(service);
		Log.i(LOGTAG, "Attached.");
		try {
			Message msg = Message.obtain(null,
					LocationService.MSG_REGISTER_CLIENT);
			msg.replyTo = mMessenger;
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			// In this case the service has crashed before we could even do
			// anything with it
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected - process crashed.
		mServiceMessenger = null;
		Log.i(LOGTAG, "Disconnected.");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.e(LOGTAG, "Failed to unbind from the service", t);
		}
	}

	public int intToSend(String moveType) {

		int id = 0;

		if (moveType.equals("Walking"))
			id = 1;
		else if (moveType.equals("Running"))
			id = 2;
		else if (moveType.equals("Biking"))
			id = 3;
		else if (moveType.equals("Driving"))
			id = 4;
		else if (moveType.equals("Metro"))
			id = 5;
		else if (moveType.equals("Bus"))
			id = 6;
		else if (moveType.equals("Motionless"))
			id = 7;

		return id;
	}

	/**
	 * Handle incoming messages from MyService
	 */
	private class IncomingMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(LOGTAG, "IncomingHandler:handleMessage");
			switch (msg.what) {
			case LocationService.MSG_SET_STRING_VALUE:
				String instance = msg.getData().getString("str");
				String tokens[] = instance.split(",", 3);
				createPoint(tokens[0], tokens[1], tokens[2]);
		
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private void createPoint(String Lat, String Lon, String moveType){
		
		double lat = Double.parseDouble(Lat);
		double lon = Double.parseDouble(Lon);
		if (firstMarker)
			firstMarker = false;
		else
			createPolyline(lat, lon, prevLat, prevLon);
		if (moveType.equals("Walking")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.walking)));
			lineColor = Color.rgb(255, 255, 86);
		} else if (moveType.equals("Running")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.running)));
			lineColor = Color.rgb(255, 188, 84);
		} else if (moveType.equals("Biking")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.cycling)));
			lineColor = Color.rgb(255, 88, 255);
		} else if (moveType.equals("Driving")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.driving)));
			lineColor = Color.rgb(233, 206, 130);
		} else if (moveType.equals("Metro")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.metro)));
			lineColor = Color.rgb(169, 179, 218);
		} else if (moveType.equals("Bus")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));
			lineColor = Color.rgb(138, 236, 216);
		} else if (moveType.equals("Motionless")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.motionless)));
			lineColor = Color.rgb(247, 170, 170);
		}
		prevLat = lat;
		prevLon = lon;
		CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(
				lat, lon));
		CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
		mMap.moveCamera(center);
		mMap.animateCamera(zoom);

	}
	
	private void createPolyline(double curLat, double curLon, double prevLat, double prevLon) {
		
		mMap.addPolyline(new PolylineOptions()
        .add(new LatLng(prevLat, prevLon), 
             new LatLng(curLat, curLon))
        .color(lineColor)
        .width(5)
        .geodesic(true));
		
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
		new GPSChecker(this).pass(new GPSChecker.Pass() {
		     @Override public void pass() {
		     }
		  }).check(Resource.GPS);
	}


}
