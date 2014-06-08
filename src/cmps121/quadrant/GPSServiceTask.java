package cmps121.quadrant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GPSServiceTask implements Runnable{

	public static final String LOG_TAG = "GPSService";
	private boolean running;
	private Context context;
	
    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults = new ConcurrentLinkedQueue<ServiceResult>();
    
    // Location data
    private LocationManager myLocationManager;
    private LocationListener myLocationListener;
    private double latitude;
    private double longitude;
    private double altitude;
    private long pollingTime;
    
    // this data structure contains everything about the trip
    private GPSData locationData;
	
	// Constructor
    public GPSServiceTask(Context _context) {
    	context = _context;
    	locationData = new GPSData();
    	myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	myLocationListener = new LocationListener() {
    		
    		public void onLocationChanged(Location location) {
    			latitude = location.getLatitude();
    			longitude = location.getLongitude();
    			altitude = location.getAltitude();
    			pollingTime = System.currentTimeMillis();
    			
    			// add a new location to our data set
    			locationData.insertLocation(latitude, longitude, altitude, pollingTime);
    			
    			Log.d(LOG_TAG, "LAT: " + latitude + "LONG: " + longitude + "ELEV: " + altitude);
    			
    		}

			@Override
			public void onProviderDisabled(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onProviderEnabled(String arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				// TODO Auto-generated method stub
				
			}
    	};
    	
		long time = 2000;	// Time interval for GPS polling
		float minDist = 5;	// Minimum distance to travel between pollings.
		myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, minDist, myLocationListener);
    }
    
    @Override
    // GPS Service thread mainloop
    public void run() {
        running = true;
        while(true){
        	while (running) {
        		// Sleep a tiny bit.
        		try {
        			Thread.sleep(2000);
        		} catch (Exception e) {
        			e.getLocalizedMessage();
        		}
        		// report GPS to the UI thread in MainActivity
        		notifyResultCallback();
        	}
        }
    }

    public void addResultCallback(ResultCallback resultCallback) {
    	Log.i(LOG_TAG, "Adding result callback");
        resultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
    	Log.i(LOG_TAG, "Removing result callback");
    	// We remove the callback... 
        resultCallbacks.remove(resultCallback);
    	freeResults.clear();
    }

    // Creates result bitmaps if they are needed.
    private void createResultsBuffer() {
    	freeResults.clear();
    	for (int i = 0; i < 10; i++) {
    		freeResults.offer(new ServiceResult());
    	}
    }
    
    // This is called by the UI thread to return a result to the free pool.
    public void releaseResult(ServiceResult r) {
        freeResults.offer(r);
    }
    
    public void stopProcessing() {
    	Log.d("service control", "service paused");
        running = false;
        //stop requesting updates
        myLocationManager.removeUpdates(myLocationListener);
        
    }
    public void startProcessing() {
    	Log.d("service control", "service resumed");
    	running = true;
    	//request updates
		long time = 2000;	//Time interval for GPS polling
		float minDist = 5;	//Minimum distance to travel between pollings.
		myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, minDist, myLocationListener);
    }

    // Return the most current location data to the main activity.
    private void notifyResultCallback() {
    	if (!resultCallbacks.isEmpty()) {
    		// If we have no free result holders in the buffer, then we need to create them.
    		if (freeResults.isEmpty()) {
    			createResultsBuffer();
    		}
    		
    		ServiceResult result = freeResults.poll();
    		if (result != null) {
    			result.distance = locationData.getDistance();
    			result.elevation = locationData.getElevation();
    			result.speed = locationData.getAverageSpeed();
    			
    			for (ResultCallback resultCallback : resultCallbacks) {
    				resultCallback.onResultReady(result);
    			}
    		}
    	}
    }

    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }
    
    // Return all location data in the form of a JSONArray 
    public JSONArray getTripData() {
    	return locationData.toJSONArray();
    }

    // User hit pause button in MainActivity
	public void notifyPaused() {
		locationData.insertPause();
	}
}

