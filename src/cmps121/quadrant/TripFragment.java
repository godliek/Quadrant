package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

/** A MapFragment with selectable recorded trips */
public class TripFragment extends Fragment implements OnItemSelectedListener {
	
	private static final String LOG_TAG = "TripFragment";
	
    private SharedPreferences mPrefs;
    
    private GoogleMap googleMap = null;
    private Polyline mPolyline = null;
    private LatLngBounds builtBounds = null;
    private LatLng tripStart = null;
    private LatLng tripEnd = null;
    private boolean mapLoaded = false;
    
	protected static ArrayList<GPSEntry> savedTrips;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        View view  = inflater.inflate(R.layout.map_fragment, container, false);

        return view;
    }
	
	@Override
	public void onStart() {
		super.onStart();
		
		savedTrips = new ArrayList<GPSEntry>();
        // initialize class members
     	mPrefs = getActivity().getSharedPreferences("quadrant", Context.MODE_PRIVATE);

 		try {
 			String data = mPrefs.getString("TRIPDATA", "[]");
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
 			Log.d(LOG_TAG, e.toString());
 		}
 		
 		Collections.reverse(savedTrips);	//Reverse the list; it is in the order in which the records were saved(newest last)
 		
 		// setup the map
        setUpMapIfNeeded();
 		
 		// setup spinner
        Spinner mySpinner = (Spinner) getView().findViewById(R.id.trip_spinner);
        ArrayAdapter<GPSEntry> adapter = new ArrayAdapter<GPSEntry>(getActivity(), android.R.layout.simple_spinner_item, savedTrips);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter); 
        mySpinner.setOnItemSelectedListener(this);
        adapter.notifyDataSetChanged();
	}
	
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            googleMap = ((SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            
            // Check if we were successful in obtaining the map.
            if (googleMap != null) {
            	Log.d(LOG_TAG, "Map initialized successfully");
            	// set the camera view upon loading
            	googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            	    @Override
            	    public void onMapLoaded() {
        	    		mapLoaded = true;
            	    	if (builtBounds != null) {
            	    		googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builtBounds, 30));
            	    	}
            	    }
            	});
            } else {
            	Log.d(LOG_TAG, "Map failed to initialize");
            }
        }
    }
	
	@Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.d(LOG_TAG, "spinner entry selected");
        drawTrip(GPSData.fromJSONArray(((GPSEntry)parent.getItemAtPosition(position)).data));
    }

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	// draw a selected trip on the map
	public void drawTrip(GPSData data) {
		if (googleMap == null) {
			Log.d(LOG_TAG, "ERROR drawTrip() map is null");
			return;
		}
		if (data.size() < 1) {
			Log.d(LOG_TAG, "ERROR drawTrip() bad data");
			return;
		}
		
		// clear any old markers or polylines from the map
		googleMap.clear();
		
		Builder bounds = new LatLngBounds.Builder();
		PolylineOptions options = new PolylineOptions();
		options.color(0xff54FF9F); // seagreen 
		
		for (int i = 0; i < data.size(); i++) {
			LatLng location = new LatLng(data.getLatitude(i), data.getLongitude(i));
			String time = data.getTimeString(i);
			//Add coordinates to polyline
			options.add(location);
			// drop a marker at the start and end locations
			if (i == 0) {
				tripStart = location;

				MarkerOptions marker = new MarkerOptions().position(tripStart).title("Start: " + time)
						.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
				googleMap.addMarker(marker);
				
			}
			if (i == data.size() - 1) {
				tripEnd = location;
				MarkerOptions marker = new MarkerOptions().position(tripEnd).title("End: " + time);
				googleMap.addMarker(marker);
			}
			bounds.include(new LatLng(data.getLatitude(i), data.getLongitude(i)));
		}
		mPolyline = googleMap.addPolyline(options);
		builtBounds = bounds.build();
		
		if(mapLoaded == false) {
			Log.d(LOG_TAG, "waiting to relocate camera");
		} else {
			googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builtBounds, 30));
		}
	}
	
	/** GPSEntry
	 *    ListView element
	 */
	public class GPSEntry {
		GPSEntry() {};
		GPSEntry(JSONArray jArr) {
			data = jArr;
		}
		String titleText;
		JSONArray data;
		
		@Override
		public String toString() {
			return titleText;
		}
	}
}