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
import android.util.Log;

public class GPSServiceTask implements Runnable, SensorEventListener {

	public static final String LOG_TAG = "MyService";
	private boolean running;
	private Context context;
	
    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
    		new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults = 
    		new ConcurrentLinkedQueue<ServiceResult>();
    
    // Sensor data
    private SensorManager mSensorManager;
	private Sensor accelSensor;
	private double curAccel;
	private double maxAccel;
	private String maxTime;
	
	// Constructor
    public GPSServiceTask(Context _context) {
    	context = _context;
    	curAccel = 0;
    	maxAccel = 0;
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
        	accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        } else {
        	Log.i(LOG_TAG, "ERROR: NO ACCELEROMETER");
        	running = false;
        }
        mSensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
			Log.i(LOG_TAG, "Sending accelerometer readout: " + curAccel);
			notifyResultCallback();
        }
    }
    
    // records data from the linear accelerometer on a normal interval
    public void onSensorChanged(SensorEvent event) {
    	
    	double x = event.values[0];
    	double y = event.values[1];
    	double z = event.values[2];
    	curAccel = Math.sqrt(x*x + y*y + z*z);
    	
    	if (curAccel > maxAccel) {
    		maxAccel = curAccel;
    		Date timeNow = new Date();
    		SimpleDateFormat ft = new SimpleDateFormat ("E hh:mm:ss a");
    		maxTime = ft.format(timeNow);
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
    	Log.i(LOG_TAG, "Freeing result holder for " + r.curAccel);
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
    			for (ResultCallback resultCallback : resultCallbacks) {
    				Log.i(LOG_TAG, "calling resultCallback for " + result.curAccel);
    				resultCallback.onResultReady(result);
    			}
    		}
    	}
    }

    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// ignore for now
	}
}
