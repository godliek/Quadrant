package cmps121.quadrant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends Activity{
	
	private Button b;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        b = (Button) findViewById(R.id.clearButton);
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
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here.
		int id = item.getItemId();
		if (id == R.id.action_history) {
			Intent intent = new Intent(this, MapActivity.class);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_help) {
			displayHelpDialog();
			return true;
		}
		
		
		return super.onOptionsItemSelected(item);
	}
    
    public void onClearHistory(View v){
    	Context c = getApplicationContext();
    	if(c.deleteFile("TRIPDATA.TXT")) {
    		Log.d("delete", "data deleted");
    	}
    	SharedPreferences mPrefs = getSharedPreferences("quadrant", MODE_PRIVATE);
    	SharedPreferences.Editor ed = mPrefs.edit();
    	ed.remove("TRIPDATA");
    	ed.putString("TRIPDATA", "[]");
    	ed.commit();
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
}
