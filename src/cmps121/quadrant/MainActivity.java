package cmps121.quadrant;

import org.json.JSONArray;
import org.json.JSONException;

import cmps121.quadrant.GPSService.MyBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    private int activityState = STATE_IDLE;
    private boolean firstApplicationUse = false;
    
    // Notification
    private static Toast toast;

    private long elapsedTime = 0;
    
    //UI Elements
    private TextView elevationTextView, distanceTextView, velocityTextView;
    private Button recordButton;
    
    // Persistent data
    private SharedPreferences mPrefs;
    
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

            elapsedTime = timerElapsedTime;
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
        
        elevationTextView = (TextView) findViewById(R.id.textView_elevationValue);
        distanceTextView = (TextView) findViewById(R.id.textView_distanceValue);
        velocityTextView = (TextView) findViewById(R.id.textView_speedValue);
    	recordButton = (Button) findViewById(R.id.button_record);
    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			//Start up settings
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_history) {
			Intent intent = new Intent(this, MapActivity.class);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_help) {
			displayHelpDialog();
			return true;
		}
		
		
		return super.onOptionsItemSelected(item);
	}

    @Override
    protected void onResume() {
        super.onResume();   
        
    	// restore persistent data
    	mPrefs = getSharedPreferences("quadrant", MODE_PRIVATE);
    	timerStartTime = mPrefs.getLong("timerStartTime", 0);
    	timerElapsedTime = mPrefs.getLong("timerElapsedTime", 0);
    	activityState = mPrefs.getInt("activityState", STATE_IDLE);
    	elapsedTime = mPrefs.getLong("elapsedTime", 0);
    	firstApplicationUse = mPrefs.getBoolean("firstApplicationUse", true);
        
        // check if the device has a GPS module
        PackageManager pm = getPackageManager();
        boolean hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        
    	if (hasGPS) {
    		// prompt to enable GPS if necessary
    		displayEnableGPS();
    	} else {
    		// The device doesn't support GPS (exit)
    		displayDeviceIncompatible();
    	}
    	
    	// display a help dialog for first time users
    	if (firstApplicationUse) {
    		firstApplicationUse = false;
    		SharedPreferences.Editor ed = mPrefs.edit();
    		ed.putBoolean("firstApplicationUse", firstApplicationUse);
    		ed.commit();
    		
    		displayHelpDialog();
    	}
    	
        // bind to GPS Service
        if (!serviceBound && activityState != STATE_IDLE) {
	    	bindMyService();
        }
        
        // restore GUI
    	TextView tv = (TextView) findViewById(R.id.textView_timer);
    	Button finishButton = (Button) findViewById(R.id.button_finish);
    	Button recordLed = (Button) findViewById(R.id.rec_led);
    	if (activityState == STATE_IDLE) {
    		//restore timer
    		tv.setText("00:00:00");
    		
    		// setup GUI status and button state
    		finishButton.setBackgroundResource(R.drawable.finish_gray);
    		recordLed.setBackgroundResource(R.drawable.clear_background);
    		
    	} else if(activityState == STATE_RECORDING) {
    		// restore timer
            int seconds = (int) (timerElapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            tv.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            // resume timer
    		timerHandler.postDelayed(timerRunnable, 0);
    		
    		// setup GUI status and button state
    		finishButton.setBackgroundResource(R.drawable.finish_button);
    		recordLed.setBackgroundResource(R.drawable.record_led);
    		
    	} else if (activityState == STATE_PAUSED) {
    		// restore timer
            int seconds = (int) (timerElapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            tv.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            
    		// setup GUI status and button state
    		finishButton.setBackgroundResource(R.drawable.finish_button);
    		recordLed.setBackgroundResource(R.drawable.paused_led);
    	}
    }

	@Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) {
        	if (myService != null) {
        		myService.removeResultCallback(this);
        	}
    		Log.d("GPS Service", "Unbinding");
    		unbindService(serviceConnection);
        	serviceBound = false;

        	// Keep watching the accelerometer when the app is paused
        	if (SERVICE_STOP_ON_PAUSE) {
        		Log.d(LOG_TAG, "Stopping.");
        		Intent intent = new Intent(this, GPSService.class);
        		stopService(intent);
        		Log.d(LOG_TAG, "Stopped.");
        		
        	}
        }
        
    	timerHandler.removeCallbacks(timerRunnable);
        
        // save persistent data
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putLong("timerStartTime", timerStartTime);
        ed.putLong("timerElapsedTime", timerElapsedTime);
        ed.putLong("elapsedTime", elapsedTime);
        ed.putInt("activityState", activityState);
        ed.commit();
    }
    
 // check if GPS is available and prompt the user to enable it if needed
 	private boolean displayEnableGPS() {
 		boolean GPSEnabled = false;
 		LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
 		GPSEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
 		if(!GPSEnabled) {
 		    //Ask the user to enable GPS
 		    AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		    builder.setTitle("Location Manager");
 		    builder.setMessage("This application requires GPS location data.  Would you like to enable GPS?");
 		    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
 		        @Override
 		        public void onClick(DialogInterface dialog, int which) {
 		            //Launch settings, allowing user to make a change
 		            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
 		            startActivity(i);
 		        }
 		    });
 		    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
 		        @Override
 		        public void onClick(DialogInterface dialog, int which) {
 		            //No location service, leave the activity
 		            finish();
 		        }
 		    });
 		    builder.create().show();
 		}
 		
 		return GPSEnabled;
 	}
 	
 	// notify the user that their device is incompatible with the Application
 	private void displayDeviceIncompatible() {
 		//Ask the user to enable GPS
 	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
 	    builder.setTitle("Warning");
 	    builder.setMessage("Your device does not support GPS location.  Press OK to exit.");
 	    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
 	        @Override
 	        public void onClick(DialogInterface dialog, int which) {
 	            //No location service, leave the activity
 	            finish();
 	        }
 	    });
 	    builder.create().show();
 	}
 	
    private void displayHelpDialog() {
 	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
 	    builder.setTitle("Welcome!");
 	    builder.setMessage("Instructions:\n"
 	    		+ "1) To start recording your location simply hit big shiny button and get going somewhere!\n\n"
 	    		+ "2) When you're done, press the \"FINISH\" button and your activity will automatically be saved.\n\n"
 	    		+ "3) To see all the data about your past activities press the \"HISTORY\" button on the "
 	    		+ "top-right corner of your screen.\n\nThanks for using Quadrant!\n"
 	    		+ "(This message will only appear once)");
 	    builder.setNegativeButton("Let's Go", new DialogInterface.OnClickListener() {
 	        @Override
 	        public void onClick(DialogInterface dialog, int which) {
 	        	// return
 	        }
 	    });
 	    builder.create().show();
	}
    
    // attempt to bind to MyService
    private void bindMyService() {
    	Intent intent = new Intent(this, GPSService.class);
    	Log.d("LOG_TAG", "Trying to bind");
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
    		Log.d("GPS Service", "Bound succeeded, adding the callback");
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
    	if (result == null) {
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
            		
            		// UPDATE GUI with service result here
            		String elevation = String.format("%.2f", result.elevation * 3.28084);
            		String distance = String.format("%.2f", result.distance * 0.000621371);
            		String speed = String.format("%.2f", result.speed * 2.23694);
            		
            		elevationTextView.setText(elevation);
            		distanceTextView.setText(distance);
            		velocityTextView.setText(speed);
            		
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
    	// handle the recording state
    	if (activityState == STATE_IDLE) {
    		activityState = STATE_RECORDING;
    		
            // Starts the GPS Service, the service stops when Finish is clicked
            Intent intent = new Intent(this, GPSService.class);
            startService(intent);
        	bindMyService();
    		
    		recordButton.setBackgroundResource(R.drawable.record_button_pause);
    		// Set timer start time and start the timer handler
    		timerStartTime = System.currentTimeMillis();
            timerHandler.postDelayed(timerRunnable, 0);
            
            // set finish button pressable
	    	Button button = (Button) findViewById(R.id.button_finish);
			button.setBackgroundResource(R.drawable.finish_button);
			// show recording indicator
			button = (Button) findViewById(R.id.rec_led);
			button.setBackgroundResource(R.drawable.record_led);
			
        	showToast("Recording");
    	} else if (activityState == STATE_RECORDING) {
    		activityState = STATE_PAUSED;
    		myService.notifyPaused();
    		recordButton.setBackgroundResource(R.drawable.record_button_resume);
    		// Pause the timer
    		timerHandler.removeCallbacks(timerRunnable);
    		//Pause service
    		myService.setServiceRunning(false);
    		
			// hide recording indicator
			Button button = (Button) findViewById(R.id.rec_led);
			button.setBackgroundResource(R.drawable.paused_led);
    		
        	showToast("Paused");
    	} else if (activityState == STATE_PAUSED) {
    		activityState = STATE_RECORDING;
    		recordButton.setBackgroundResource(R.drawable.record_button_pause);
    		// Resume the timer
    		timerStartTime = System.currentTimeMillis();
    		timerHandler.postDelayed(timerRunnable, 0);
    		
    		//Resume service
    		myService.setServiceRunning(true);
    		
			// show recording indicator
			Button button = (Button) findViewById(R.id.rec_led);
			button.setBackgroundResource(R.drawable.record_led);
    		
            // notify resumed
        	showToast("Resumed");
    	}
    }
    
    public void clickFinish(View v) {
    	
    	if (activityState != STATE_IDLE) {
	    	// reset clock
    		timerElapsedTime = 0;
    		
    		// update state, button
    		activityState = STATE_IDLE;
    		recordButton.setBackgroundResource(R.drawable.record_button);
    		
    		// stop counting
    		timerHandler.removeCallbacks(timerRunnable);
    		elapsedTime = 0;
    		
    		// save data from the service
    		JSONArray j = myService.getData();
    		if (j != null) {
        		Log.d("JSON DATA", j.toString());
        		saveSingleTrip(j);
    		} else {
    			showToast("No data! Trip discarded.");
    		}

    		// stop the service
	    	if (serviceBound) {
	    		try {
		        	if (myService != null) {
		        		myService.removeResultCallback(this);
		        	}
		    		Log.d("GPSService", "Unbinding");
		    		unbindService(serviceConnection);
		        	serviceBound = false;
		
		        	Log.d(LOG_TAG, "Stopping GPS Service.");
		        	Intent intent = new Intent(this, GPSService.class);
		        	stopService(intent);
		        	Log.d(LOG_TAG, "Stopped GPS Service.");
	        	
	    		} catch (Exception e) {
	    			Log.d(LOG_TAG, "ERROR stopping service: " + e.toString());
	    		}
	        }
	    	
	    	// show grayed out finish button
	    	Button button = (Button) findViewById(R.id.button_finish);
			button.setBackgroundResource(R.drawable.finish_gray);
			
			// hide recording indicator
			button = (Button) findViewById(R.id.rec_led);
			button.setBackgroundResource(R.drawable.clear_background);
    	} else {
    		showToast("You're not recording.  Doh!");
    	}
    }
    
    private void showToast(String msg) {
    	if (toast != null)
    	    toast.cancel();

    	toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT);
    	toast.show();
    }
    
    /** write a new trip to the preferences */
    private void saveSingleTrip(JSONArray jArray) {
    	if (mPrefs == null) {
    		mPrefs = getSharedPreferences("quadrant", MODE_PRIVATE);
    	}
    	try {
			JSONArray tripData = new JSONArray(mPrefs.getString("TRIPDATA", "[]"));
			
    		if(jArray.toString().equals("[]")) {
    			Log.d(LOG_TAG, "Saving first trip string to prefs...");
    		}
    		tripData.put(jArray);
    		// save persistent data
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putString("TRIPDATA", tripData.toString());
            ed.commit();
    		Log.d(LOG_TAG, "Saved trip data: " + tripData.toString());
    		showToast("Trip Saved!");
		} catch (JSONException e1) {
			Log.d(LOG_TAG, e1.toString());
		}
    }

}
