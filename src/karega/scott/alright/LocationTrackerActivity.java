package karega.scott.alright;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.TrackingDetails;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

/*
 * Activity used for actual game play
 */
public class LocationTrackerActivity extends AlrightBaseActivity {
	// NOTE: LOG_TAG should not exceed 23 characters! ;-)
	private final static String LOG_TAG ="LocationTracker";

	private LinearLayout container;
	private GoogleMap containerMap;
	private TextView containerSummary;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Creating the activity");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_tracker);
		
		// NOTE: Not for production use
		if(/* TODO: Debug Only*/ true) {
			this.container = (LinearLayout)this.findViewById(R.id.location_tracker_container);
			
			this.containerSummary = (TextView)this.findViewById(R.id.location_tracker_container_summary);
			this.containerSummary.setText("");
			
			this.containerMap = ((MapFragment) getFragmentManager().findFragmentById(
						R.id.location_tracker_container_map)).getMap();
	
			this.containerMap.setMyLocationEnabled(true);
		} // end Not for production use
		
		this.manager.startGame();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "Creating menu options for the activity");
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game_setup, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, "Menu item selected for this activity");
		
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
		Log.d(LOG_TAG, "Handling manager state changes...");

		switch(state.stateType) {
			case AlrightManager.STATE_TYPE_MY_LOCATION:
				Address address = (Address)state.stateData;
				LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());		
				this.containerMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 12.0f));
				break;
				
			case AlrightManager.STATE_TYPE_STILL_ON_TRACK:
				this.showTrackingDetails((TrackingDetails)state.stateData);
				break;				
		}
		
		this.container.invalidate();
	}
	
	/*
	 * Shows the tracking details 
	 */
	private void showTrackingDetails(TrackingDetails data) {
		Log.d(LOG_TAG, String.format("Tracking Detals: %s", data));

		if(/* TODO: Debug Only */ true) {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("Provider:        %s\n", data.Provider));
			builder.append(String.format("Direction:       %s\n", data.Direction));
			builder.append(String.format("Direction Prev:  %s\n", data.Direction_Previous));
			builder.append(String.format("Pitch (X):       %s\n", data.Pitch));
			builder.append(String.format("Roll (Y):        %s\n", data.Roll));
			builder.append(String.format("Heading (Z):     %s\n", data.Azimuth));
			
			this.containerSummary.setText(builder.toString());
		}
	} // end showTrackingDetails
} // end LocationTrackerActivity
