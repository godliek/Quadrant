package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
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
    private Criteria criteria;
    private double longitude, latitude, altitude;
    private double prevLong = -9999;
    private double prevLat = -9999;
    
    private double totalElevation;
    private double prevElevation = -9999;
    
    private double distanceTraveled;

	
	// Constructor
    public GPSServiceTask(Context _context) {
    	context = _context;
    	
    	myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	criteria = new Criteria();
    	criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    	
    	myLocationListener = new LocationListener() {
    		
    		public void onLocationChanged(Location location) {
    			Log.d(LOG_TAG, "LAT: " + location.getLatitude());
    			Log.d(LOG_TAG, "LONG: " + location.getLongitude());
    			Log.d(LOG_TAG, "ELEV: " + location.getAltitude());
    			latitude = location.getLatitude();
    			longitude = location.getLongitude();
    			altitude = location.getAltitude();
    			
    			
    			//Elevation
    			if(prevElevation == -9999) {	//only on first run, set the elevation to the first elevation recorded.
    				prevElevation = altitude;
    			} else {
    				if(altitude > prevElevation) {	//otherwise, if the elevation is higher than the previous point, add it to the total
    					totalElevation = totalElevation + (altitude - prevElevation);
    					prevElevation = altitude;	//update previous elevation
    				}
    			}
    			
    			//Distance
    			if(prevLat == -9999) {	//Set up values after first polling of gps data
    				prevLat = latitude;
    				prevLong = longitude;
    				distanceTraveled = 0;
    				totalElevation = 0;
    			}
    			else {					//Do distance calculations once there has been more than one gps location polled.
    				float[] results = new float[3];
    				Location.distanceBetween(prevLat, prevLong, latitude, longitude, results);	//Distance between current and previous point
    				prevLat = latitude;		//Update long/lat values so the current values are now previous, to set up for the next pass
    				prevLong = longitude;
    				distanceTraveled += results[0];
    			}
    		}
    		public void onProviderDisabled(String provider) {}
    		public void onProviderEnabled(String provider) {}
			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
				// TODO Auto-generated method stub
				
			}
    		};		

    	
    	if(criteria == null) {
    		Log.d("criteria", "criteria is NULL");
    	}
    	else {
    		String best = myLocationManager.getBestProvider(criteria, false);
    		long time = 2000;	//Time interval for GPS polling
    		float minDist = 5;	//Minimum distance to travel between pollings.
    		myLocationManager.requestLocationUpdates(best, time, minDist, myLocationListener);
    	}
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
        		// report the acceleration to the UI thread in MainActivity
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
    	// ...and we clear the list of results.
    	// Note that this works because, even though mResultCallbacks is a synchronized set,
    	// its cardinality should always be 0 or 1 -- never more than that. 
    	// We have one viewer only.
        // We clear the buffer, because some result may never be returned to the
        // free buffer, so using a new set upon reattachment is important to avoid
        // leaks.
    	freeResults.clear();
    }

    // Creates result bitmaps if they are needed.
    private void createResultsBuffer() {
    	// I create some results to talk to the callback, so we can reuse these instead of creating new ones.
    	// The list is synchronized, because integers are filled in the service thread,
    	// and returned to the free pool from the UI thread.
    	freeResults.clear();
    	for (int i = 0; i < 10; i++) {
    		freeResults.offer(new ServiceResult());
    	}
    }
    
    // This is called by the UI thread to return a result to the free pool.
    public void releaseResult(ServiceResult r) {
    	//Log.i(LOG_TAG, "Freeing result holder for " + r.curAccel);
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
    	String best = myLocationManager.getBestProvider(criteria, false);
		long time = 2000;	//Time interval for GPS polling
		float minDist = 5;	//Minimum distance to travel between pollings.
		myLocationManager.requestLocationUpdates(best, time, minDist, myLocationListener);
    }


    /**
     * Call this function to return the current and max acceleration to the activity.
     * @param cur max
     */
    private void notifyResultCallback() {
    	if (!resultCallbacks.isEmpty()) {
    		// If we have no free result holders in the buffer, then we need to create them.
    		if (freeResults.isEmpty()) {
    			createResultsBuffer();
    		}
    		ServiceResult result = freeResults.poll();
    		if (result != null) {
    			result.elevation = totalElevation * 3.28084;		//elevation in feet
    			result.distance = distanceTraveled * 0.000621371;	//distance in miles
    			Log.d(LOG_TAG, "" + result.elevation + "\n" + result.distance);
    			for (ResultCallback resultCallback : resultCallbacks) {
    				//Log.i(LOG_TAG, "calling resultCallback for " + result.curAccel);
    				resultCallback.onResultReady(result);
    			}
    		}
    	}
    }

    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }
}

