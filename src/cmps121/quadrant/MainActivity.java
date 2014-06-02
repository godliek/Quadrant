package cmps121.quadrant;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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
    
    // Notification
    private static Toast toast;

    private long elapsedTime = 0;
    
    //UI Elements
    private TextView elevationTextView, distanceTextView, velocityTextView;
    private Button recordButton;
    
    // Persistent data
    private SharedPreferences mPrefs;
    
    //Stored JSON data
    final private String fileName = "TRIPDATA.TXT";
    private JSONArray tripHistory;
    
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
    	
    	tripHistory = new JSONArray();
    	
    	//try to load data from file
    	Log.d("load","trying to load file");
    	StringBuffer datax = new StringBuffer("");
    	try {
			FileInputStream fis = openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis) ;
            BufferedReader buffreader = new BufferedReader(isr) ;

            String readString = buffreader.readLine ( ) ;
            while ( readString != null ) {
                datax.append(readString);
                readString = buffreader.readLine ( ) ;
            }
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    	Log.d("opening file", datax.toString());
    	
    	try {
			tripHistory = new JSONArray(datax.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


    	
    	// restore persistent data
    	mPrefs = getSharedPreferences("quadrant", MODE_PRIVATE);
    	timerStartTime = mPrefs.getLong("timerStartTime", 0);
    	timerElapsedTime = mPrefs.getLong("timerElapsedTime", 0);
    	activityState = mPrefs.getInt("activityState", STATE_IDLE);
    	elapsedTime = mPrefs.getLong("elapsedTime", 0);
  
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
        if (!serviceBound && activityState != STATE_IDLE) {
	    	bindMyService();
        }
        
        // check if the device has a GPS module
        PackageManager pm = getPackageManager();
        boolean hasGPS = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        
    	if (hasGPS) {
    		// prompt to enable GPS if necessary
    		promptEnableGPS();
    	} else {
    		// The device doesn't support GPS (exit)
    		notifyDeviceIncompatible();
    	}
        
        // restore GUI
    	TextView tv = (TextView) findViewById(R.id.textView_timer);
    	if (activityState == STATE_IDLE) {
    		//restore timer
    		tv.setText("00:00:00");
    		
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
    	} else if (activityState == STATE_PAUSED) {
    		// restore timer
            int seconds = (int) (timerElapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;
            tv.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

    	}
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) {
        	if (myService != null) {
        		myService.removeResultCallback(this);
        	}
    		Log.i("GPS Service", "Unbinding");
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
        
        // save persistent data
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putLong("timerStartTime", timerStartTime);
        ed.putLong("timerElapsedTime", timerElapsedTime);
        ed.putLong("elapsedTime", elapsedTime);
        ed.putInt("activityState", activityState);
        ed.commit();
    }
    
 // check if GPS is available and prompt the user to enable it if needed
 	private boolean promptEnableGPS() {
 		boolean GPSEnabled = false;
 		LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
 		GPSEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
 		if(!GPSEnabled) {
 		    //Ask the user to enable GPS
 		    AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		    builder.setTitle("Location Manager");
 		    builder.setMessage("This application requires GPS Location data.  Would you like to enable GPS?");
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
 	private void notifyDeviceIncompatible() {
 		//Ask the user to enable GPS
 	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
 	    builder.setTitle("Warning");
 	    builder.setMessage("Your device does not support GPS location services.  Press OK to exit.");
 	    builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
 	        @Override
 	        public void onClick(DialogInterface dialog, int which) {
 	            //No location service, leave the activity
 	            finish();
 	        }
 	    });
 	    builder.create().show();
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
			intent.putExtra("tripHistory", tripHistory.toString());
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    // attempt to bind to MyService
    private void bindMyService() {
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
    		Log.i("GPS Service", "Bound succeeded, adding the callback");
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
            		String elev = String.format("%.2f", result.elevation);
            		String dist = String.format("%.2f", result.distance);
            		elevationTextView.setText(elev);
            		distanceTextView.setText(dist);
            		
            		double mi = result.distance;
            		double h = (double) elapsedTime / (1000 * 60 * 60);
            		double mph = mi/h;
            		Log.d("time",  "" + elapsedTime);
            		
            		String velocity = String.format("%.2f", mph);
            		velocityTextView.setText(velocity);

            		
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
            
        	showToast("Now Recording");
    	} else if (activityState == STATE_RECORDING) {
    		activityState = STATE_PAUSED;
    		recordButton.setBackgroundResource(R.drawable.record_button_resume);
    		// Pause the timer
    		timerHandler.removeCallbacks(timerRunnable);
    		//Pause service
    		myService.setServiceRunning(false);
    		
        	showToast("Paused");
    	} else if (activityState == STATE_PAUSED) {
    		activityState = STATE_RECORDING;
    		recordButton.setBackgroundResource(R.drawable.record_button_pause);
    		// Resume the timer
    		timerStartTime = System.currentTimeMillis();
    		timerHandler.postDelayed(timerRunnable, 0);
    		
    		//Resume service
    		myService.setServiceRunning(true);
    		
            // notify resumed
        	showToast("Resumed");
    	}
    }
    
    public void clickFinish(View v) {
    	
    	if (activityState != STATE_IDLE) {
	    	//Reset clock
    		timerElapsedTime = 0;
    		
    		
    		//update state, button
    		activityState = STATE_IDLE;
    		recordButton.setBackgroundResource(R.drawable.rec_button);
    		//stop counting
    		timerHandler.removeCallbacks(timerRunnable);
    		elapsedTime = 0;
    		
    		JSONArray j = myService.getData();
    		Log.d("JSON DATA", j.toString());
    		if(!j.toString().equals("[]"))
    			tripHistory.put(j);
    		
    		Log.d("JSON DATA", tripHistory.toString());
    		
    		//TODO: check the json data to make sure that it is not empty before writing
    		
    		FileOutputStream fos;
			try {
				fos = openFileOutput(fileName, Context.MODE_PRIVATE);
	    		fos.write(tripHistory.toString().getBytes());
	    		fos.close();
	    		showToast("Trip Saved!");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		// stop the service
    		
	    	if (serviceBound) {
	        	if (myService != null) {
	        		myService.removeResultCallback(this);
	        	}
	    		Log.i("GPSService", "Unbinding");
	    		unbindService(serviceConnection);
	        	serviceBound = false;
	
	        	Log.i(LOG_TAG, "Stopping GPS Service...");
	        	Intent intent = new Intent(this, GPSService.class);
	        	stopService(intent);
	        	Log.i(LOG_TAG, "Stopped  GPS Service.");
	        }
	    	
	    	showToast("GPS Service stopped");
    	} else {
    		showToast("Nothing to finish");
    	}
    }
    
    private void showToast(String msg) {
    	if (toast != null)
    	    toast.cancel();

    	toast = Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT);
    	toast.show();
    }

}
