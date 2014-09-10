package karega.scott.alright;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.Activity;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * Activity used for actual game play
 */
public class LocationTrackerActivity extends Activity implements
	ManagerStateListener
{
	// NOTE: LOG_TAG should not exceed 23 characters! ;-)
	private final static String LOG_TAG ="LocationTracker";

	private AlrightManager manager;
	private LinearLayout container;
	private GoogleMap containerMap;
	private TextView containerSummary;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_tracker);
		
		this.manager = AlrightManager.getInstance(this.getApplicationContext());
		
		// NOTE: Not for production use
		if(/* TODO: Debug Only*/ true) {
			this.container = (LinearLayout)this.findViewById(R.id.location_tracker_container);
			
			this.containerSummary = (TextView)this.findViewById(R.id.location_tracker_container_summary);
			this.containerSummary.setText("");
			
			this.containerMap = ((MapFragment) getFragmentManager().findFragmentById(
						R.id.location_tracker_container_map)).getMap();
	
			this.containerMap.setMyLocationEnabled(true);
		} // end Not for production use		
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart");
		
		super.onStart();
		
		// Activity visible start listening
		this.manager.startTracker(this);
	}

	@Override
	protected void onResume() {
		//Log.d(LOG_TAG, "onResume");
		// NO NEED onStart handles this case
		
		super.onResume();
		
		this.manager.startTracker(this);
	}

	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "onPause");
		
		super.onPause();

		this.manager.stopTracker(this);
	}
	
	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop");
		
		super.onStop();
		
		this.manager.stopTracker(this);
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");
	
		this.container = null;
		this.containerMap = null;
		this.containerSummary = null;
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game_setup, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, String.format("onManagerStateChanged %s", state));

		switch(state.stateType) {
			case MY_LOCATION:
				Address address = (Address)state.stateData;
				LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());		
				this.containerMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 12.0f));
				break;
			
			
			case GAME_OVER_LOSER:
			case GAME_OVER_WINNER:
			case STILL_ON_TRACK:
				this.showTrackingDetails((Location)state.stateData);
				break;
			
		}
		
		this.containerSummary.invalidate();
		this.container.invalidate();
	}
	
	/*
	 * Shows the tracking details 
	 */
	private void showTrackingDetails(Location location) {
		if(/* TODO: Debug Only */ true) {
			
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("Provider:  %s\n", location.getProvider()));
			builder.append(String.format("Latitude:  %s\n", location.getLatitude()));
			builder.append(String.format("Longitude: %s\n", location.getLongitude()));
			builder.append(String.format("Bearing:   %s\n", location.getBearing()));
			builder.append(String.format("Direction: %s\n", AlrightManager.computeCompassDirection(location.getBearing())));
			
			this.containerSummary.setText(builder.toString());
		}
	} // end showTrackingDetails
} // end LocationTrackerActivity
