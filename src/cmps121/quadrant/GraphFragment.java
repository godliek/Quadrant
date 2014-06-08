package cmps121.quadrant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
	private XYSeries mCurrentSeries;
	/** The main renderer that includes all the renderers customizing a chart. */
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	/** The most recently created renderer, customizing the current series. */
	private XYSeriesRenderer mCurrentRenderer;
	/** The chart view that displays the data. */
	private GraphicalView mChartView;
	
	//Layout that will contain the graph when drawn
	private LinearLayout layout;
	
	private static final String LOG_TAG = "GraphFragment";
	
    private SharedPreferences mPrefs;

	protected static ArrayList<GPSEntry> savedTrips;
	
	//Spinners to hold the different trips and choose the type of graph
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
        mRenderer.setMargins(new int[] { 20, 45, 15, 0 });
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setPointSize(5);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setShowGrid(true);
        
 		try {
 			String data = mPrefs.getString("TRIPDATA", "[]");
 			JSONArray tripData = new JSONArray(data);
 			Log.d("JSON", tripData.toString());
 			for(int i = 0; i < tripData.length(); i++) {
 				JSONArray jArr = (JSONArray) tripData.get(i);
 				if(!jArr.toString().equals("[]")) {
 					GPSEntry g = new GPSEntry(jArr);
 				
 					//Get time of first GPS data in JSON array
 					JSONObject jObj = (JSONObject) jArr.get(0);
 					
 					//Maybe use time in graph later
 					String time = jObj.getString("time");
 					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
 					String dateStarted = formatter.format(new Date(Long.parseLong(time)));
 				
 					g.titleText = dateStarted;
 					savedTrips.add(g);
 				}
 			}
 		} catch (JSONException e) {
 			e.printStackTrace();
 		}
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
        graphTypes.add("Total Elevation Gain Over Time");
        graphTypes.add("Distance Over Time");
        
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item,graphTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(this);
        typeAdapter.notifyDataSetChanged();
        

 		mRenderer.setXTitle("Time");
        mRenderer.setYTitle("Elevation (ft)");
 		

 		mCurrentRenderer = new XYSeriesRenderer();
 		mCurrentRenderer.setLineWidth(5);
 		mCurrentRenderer.setColor(Color.argb(100, 18, 179, 12));
 		mRenderer.addSeriesRenderer(mCurrentRenderer);
 		
 		layout = (LinearLayout) getActivity().findViewById(R.id.graphLayout);
 		Log.d(LOG_TAG,"onstart finished");
	}
	
   
	@Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		Log.d(LOG_TAG, "spinner entry selected");

		//Make sure there is information to display before creating a graph.
		if(savedTrips.size() != 0) {
			//Clear graph data
			mDataset.clear();
			Log.d("trip spinner", "Trip Index Selected: " + tripSpinner.getSelectedItemPosition());
			GPSEntry g = savedTrips.get(tripSpinner.getSelectedItemPosition());
			Log.d("type spinner","Graph Index Selected: " + typeSpinner.getSelectedItemPosition());
			String graphType = typeSpinner.getSelectedItem().toString();
			XYSeries series = new XYSeries(graphType);

			//Generate new graph
			JSONArray jArr = g.data;
			Log.d("testData", jArr.toString());
			mRenderer.setXLabels(8);
	 		for(int i = 0; i < jArr.length(); i++) {
	 			int time = i *2;
	 			try {
					JSONObject jObj = jArr.getJSONObject(i);
					double graphData = 0;
					if(graphType.equals("Elevation Over Time")) {
						Log.d("testData", "" + i + ": " + jObj.getString("elev"));
						graphData = Double.parseDouble(jObj.getString("elev"));
						series.add(time, graphData);
						mRenderer.setXTitle("Time (s)");
				        mRenderer.setYTitle("Elevation (ft)");
					} else {
						if(graphType.equals("Total Elevation Gain Over Time")) {
							Log.d("testData", "" + i + ": " + jObj.getString("totalElev"));
							graphData = Double.parseDouble(jObj.getString("totalElev"));
							series.add(time, graphData);
							mRenderer.setXTitle("Time (s)");
					        mRenderer.setYTitle("Total Elevation Gain(ft)");
						} else {
							if(graphType.equals("Distance Over Time")) {
								Log.d("testData", "" + i + ": " + jObj.getString("totalDistance"));
								graphData = Double.parseDouble(jObj.getString("totalDistance"));
								series.add(time, graphData);
								mRenderer.setXTitle("Time (s)");
						        mRenderer.setYTitle("Distance (mi)");
							}
						}
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	 			
	 		}
	 		mCurrentSeries = series;
	 		mDataset.addSeries(series);
		    Log.d("LOG_TAG", "" + mDataset.getSeriesCount() + " " + mRenderer.getSeriesRendererCount());
	 		mChartView = ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
	 		layout.removeAllViews();
	 		layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	  @Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(LOG_TAG, "onSaveInstanceState called");
	    super.onSaveInstanceState(outState);
	    // save the current data, for instance when changing screen orientation
	    mRenderer.removeAllRenderers();
	    outState.putSerializable("dataset", mDataset);
	    outState.putSerializable("renderer", mRenderer);
	    outState.putSerializable("current_series", mCurrentSeries);
	    outState.putSerializable("current_renderer", mCurrentRenderer);
	  }

	  
	  @Override
	  public void onActivityCreated(Bundle savedInstanceState) {
	  super.onActivityCreated(savedInstanceState);
	  Log.d(LOG_TAG, "onActivityCreated called");
	  if (savedInstanceState != null) {
		  Log.d(LOG_TAG, "onactivitycreated instance is not null");
	          // Restore last state for checked position.
	        mDataset = (XYMultipleSeriesDataset) savedInstanceState.getSerializable("dataset");
	  	    mRenderer = (XYMultipleSeriesRenderer) savedInstanceState.getSerializable("renderer");
	  	    mCurrentSeries = (XYSeries) savedInstanceState.getSerializable("current_series");
	  	    mCurrentRenderer = (XYSeriesRenderer) savedInstanceState.getSerializable("current_renderer");
	  	    
		    if (mChartView == null) {
			      LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.graphLayout);
			      mDataset.addSeries(mCurrentSeries);
			      mChartView = ChartFactory.getLineChartView(getActivity(), mDataset, mRenderer);
			      // enable the chart click events
			      mRenderer.setClickEnabled(true);
			      mRenderer.setSelectableBuffer(10);
			      layout.removeAllViews();
			      layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT,
			          LayoutParams.FILL_PARENT));
			      boolean enabled = mDataset.getSeriesCount() > 0;
			    } else {
			      mChartView.repaint();
			    }
	      }
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