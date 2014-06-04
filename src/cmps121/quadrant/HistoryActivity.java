package cmps121.quadrant;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.os.Build;

public class HistoryActivity extends Activity {

	private ArrayList<GPSEntry> savedTrips;
	private ListViewAdapter aa;
	
	// Persistent data
    private SharedPreferences mPrefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		// place an up button on the action bar to return to the record activity
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		savedTrips = new ArrayList<GPSEntry>();
		aa = new ListViewAdapter(this, R.layout.list_element, savedTrips);
		ListView myListView = (ListView) findViewById(R.id.listView1);
		myListView.setAdapter(aa);
		// initialize class members

		mPrefs = getSharedPreferences("quadrant", MODE_PRIVATE);

		try {
			String data = mPrefs.getString("TRIPDATA", "oh shit");
			JSONArray tripData = new JSONArray(data);
			Log.d("JSON", "array made from extras");
			for(int i = 0; i < tripData.length(); i++) {
				Log.d("loop", "trying to add item to listview");
				JSONArray jArr = (JSONArray) tripData.get(i);
				if(!jArr.toString().equals("[]")) {
					GPSEntry g = new GPSEntry(jArr);
				
					//Get time of first GPS data in JSON array
					JSONObject jObj = (JSONObject) jArr.get(0);
					String time = jObj.getString("time");
					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
					String dateStarted = formatter.format(new Date(Long.parseLong(time)));
				
					g.titleText = dateStarted;
					savedTrips.add(g);
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Collections.reverse(savedTrips);	//Reverse the list; it is in the order in which the records were saved(newest last)

		/*
		// create a list item click handler
		myListView.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent,View view, int position, long id) 
		    {
		    	//Start mapview activity with data from this GPSEntry
		    	Log.d("GPSEntryClicked", savedTrips.get(position).data.toString());
		    	String tripData = savedTrips.get(position).data.toString();
		    	Intent i = new Intent(getApplicationContext(), ViewTripActivity.class);
		    	i.putExtra("tripData", tripData);
		    	startActivity(i);
		    }
		});*/
		
		// refresh the listview
		aa.notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.history, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_help) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/** GPSEntry
	 *    ListView element
	 */
	private class GPSEntry {
		GPSEntry() {};
		GPSEntry(JSONArray jArr) {
			data = jArr;
		}
		String titleText;
		JSONArray data;
	}
	
	/** ListViewAdapter
	 *    ArrayAdapter for the ListView
	 */
	private class ListViewAdapter extends ArrayAdapter<GPSEntry>{

		int resource;
		Context context;
		
		// constructor
		public ListViewAdapter(Context _context, int _resource, List<GPSEntry> items) {
			super(_context, _resource, items);
			resource = _resource;
			context = _context;
			this.context = _context;
		}
		
		@Override
		// inflate an array element
		public View getView(final int position, View convertView, ViewGroup parent) {
			LinearLayout newView;
			
			GPSEntry w = getItem(position);
			
			// Inflate a new view if necessary.
			if (convertView == null) {
				newView = new LinearLayout(getContext());
				String inflater = Context.LAYOUT_INFLATER_SERVICE;
				LayoutInflater vi = (LayoutInflater) getContext().getSystemService(inflater);
				vi.inflate(resource,  newView, true);
			} else {
				newView = (LinearLayout) convertView;
			}
			
			// Fills in the view.
			TextView tv = (TextView) newView.findViewById(R.id.listText);
			tv.setText(w.titleText);
			
			Button b1 = (Button) newView.findViewById(R.id.viewButton);
			b1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
			    	//Start mapview activity with data from this GPSEntry
			    	Log.d("GPSEntryClicked", savedTrips.get(position).data.toString());
			    	String tripData = savedTrips.get(position).data.toString();
			    	Intent i = new Intent(getApplicationContext(), ViewTripActivity.class);
			    	i.putExtra("tripData", tripData);
			    	startActivity(i);
				}
			});
			Button b = (Button) newView.findViewById(R.id.removeButton);
			b.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Reacts to a button press.
					// Gets the integer tag of the button.
					String s = (String) v.getTag();
					Log.d("tag value", "the value is: " + position);
					//int pos = Integer.parseInt(s);
					
					//remove GPSEntry at index
					savedTrips.remove(position);
					aa.notifyDataSetChanged();
					//Remake JSON array without removed values
					JSONArray updatedTrips = new JSONArray();
					for(int i = 0; i < savedTrips.size(); i++) {
						GPSEntry g = savedTrips.get(i);
						updatedTrips.put(g.data);
					}
					//update sharedprefs
					SharedPreferences.Editor ed = mPrefs.edit();
					ed.remove("TRIPDATA");
					ed.putString("TRIPDATA", updatedTrips.toString());
					ed.commit();
					
					//write file
		    		FileOutputStream fos;
					try {
						fos = openFileOutput("TRIPDATA.TXT", Context.MODE_PRIVATE);
			    		fos.write(updatedTrips.toString().getBytes());
			    		fos.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});

			return newView;
		}		
	}

}
