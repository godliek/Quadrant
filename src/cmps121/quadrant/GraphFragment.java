package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;










import cmps121.quadrant.TripFragment.GPSEntry;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

/** A MapFragment with selectable recorded trips */
public class GraphFragment extends Fragment implements OnItemSelectedListener {
	
	//Graph Items
	/** The main dataset that includes all the series that go into a chart. */
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	/** The main renderer that includes all the renderers customizing a chart. */
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	/** The most recently added series. */
	private XYSeries mCurrentSeries;
	/** The most recently created renderer, customizing the current series. */
	private XYSeriesRenderer mCurrentRenderer;
	/** The chart view that displays the data. */
	private GraphicalView mChartView;
	
	private static final String LOG_TAG = "TripFragment";
	
    private SharedPreferences mPrefs;

	protected static ArrayList<GPSEntry> savedTrips;
	
	private Spinner tripSpinner;
	private Spinner typeSpinner;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
        View view  = inflater.inflate(R.layout.graph_fragment, container, false);

        return view;
    }
	
	@Override
	public void onStart() {
		super.onStart();
		
		savedTrips = new ArrayList<GPSEntry>();
        // initialize class members
     	mPrefs = getActivity().getSharedPreferences("quadrant", Context.MODE_PRIVATE);
     	
     	//Set up spinners
     	tripSpinner = (Spinner) getActivity().findViewById(R.id.graph_trip_spinner);
     	typeSpinner = (Spinner) getActivity().findViewById(R.id.graph_type_spinner);
     	
     	//Set up graph
     	// set some properties on the main renderer
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
        mRenderer.setAxisTitleTextSize(16);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);
        mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setPointSize(5);
        

        List<Double> elevations = new ArrayList<Double>();
 		try {
 			String data = mPrefs.getString("TRIPDATA", "[]");
 			JSONArray tripData = new JSONArray(data);
 			Log.d("JSON", tripData.toString());
 			for(int i = 0; i < tripData.length(); i++) {
 				Log.d("loop", "trying to add item to listview");
 				JSONArray jArr = (JSONArray) tripData.get(i);
 				if(!jArr.toString().equals("[]")) {
 					GPSEntry g = new GPSEntry(jArr);
 				
 					//Get time of first GPS data in JSON array
 					JSONObject jObj = (JSONObject) jArr.get(0);
 					
 					//get elevation
 					double elev = Double.parseDouble(jObj.getString("elev"));
 					elevations.add(elev);
 					//Maybe use time in graph later
 					String time = jObj.getString("time");
 					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
 					String dateStarted = formatter.format(new Date(Long.parseLong(time)));
 				
 					
 					g.titleText = dateStarted;
 					savedTrips.add(g);
 				}
 			}
 		} catch (JSONException e) {
 			Log.d(LOG_TAG, e.toString());
 		}
 		Collections.reverse(elevations);
 		Collections.reverse(savedTrips);	//Reverse the list; it is in the order in which the records were saved(newest last)
 		
 		//Set up spinner items for trips
        ArrayAdapter<GPSEntry> adapter = new ArrayAdapter<GPSEntry>(getActivity(), android.R.layout.simple_spinner_item, savedTrips);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tripSpinner.setAdapter(adapter); 
        tripSpinner.setOnItemSelectedListener(this);
        adapter.notifyDataSetChanged();
        
        //Set up spinner items for graph types
        ArrayList<String> graphTypes = new ArrayList();
        graphTypes.add("Elevation Over Time");
        graphTypes.add("Total Elevation Over Time");
        graphTypes.add("Distance Over Time");
        
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,graphTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        tripSpinner.setOnItemSelectedListener(this);   		//CHANGE THIS
        typeAdapter.notifyDataSetChanged();
        
 		GPSEntry g = savedTrips.get(0);
 		
 		XYSeries series = new XYSeries("Elevation Over Time");
 		JSONArray jArr = g.data;
 		for(int i = 0; i < jArr.length(); i++) {
 			try {
				JSONObject jObj = jArr.getJSONObject(i);
				Log.d("testData", "" + i + ": " + jObj.getString("elev"));
				series.add(i, Double.parseDouble(jObj.getString("elev")));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 			
 		}
 		
 		mDataset.addSeries(series);
 		mCurrentRenderer = new XYSeriesRenderer();
 		mRenderer.addSeriesRenderer(mCurrentRenderer);
 		
 		LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.graphLayout);
 		
 		mChartView = ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
 		layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
 		//mChartView.repaint();
 		// setup spinner
 		/*
        Spinner mySpinner = (Spinner) getView().findViewById(R.id.trip_spinner);
        ArrayAdapter<GPSEntry> adapter = new ArrayAdapter<GPSEntry>(getActivity(), android.R.layout.simple_spinner_item, savedTrips);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter); 
        mySpinner.setOnItemSelectedListener(this);
        adapter.notifyDataSetChanged();*/
	}
	
   
	@Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.d(LOG_TAG, "spinner entry selected");
    }

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	

	
	/** GPSEntry
	 *    ListView element
	 */
	public class GPSEntry {
		GPSEntry() {};
		GPSEntry(JSONArray jArr) {
			data = jArr;
		}
		String titleText;
		JSONArray data;
		
		@Override
		public String toString() {
			return titleText;
		}
	}
}