package com.example.gpstracker;

import java.io.File;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.Toast;

public class DeleteFile extends Activity {

	private Bundle extras = null;
	private String[] filesArr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_delete_file);
		
		extras = getIntent().getExtras();
		if (extras != null) {
			filesArr = extras.getStringArray("selectedFiles");
		}
		
		File folder = new File("/data/data/com.example.gpstracker/");
		folder.mkdirs();

		int size = filesArr.length;
		File[] filesToDelete = new File[size];
		for (int i = 0; i < size; i++){
			filesToDelete[i] = new File(folder.getAbsolutePath() + "/" + filesArr[i]);
			boolean deleted = filesToDelete[i].delete();
			if (deleted)
				Toast.makeText(getApplicationContext(), filesArr[i]+" deleted.", Toast.LENGTH_LONG)
				.show();
		}
		Intent back = new Intent(DeleteFile.this, MainActivity.class);
		startActivity(back);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.delete_file, menu);
		return true;
	}

}
