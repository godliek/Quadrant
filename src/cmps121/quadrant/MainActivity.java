package cmps121.quadrant;

import cmps121.quadrant.GPSService.MyBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements GPSServiceTask.ResultCallback {
	// Log
    private static final String LOG_TAG = "MainActivity";
    
    // Service
	public static final int MESSAGE_NUMBER = 10;
	public static final boolean SERVICE_STOP_ON_PAUSE = false;
    private Handler mUiHandler;
    
    // Timer
    private long timerStartTime;
    private long timerElapsedTime;
    private static final int STATE_IDLE = 0;
    private static final int STATE_RECORDING = 1;
    private static final int STATE_PAUSED = 2;
    
    // State
    private int activityState;
    
    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            timerElapsedTime += System.currentTimeMillis() - timerStartTime;
            timerStartTime = System.currentTimeMillis();
            int seconds = (int) (timerElapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            
            TextView tv = (TextView) findViewById(R.id.textView_timer);
            tv.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };
    
    // Service connection variables.
    private boolean serviceBound;
    private GPSService myService;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new Handler(getMainLooper(), new UiCallback());    
        serviceBound = false;
        
        restoreState(savedInstanceState);
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
        if (serviceBound) {
        	if (myService != null) {
        		myService.removeResultCallback(this);
        	}
    		Log.i("MyService", "Unbinding");
    		unbindService(serviceConnection);
        	serviceBound = false;

        	// Keep watching the accelerometer when the app is paused
        	if (SERVICE_STOP_ON_PAUSE) {
        		Log.i(LOG_TAG, "Stopping.");
        		Intent intent = new Intent(this, GPSService.class);
        		stopService(intent);
        		Log.i(LOG_TAG, "Stopped.");
        	}
        }
        super.onPause();
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_help) {
			return true;
		} else if (id == R.id.action_history) {
			Intent intent = new Intent(this, HistoryActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    @Override
    /**
     * Kills MyService when the app is closed
     */
	public void finish() {
    	super.finish();
    	// stop the service when the app is closed
    	Intent intent = new Intent(this, GPSService.class);
    	stopService(intent);
    }

    @Override
    /**
     * Save the activity state.
     */
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putLong("timerStartTime", timerStartTime);
    	outState.putInt("activityState", activityState);
    	outState.putLong("timerElapsedTime", activityState);
    	super.onSaveInstanceState(outState);
    }
    
    /**
     * Restore the activity state.
     */
    private void restoreState(Bundle savedState) {
    	timerStartTime = 0;
    	activityState = 0;
    	timerElapsedTime = 0;
    	
    	if (savedState != null) {
    		timerStartTime = Long.parseLong(savedState.getString("timerStartTime"));
    		TextView tv = (TextView) findViewById(R.id.textView_timer);
    		tv.setText(Long.toString(timerStartTime));
    		activityState = Integer.parseInt(savedState.getString("activityState"));
    		timerElapsedTime = Long.parseLong(savedState.getString("timerElapsedTime"));
    	}
    	
    }
    
    // attempt to bind to MyService
    private void bindMyService() {
    	Log.i(LOG_TAG, "Starting the service");
    	Intent intent = new Intent(this, GPSService.class);
    	Log.i("LOG_TAG", "Trying to bind");
    	bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    

    // Service connection code.
    private ServiceConnection serviceConnection = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
    		// We have bound to the acceleration service.
    		MyBinder binder = (MyBinder) serviceBinder;
    		myService = binder.getService();
    		serviceBound = true;
    		// Let's connect the callbacks.
    		Log.i("MyService", "Bound succeeded, adding the callback");
    		myService.addResultCallback(MainActivity.this);
    	}
    	
    	@Override
    	public void onServiceDisconnected(ComponentName arg0) {
    		serviceBound = false;
    	}
    };

    /**
     * This function is called from the service thread.  To process this, we need 
     * to create a message for a handler in the UI thread.
     */
    @Override
    public void onResultReady(ServiceResult result) {
    	if (result != null) {
    		//Log.i(LOG_TAG, "Preparing a message for " + result.curAccel);
    	} else {
    		Log.e(LOG_TAG, "Received an empty result!");
    	}
        mUiHandler.obtainMessage(MESSAGE_NUMBER, result).sendToTarget();
    }
    
    /**
     * This Handler callback gets the message generated above. 
     * It is used to display the integer on the screen.
     */
    private class UiCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == MESSAGE_NUMBER) {
            	// Gets the result.
            	ServiceResult result = (ServiceResult) message.obj;
            	// Displays it.
            	if (result != null) {
            		//Log.i(LOG_TAG, "Displaying: " + result.curAccel);
            		
// UPDATE GUI with service result here
            		
            		// Tell the worker that the bitmap is ready to be reused
            		if (serviceBound && myService != null) {
            			//Log.i(LOG_TAG, "Releasing result holder for " + result.curAccel);
            			myService.releaseResult(result);
            		}
            	} else {
            		Log.e(LOG_TAG, "Error: received empty message!");
            	}
            }
            return true;
        }
    }
    
    public void clickRecord(View v) {
    	int duration = Toast.LENGTH_SHORT;
    	
    	TextView tv = (TextView) findViewById(R.id.button_record);
    	
    	// handle the recording state
    	if (activityState == STATE_IDLE) {
    		activityState = STATE_RECORDING;
    		tv.setText("PAUSE");
    		// Set timer start time and start the timer handler
    		timerStartTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
    	} else if (activityState == STATE_RECORDING) {
    		activityState = STATE_PAUSED;
    		tv.setText("RESUME");
    		// Pause the timer
    		timerHandler.removeCallbacks(timerRunnable);
    	} else if (activityState == STATE_PAUSED) {
    		activityState = STATE_RECORDING;
    		tv.setText("PAUSE");
    		// Resume the timer
    		timerStartTime = System.currentTimeMillis();
    		timerHandler.postDelayed(timerRunnable, 0);
    	}
    	
    	if (serviceBound) { // stop the service
        	if (myService != null) {
        		myService.removeResultCallback(this);
        	}
    		Log.i("MyService", "Unbinding");
    		unbindService(serviceConnection);
        	serviceBound = false;

        	Log.i(LOG_TAG, "Stopping.");
        	Intent intent = new Intent(this, GPSService.class);
        	stopService(intent);
        	Log.i(LOG_TAG, "Stopped.");
        	
        	CharSequence text = "GPS Service Stopped!";
        	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        	toast.show();
        } else { // start the service
            // Starts the service, so that the service will only stop when explicitly stopped.
            Intent intent = new Intent(this, GPSService.class);
            startService(intent);
        	bindMyService();
        	
        	CharSequence text = "GPS Service Started!";
        	Toast toast = Toast.makeText(getApplicationContext(), text, duration);
        	toast.show();
        }
    }

}