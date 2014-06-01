package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.os.Build;

public class HistoryActivity extends Activity {

	private ArrayList<GPSEntry> savedTrips;
	private ListViewAdapter aa;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		// place an up button on the action bar to return to the record activity
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// initialize class members
		savedTrips = new ArrayList<GPSEntry>();
		
		
		Intent intent = getIntent();
		try {
			JSONArray tripData = new JSONArray(intent.getStringExtra("tripHistory").toString());
			Log.d("JSON", "array made from extras");
			for(int i = 0; i < tripData.length(); i++) {
				Log.d("loop", "trying to add item to listview");
				JSONArray jArr = (JSONArray) tripData.get(i);
				GPSEntry g = new GPSEntry(jArr);
				
				//Get time of first GPS data in JSON array
				JSONObject jObj = (JSONObject) jArr.get(0);
				String time = jObj.getString("time");
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
				String dateStarted = formatter.format(new Date(Long.parseLong(time)));
				
				g.something = dateStarted;
				savedTrips.add(g);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		aa = new ListViewAdapter(this, R.layout.list_element, savedTrips);
		ListView myListView = (ListView) findViewById(R.id.listView1);
		
		// set the list view adapter
		myListView.setAdapter(aa);
		
		// create a list item click handler
		myListView.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent,View view, int position, long id) 
		    {
		    	//Start mapview activity with data from this GPSEntry
		    	Log.d("GPSEntryClicked", savedTrips.get(position).data.toString());
		    }
		});
		
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
		String something;
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
		public View getView(int position, View convertView, ViewGroup parent) {
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
			tv.setText(w.something);

			return newView;
		}		
	}

}
