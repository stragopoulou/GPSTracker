package com.example.gpstracker;

import java.io.File;
import java.util.ArrayList;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectFile extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_file);
		displayFiles();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_file, menu);
		return true;
	}
	
	private void displayFiles() {

		ListView filesList = (ListView) findViewById(R.id.mylist);
		final ArrayList<String> routeFiles = new ArrayList<String>();

		File folder = new File("/data/data/com.example.gpstracker/");

		for (File file : folder.listFiles()) {
			if (file.getName().endsWith((".arff"))) {
				routeFiles.add(file.getName());
			}
		}
		filesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent showOnMapIntent = new Intent(SelectFile.this,
						MapsActivity.class);
				showOnMapIntent.putExtra("filename", routeFiles.get(position));
				startActivity(showOnMapIntent);
			}
		});
		
	        
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.list_text,R.id.list_content, routeFiles);
		filesList.setAdapter(arrayAdapter);
	}

}
