package cmps121.quadrant;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ViewStatsActivity extends Activity{
	private static final String LOG_TAG = "Stats";
	private ListView listView;
	private ArrayList<String> listEntries;
	private ArrayAdapter arrAdapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_stats);
        
        
        listView = (ListView) findViewById(R.id.statsListView);
        listEntries = new ArrayList<String>();
        arrAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listEntries);
        listView.setAdapter(arrAdapter);
        
        
        Intent intent = getIntent();
        try {
			JSONArray jArr = new JSONArray(intent.getStringExtra("tripData"));
			
			double totalDistance;
			double totalElevation;
			
			for(int i = 0; i < jArr.length(); i++) {
				JSONObject j = jArr.getJSONObject(i);
				double lat = j.getDouble("lat");
				double lng = j.getDouble("long");
				double elev = j.getDouble("elev");
				totalElevation = j.getDouble("totalElev");
				totalDistance = j.getDouble("totalDistance");
				long time = j.getLong("time");
				
				String elevation = String.format("%.3f", elev);
				String dist = String.format("%.3f", totalDistance);
				String totalElev = String.format("%.3f", totalElevation);
				String latitude = String.format("%.3f", lat);
				String longitude = String.format("%.3f", lng);
				
				int displayIndex = i + 1;
				String display = "Location Number " + displayIndex 
									+ "\nLatitude/Longitude: " + latitude + "/" + longitude
									+ "\nElevation: " + elevation + " ft" 
									+ "\nTotal Elevation: " + totalElev + " ft"
									+ "\nTotal Distance: " + dist + " mi";
				listEntries.add(display);
			}
			
		} catch (JSONException e) {
			Log.d(LOG_TAG, e.toString());
		}
        arrAdapter.notifyDataSetChanged();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();   
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
}
