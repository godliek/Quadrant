package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;


/** GPSInfo
 * 		Represents a single record of GPS data and provides an interface between
 * 		all Activities with these convenience methods:
 * 
 * 		insertLocation()
 * 		insertPause()
 *		getDistance()
 *		getElevation()
 *		getAverageSpeed()
 *		getElapsedTime()
 *		getLatitude()
 *		getLongitude()
 *		size()
 *		toJSONArray()
 *		fromJSONArray()
 *		clear()		
 *
 *		TODO:
 *		fromJSONArray()
 */
public class GPSData {
	// list of polled GPS data
	private ArrayList<GPSNode> gpsList;
	private ArrayList<Long> pauseList;
	private static final String LOG_TAG = "GPSInfo";
	
	// Constructor
	GPSData() {
		gpsList = new ArrayList<GPSNode>();
		pauseList = new ArrayList<Long>();
	};
	
	// Insert polled GPS data to the sequence
	public void insertLocation(double lat, double lon, double alt, long t) {
		// discard garbage location readings
		if (t == 0) 
			return;
		// discard duplicate location readings
		if (gpsList.size() > 0 && gpsList.get(gpsList.size()-1).time == t)
			return;
		
		//insert a new location
		gpsList.add(new GPSNode(lat, lon, alt, t));
	}
	
	// Insert a pause stop
	public void insertPause() {
		pauseList.add(System.currentTimeMillis());
	}
	
	// Return distance in meters
	public double getDistance() {
		double distance = 0.0;
		double prevLatitude, prevLongitude, curLatitude, curLongitude;
		long prevTime, curTime;
		float results[] = new float[3];
		
		if (gpsList.size() < 2) {
			return 0.0;
		}
		
		prevTime = gpsList.get(0).time;
		prevLatitude = gpsList.get(0).latitude;
		prevLongitude = gpsList.get(0).longitude;
		for (int i = 1; i < gpsList.size(); i++) {
			curLatitude = gpsList.get(i).latitude;
			curLongitude = gpsList.get(i).longitude;
			curTime = gpsList.get(i).time;
			Location.distanceBetween(prevLatitude, prevLongitude, curLatitude, curLongitude, results);
			
			if (!isPaused(prevTime, curTime)) {
				distance += results[0];
			}
			
			prevLatitude = curLatitude;
			prevLongitude = curLongitude;
			prevTime = curTime;
		}
		return distance;
	}
	
	// Calculates total elevation change in meters
	public double getElevation() {
		double delta = 0.0;
		double prevAltitude, curAltitude;
		long prevTime, curTime;
		if (gpsList.size() < 2) {
			return 0.0;
		}
		
		prevAltitude = gpsList.get(0).altitude;
		prevTime = gpsList.get(0).time;
		for (int i = 1; i < gpsList.size(); i++) {
			curAltitude = gpsList.get(i).altitude;
			curTime = gpsList.get(i).time;
			if (!isPaused(prevTime, curTime)) {
				delta += curAltitude - prevAltitude;
			}
			prevTime = curTime;
			prevAltitude = curAltitude;
		}
		
		return delta;
	}
	
	
	// Calculate the average speed in meters/second
	public double getAverageSpeed() {
		double meters = getDistance();
		double seconds = (double)(getElapsedTime()/1000); // seconds
		if (meters == 0 || seconds == 0) {
			return 0.0;
		}
		return (meters / seconds);
	}
	
	// Return the elapsed time in milliseconds
	public long getElapsedTime() {
		long elapsedTime = 0;
		long curTime, prevTime;
		if (gpsList.size() < 2) {
			return 0;
		}
		
		prevTime = gpsList.get(0).time;
		for(int i = 0; i < gpsList.size(); i++) {
			curTime = gpsList.get(i).time;
			if (!isPaused(prevTime, curTime)) {
				elapsedTime += curTime - prevTime;
			}
			prevTime = curTime;
		}
		return elapsedTime;
	}
	
	// return latitude
	public double getLatitude(int index) {
		return gpsList.get(index).latitude;
	}
	
	// return longitude
	public double getLongitude(int index) {
		return gpsList.get(index).longitude;
	}
	
	// return number of locations
	public double size() {
		return gpsList.size();
	}
	
	// Convert the sequence into a JSONArray
	public JSONArray toJSONArray() {
		if (gpsList.size() == 0)
			return null;
		
		JSONArray jArray = new JSONArray();
		for (int i = 0; i < gpsList.size(); i++) {
			JSONObject j = new JSONObject();
			try {
				j.put("lat", String.valueOf(gpsList.get(i).latitude));
				j.put("long", String.valueOf(gpsList.get(i).longitude));
				j.put("time", String.valueOf(gpsList.get(i).time));
				j.put("elev", String.valueOf(gpsList.get(i).altitude));
				j.put("totalElev", String.valueOf(getElevation() * 3.28084));
				j.put("totalDistance", String.valueOf(getDistance() * 0.000621371));
				jArray.put(j);
			} catch (JSONException e) {
				Log.d(LOG_TAG, "ERROR toJSONArray()\n" + e.toString());
			}
		}
		return jArray;
	}
	
	// Returns a GPSData object constructed from a JSONArray
	public static GPSData fromJSONArray(JSONArray jArray) {
		GPSData data = new GPSData();
		
		try {
			// iterate through the JSONArray
			for(int i = 0; i < jArray.length(); i++) {
				JSONObject jObj = jArray.getJSONObject(i);
				//get data from JSON
				double lat = Double.parseDouble(jObj.get("lat").toString());
				double lng = Double.parseDouble(jObj.get("long").toString());
				double elev = Double.parseDouble(jObj.getString("elev").toString());
				long time = Long.parseLong(jObj.getString("time"));
				
				data.insertLocation(lat, lng, elev, time);
			}
		} catch (JSONException e) {
			Log.d(LOG_TAG, "ERROR fromJSONArray()\n" + e.toString());
		}
		return data;
	}
	
	// clear location and pause data.
	public void clear() {
		gpsList.clear();
		pauseList.clear();
	}
	
	// dump to log (DEBUG)
	public void print() {
		for (int i = 0; i < gpsList.size(); i++) {
			double lat = gpsList.get(i).latitude;
			double lon= gpsList.get(i).longitude;
			double alt = gpsList.get(i).altitude;
			long time = gpsList.get(i).time;
			Log.i(LOG_TAG, "lat: " + lat + " lon: " + lon + " alt: " + alt + " t: " + time);
		}
		Log.i(LOG_TAG, "dist:" + getDistance() + " elev: " + getElevation() + " speed: " + getAverageSpeed());
	}
	
	/*	isPaused
	 *  	check if a pause interrupts the start and end time.
	 *  
	 *  @return true  - no interruption
	 *  		false - interrupted
	 */
	private boolean isPaused(long start, long end) {
		for (int i = 0; i < pauseList.size(); i++) {
			long pause = pauseList.get(i).longValue();
			if (start <= pause && end >= pause){
				return true;
			}
		}
		return false;
	}
	
    private static String getTimeFromEpoch(String epoch) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
		String currentTime = formatter.format(new Date(Long.parseLong(epoch)));
		return currentTime;
    }
	
	/** GPSNode
	 * 		list node for polled GPS data
	 */
	public class GPSNode {
		GPSNode(double lat, double lon, double alt, long t){
			latitude = lat;
			longitude = lon;
			altitude = alt;
			time = t;
		};
		
		public double longitude;
		public double latitude;
		public double altitude;
		public long time;
	}
}
