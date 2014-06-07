package cmps121.quadrant;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
}
