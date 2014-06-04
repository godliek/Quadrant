package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLngBounds.Builder;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ViewTripActivity extends Activity {
 
    // Google Map
    private GoogleMap googleMap;
    
    private LatLngBounds builtBounds;
    
    private int zoom = 15; 	//Zoom of the map
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_trip);
           
        try {
            // Loading map
            initilizeMap();
 
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = getIntent();
        try {
			JSONArray jArr = new JSONArray(intent.getStringExtra("tripData"));
			
			//Get first gps location, and set the camera to look at it.
			JSONObject initjObj = jArr.getJSONObject(0);
			double initLat = Double.parseDouble(initjObj.get("lat").toString());
			double initLng = Double.parseDouble(initjObj.get("long").toString());
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(initLat,initLng), zoom));
			
			//Create markers on map and lines to join them
			PolylineOptions plo = new PolylineOptions().geodesic(true);
			Builder bounds = new LatLngBounds.Builder();
			for(int i = 0; i < jArr.length(); i++) {
				JSONObject jObj = jArr.getJSONObject(i);
				//get data from JSON
				double lat = Double.parseDouble(jObj.get("lat").toString());
				double lng = Double.parseDouble(jObj.get("long").toString());
				double elev = Double.parseDouble(jObj.getString("elev").toString());
				String time = getTimeFromEpoch(jObj.getString("time"));
				
				
				//Add coordinates to polyline
				plo.add(new LatLng(lat, lng));
				//Add coordinates to marker
				MarkerOptions marker = new MarkerOptions().position(new LatLng(lat, lng)).title(time).snippet("Elevation: " + String.valueOf(elev));
				googleMap.addMarker(marker);
				bounds.include(new LatLng(lat, lng));
			}
			googleMap.addPolyline(plo);
			builtBounds = bounds.build();
			//googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builtBounds, 3));
			
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
 
    /**
     * function to load map. If map is not created it will create it for you
     * */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
 
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
 
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }
 
    public String getTimeFromEpoch(String epoch) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		String currentTime = formatter.format(new Date(Long.parseLong(epoch)));
		return currentTime;
    }
}