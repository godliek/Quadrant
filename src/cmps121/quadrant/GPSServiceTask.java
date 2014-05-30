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

	public static final String LOG_TAG = "MyService";
	private boolean running;
	private Context context;
	
    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults = new ConcurrentLinkedQueue<ServiceResult>();
    
    // Sensor data
    //private SensorManager mSensorManager;
    private LocationManager myLocationManager;
    private LocationListener myLocationListener;
    private Criteria criteria;
    private double longitude, latitude;
    private double prevLong = - 9999;
    private double prevLat = -9999;
    
    private double totalElevation = 0;
    private double prevElevation = -9999;
    
    private double distanceTraveled = 0;
    
	private Sensor accelSensor;
	private double curAccel;
	private double maxAccel;
	private String maxTime;
	
	// Constructor
    public GPSServiceTask(Context _context) {
    	Log.d("test","creating service task");
    	context = _context;
    	
    	myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    	criteria = new Criteria();
    	criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    	
    	myLocationListener = new LocationListener() {
    		
    		public void onLocationChanged(Location location) {
    		    //tv1.setText("Lat " +   location.getLatitude() + " Long " + location.getLongitude());
    			Log.d("Latitude", "LAT: " + location.getLatitude());
    			Log.d("Longitude", "LONG: " + location.getLongitude());
    			Log.d("Elevation", "ELEV: " + location.getAltitude());
    			
    			if(prevElevation == -9999) {	//only on first run, set the elevation to the first elevation recorded.
    				prevElevation = location.getAltitude();
    			} else {
    				if(location.getAltitude() > prevElevation) {
    					totalElevation = totalElevation + (location.getAltitude() - prevElevation);
    					prevElevation = location.getAltitude();
    				}
    			}
    			
    			if(prevLat == -9999) {
    				prevLat = location.getLatitude();
    				prevLong = location.getLongitude();
    			}
    			else {
    				float[] results = new float[3];
    				Location.distanceBetween(prevLat, prevLong, location.getLatitude(), location.getLongitude(), results);
    				prevLat = location.getLatitude();
    				prevLong = location.getLongitude();
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
    		long time = 6000;
    		float minDist = 5;
    		myLocationManager.requestLocationUpdates(best, time, minDist, myLocationListener);
    		Log.d("test","requested location updates");
    	}
    }
    
    @Override
    // GPS Service thread mainloop
    public void run() {
        running = true;
        
        while (running) {
        	// Sleep a tiny bit.
			try {
				Thread.sleep(250);
			} catch (Exception e) {
				e.getLocalizedMessage();
			}

			// report the acceleration to the UI thread in MainActivity
			//Log.i(LOG_TAG, "Sending accelerometer readout: " + curAccel);
			notifyResultCallback();
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
        running = false;
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
    			result.curAccel = curAccel;
    			result.maxAccel = maxAccel;
    			result.maxTime = maxTime;
    			result.elevation = totalElevation * 3.28084;		//elevation in feet
    			result.distance = distanceTraveled * 0.000621371;	//distance in miles
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
