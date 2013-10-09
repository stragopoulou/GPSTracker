package com.example.gpstracker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.graphics.Color;
import android.view.Menu;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity {

	private GoogleMap mMap;
	private int mapType = GoogleMap.MAP_TYPE_NORMAL;
	ArrayList<Double> Speed;
	ArrayList<Double> Latitude;
	ArrayList<Double> Longtitude;
	ArrayList<String> Timestamp;
	ArrayList<String> Type;
	ArrayList<String> GpsFixed;
	private String filename;
	private Bundle extras = null;
	private int lineColor = 0;
	private boolean firstMarker = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		Speed = new ArrayList<Double>();
		Latitude = new ArrayList<Double>();
		Longtitude = new ArrayList<Double>();
		Timestamp = new ArrayList<String>();
		Type = new ArrayList<String>();
		GpsFixed = new ArrayList<String>();

		FragmentManager fragmentManager = getSupportFragmentManager();
	    SupportMapFragment mapFragment =  (SupportMapFragment)fragmentManager.findFragmentById(R.id.map);
	    mMap = mapFragment.getMap();

		extras = getIntent().getExtras();
		if (extras != null) {
			filename = extras.getString("filename");
		}
		readRouteFile(filename);

		for (int i = 0; i < Longtitude.size(); i++) {
			createPoint(i);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.maps, menu);
		return true;
	}

	private void readRouteFile(String filename) {

		BufferedReader br = null;
		try {
			int counter = 0;
			String currentLine;
			br = new BufferedReader(new FileReader(
					"/data/data/com.example.gpstracker/" + filename));
			while ((currentLine = br.readLine()) != null) {
				counter++;
				if (counter > 10) {
					// gia kathe grammi tou arxeiou pou diavazei kanei
					// tokenizing me ","
					StringTokenizer st = new StringTokenizer(currentLine, ",");
					while (st.hasMoreTokens()) { // apothikeuei to kathe token
													// stin antistoixi arrayList
						Latitude.add(Double.parseDouble(st.nextToken()));
						Longtitude.add(Double.parseDouble(st.nextToken()));
						Speed.add(Double.parseDouble(st.nextToken()));
						Timestamp.add(st.nextToken());
						Type.add(st.nextToken());
						GpsFixed.add(st.nextToken());
					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), "IO Exception",
					Toast.LENGTH_LONG).show();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException ex) {
				Toast.makeText(getApplicationContext(), "IO Exception 2",
						Toast.LENGTH_LONG).show();
				ex.printStackTrace();
			}
		}
	}

	private void createPoint(int position) {
		
		double lat = Latitude.get(position);
		double lon = Longtitude.get(position);
		String moveType = Type.get(position);
		String timestamp = Timestamp.get(position);
		timestamp = timestamp.replace("\"", "");
		String[] tokens = timestamp.split("\\s+");
		String time = tokens[1];
		if (firstMarker)
			firstMarker = false;
		else
			createPolyline(position);
		if (moveType.equals("Walking")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.walking)).title("Walking\n" + time));
			lineColor = Color.rgb(255, 255, 86);
		} else if (moveType.equals("Running")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.running)).title("Running\n" + time));
			lineColor = Color.rgb(255, 188, 84);
		} else if (moveType.equals("Biking")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.cycling)).title("Cycling\n" + time));
			lineColor = Color.rgb(255, 88, 255);
		} else if (moveType.equals("Driving")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.driving)).title("Driving\n" + time));
			lineColor = Color.rgb(233, 206, 130);
		} else if (moveType.equals("Metro")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.metro)).title("Metro\n" + time));
			lineColor = Color.rgb(169, 179, 218);
		} else if (moveType.equals("Bus")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)).title("Bus\n" + time));
			lineColor = Color.rgb(138, 236, 216);
		} else if (moveType.equals("Motionless")) {
			mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.motionless)).title("Motionless\n" + time));
			lineColor = Color.rgb(247, 170, 170);
		}
		CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(
				lat, lon));
		CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
		mMap.moveCamera(center);
		mMap.animateCamera(zoom);

	}
	
	private void createPolyline(int position) {
		
		double curLat = Latitude.get(position);
		double curLon = Longtitude.get(position);
		double prevLat = Latitude.get(position - 1);
		double prevLon = Longtitude.get(position - 1);
		
		mMap.addPolyline(new PolylineOptions()
        .add(new LatLng(prevLat, prevLon), 
             new LatLng(curLat, curLon))
        .color(lineColor)
        .width(5)
        .geodesic(true));
		
	}

}
