package com.example.gpstracker;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SelectMultiFiles extends Activity implements OnClickListener {

	Button upload_btn;
	Button delete_btn;
	ListView listView;
	ArrayAdapter<String> adapter;
	Bundle b;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_multi_files);

		upload_btn = (Button) findViewById(R.id.uploadbutton);
		delete_btn = (Button) findViewById(R.id.deletebutton);
		listView = (ListView) findViewById(R.id.mysecondlist);

		displayFiles();

	}

	private void displayFiles() {

		final ArrayList<String> routeFiles = new ArrayList<String>();

		File folder = new File("/data/data/com.example.gpstracker/");

		for (File file : folder.listFiles()) {
			if (file.getName().endsWith((".arff"))) {
				routeFiles.add(file.getName());
			}
		}

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, routeFiles);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setAdapter(adapter);
		upload_btn.setOnClickListener(this);
		delete_btn.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_multi_files, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		SparseBooleanArray checked = listView.getCheckedItemPositions();
		ArrayList<String> selectedItems = new ArrayList<String>();
		for (int i = 0; i < checked.size(); i++) {
			// Item position in adapter
			int position = checked.keyAt(i);
			if (checked.valueAt(i))
				selectedItems.add(adapter.getItem(position));
		}

		String[] outputStrArr = new String[selectedItems.size()];

		for (int i = 0; i < selectedItems.size(); i++) {
			outputStrArr[i] = selectedItems.get(i);
		}

		b = new Bundle();
		b.putStringArray("selectedFiles", outputStrArr);

		if (v.equals(upload_btn)) {
			Intent uploadIntent = new Intent(getApplicationContext(),
					DropboxUpload.class);
			uploadIntent.putExtras(b);
			startActivity(uploadIntent);
		} else if (v.equals(delete_btn)) {
			AlertDialog diaBox = AskOption();
			diaBox.show();
		}
	}

	private AlertDialog AskOption() {
		AlertDialog myQuittingDialogBox = new AlertDialog.Builder(this)
				// set message, title, and icon
				.setTitle("Delete")
				.setMessage("Are you sure you want to delete")
				.setPositiveButton("Delete",
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								Intent deleteIntent = new Intent(getApplicationContext(),
										DeleteFile.class);
								deleteIntent.putExtras(b);
								startActivity(deleteIntent);
								dialog.dismiss();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						}).create();
		return myQuittingDialogBox;

	}
}
