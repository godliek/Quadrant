package cmps121.quadrant;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class MapActivity extends FragmentActivity {
	
	private static final String LOG_TAG = "MapActivity";
	
    private PagerAdapter pagerAdapter;
    private ViewPager mPager;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // setup action bar
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        setContentView(R.layout.activity_map);
        
        // setup pager adapter
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(pagerAdapter);
        
        // Switch tabs when swiping between pages
        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        // This is required to avoid a black flash when the map is loaded.  The flash is due
        // to the use of a SurfaceView as the underlying view of the map.
        mPager.requestTransparentRegion(mPager);
        
        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
        	
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				// probably ignore this event
			}

			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				// Switch pages when tabs are selected
				mPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				// probably ignore this event
			}
        };
        
        // setup the Map and Details tab
        actionBar.addTab(actionBar.newTab().setText("Map").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("Details").setTabListener(tabListener));
        
    }

    /** A simple FragmentPagerAdapter that returns a SupportMapFragment and a TextFragment. */
    public static class PagerAdapter extends FragmentPagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "PAGE " + (position + 1);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case 0:
            	return new TripFragment();
            case 1:
                return new GraphFragment();
            default:
                return null;
            }
        }
    }
    

}
