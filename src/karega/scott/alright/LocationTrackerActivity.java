package karega.scott.alright;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.TrackingDetails;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

/*
 * Activity used for actual game play
 */
public class LocationTrackerActivity extends AlrightBaseActivity {
	// NOTE: LOG_TAG should not exceed 23 characters! ;-)
	private final static String LOG_TAG ="Alright Tracker Activit";

	private GoogleMap map;
	private TextView summary;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Creating the activity");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location_tracker);
		
		// NOTE: Not for production use
		if(/* TODO: Debug Only*/ true) {
			this.summary = (TextView)this.findViewById(R.id.location_tracker_summary);
			this.summary.setText("");
			
			this.map = ((MapFragment) getFragmentManager().findFragmentById(
						R.id.location_tracker_map)).getMap();
	
			this.map.setMyLocationEnabled(true);
		}
		
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
	
	@Override
	protected void onAlrightBaseActivityStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling base activity state changes...");

		switch(state.stateType) {
			case AlrightManager.STATE_TYPE_MY_LOCATION:
				Address address = (Address)state.stateData;
				LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());		
				this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 12.0f));
				break;
				
			case AlrightManager.STATE_TYPE_STILL_ON_TRACK:
				this.showTrackingDetails((TrackingDetails)state.stateData);
				break;
				
			case AlrightManager.STATE_TYPE_GAME_STARTED:				
				break;
							
		}
	}
	
	/*
	 * Shows the tracking details 
	 */
	private void showTrackingDetails(TrackingDetails data) {
		Log.d(LOG_TAG, String.format("Tracking Detals: %s", data));

		if(/* TODO: Debug Only */ true) {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("Provider:    %s\n", data.Provider));
			builder.append(String.format("Direction:    %s\n", data.Direction));
			builder.append(String.format("Pitch (X):   %s\n", data.Axis_X_Pitch));
			builder.append(String.format("Roll (Y):    %s\n", data.Axis_Y_Roll));
			builder.append(String.format("Heading (Z): %s\n", data.Axis_Z_Heading));
			
			this.summary.setText(builder.toString());
		}
	} // end showTrackingDetails
} // end GameActivity
