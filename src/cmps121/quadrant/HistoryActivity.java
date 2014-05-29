package cmps121.quadrant;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
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
		
		GPSEntry sampleEntry1 = new GPSEntry();
		sampleEntry1.something = "sample 1";
		GPSEntry sampleEntry2 = new GPSEntry();
		sampleEntry2.something = "sample 2";
		GPSEntry sampleEntry3 = new GPSEntry();
		sampleEntry3.something = "sample 3";
		savedTrips.add(sampleEntry1);
		savedTrips.add(sampleEntry2);
		savedTrips.add(sampleEntry3);
		
		aa = new ListViewAdapter(this, R.layout.list_element, savedTrips);
		ListView myListView = (ListView) findViewById(R.id.listView1);
		
		// set the list view adapter
		myListView.setAdapter(aa);
		
		// create a list item click handler
		myListView.setOnItemClickListener(new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent,View view, int position, long id) 
		    {
		    	// do something when a list view element is selected
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
		String something;
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
