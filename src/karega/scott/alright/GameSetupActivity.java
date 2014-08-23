package karega.scott.alright;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import android.app.ActionBar;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/*
 * Activity used to setup game parameters for play
 */
public class GameSetupActivity extends AlrightBaseActivity implements 
	OnClickListener,
	OnCloseListener,
	OnSuggestionListener,
	OnQueryTextListener
{
	private final static String LOG_TAG ="Alright Game Setup Activity";

	private ActionBar actionBar;
	private GoogleMap map;
	
	private TextView myDestinationText;
	private ImageView myDestinationIcon;
	private ImageView myLocationIcon;
	
	private TextView gameCardButtonsLeft;
	private TextView gameCardButtonsRight;
	
	private String queryText;
	
	/**
	 * Initializes all instance objects
	 */
	private void init() {
		Log.d(LOG_TAG, "Initialize instance objects");

		this.queryText = "";
		
		// Initialize all UI controls before anything else
		this.actionBar = this.getActionBar();
		this.actionBar.hide();
		
		this.gameCardButtonsLeft = (TextView)this.findViewById(R.id.game_card_buttons_left_textbox);
		this.gameCardButtonsRight = (TextView)this.findViewById(R.id.game_card_buttons_right_textbox);
		
		this.gameCardButtonsRight.setClickable(false);
		this.gameCardButtonsLeft.setClickable(false);
	
	
		this.myDestinationText = (TextView)this.findViewById(R.id.game_mydestination_textbox);
		this.myDestinationText.setOnClickListener(this);
	
		this.myLocationIcon = (ImageView)this.findViewById(R.id.game_mylocation_icon);
		this.myLocationIcon.requestFocus();
		
		this.myDestinationIcon = (ImageView)this.findViewById(R.id.game_mydestination_icon);

		if (this.map == null) {
			this.map = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.game_card_summary_map)).getMap();

			if (this.map == null) {
				// TODO: Device Issue Notify User
			}
			
			this.map.setMyLocationEnabled(true);
		} // end if
		
		this.manager.newGame();
	} // end init
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(LOG_TAG, "New intent initiated...");

		super.onNewIntent(intent);
		
		if( intent.getAction().equals(AlrightManager.ACTION_LOCATION_SUGGESTION) || intent.getAction().equals(Intent.ACTION_SEARCH)) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			this.manager.setMyDestination(query);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Create the activity");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_setup);
		
		this.init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "Creating the menu options for this actvity");
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		
		SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		searchView.setBackgroundResource(R.color.white);
		searchView.setIconified(false);
		
	    searchView.setSearchableInfo(this.manager.getSearchManager()
	    		.getSearchableInfo(getComponentName()));
	    
	    searchView.setOnSuggestionListener(this);
	    searchView.setOnQueryTextListener(this);
	    searchView.setOnCloseListener(this);
	    
		return super.onCreateOptionsMenu(menu);
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
	public void onClick(View v) {
		Log.d(LOG_TAG, String.format("Clicked %s",""));
		
		switch(v.getId()) {
			case R.id.game_card_buttons_left_textbox:
				this.startGameLocationTracker(AlrightManager.TURN_DIRECTION_LEFT);
				break;
			
			case R.id.game_card_buttons_right_textbox:
				this.startGameLocationTracker(AlrightManager.TURN_DIRECTION_RIGHT);
				break;
			
			case R.id.game_mydestination_textbox:
				this.actionBar.show();
				break;
		}				
	}
	
	/**
	 * Starts the game for play
	 * @param turnDirection
	 */
	private void startGameLocationTracker(int turnDirection) {
		Log.d(LOG_TAG, "Start game location tracker");

		this.manager.setTurnDirection(turnDirection);
		
		Intent intent = new Intent(this, LocationTrackerActivity.class);
		intent = new Intent(this, LocationTrackerActivity.class);
		this.startActivity(intent);
	} // end startGameLocationTracker
	
	@Override
	protected void onAlrightBaseActivityStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling base activity state changes...");
		
		this.actionBar.hide();
		
		Bitmap bm;
		switch(state.stateType) {
			case AlrightManager.STATE_TYPE_SUCCESS:
				break;
				
			case AlrightManager.STATE_TYPE_NEW_GAME:
				bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_directions_form_destination_notselected);
				this.myDestinationIcon.setImageBitmap(bm);
				this.myDestinationText.setText("");
				break;
			
			case AlrightManager.STATE_TYPE_MY_LOCATION:				
				bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_directions_form_mylocation);
				this.myLocationIcon.setImageBitmap(bm);
				
				this.animateCamera((Address)state.stateData, false, null, 0);
				break;
				
			case AlrightManager.STATE_TYPE_DESTINATION_CHANGED:
				bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_directions_form_startpoint);
				this.myDestinationIcon.setImageBitmap(bm);
				this.myDestinationText.setText(AlrightManager.addressToString((Address)state.stateData));
				this.animateCamera((Address)state.stateData, true, "Your destination", BitmapDescriptorFactory.HUE_VIOLET);
				
				this.gameCardButtonsRight.setOnClickListener(this);
				this.gameCardButtonsLeft.setOnClickListener(this);
				break;
		}
	} 
	
	/**
	 * Move the map into focus for the address
	 * @param address
	 * @param addressTitle
	 * @param hue
	 * @param addMarker
	 */
	private void animateCamera(Address address, boolean addMarker, String title, float hue) {
		Log.d(LOG_TAG, String.format("Animating camera for %s",address));
		
		LatLng latlng = new LatLng(address.getLatitude(), address.getLongitude());
		
		if(addMarker) {
			MarkerOptions options = new MarkerOptions().position(latlng)
					.title(title)
					.icon(BitmapDescriptorFactory.defaultMarker(hue));
			this.map.addMarker(options);
		}
		
		this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 12.0f));
	} // end animateCamera

	@Override
	public boolean onSuggestionClick(int position) {
		this.queryText = AlrightManager.addressToString(this.manager.getAddress(queryText, position));
		if(this.queryText == null)
			return false;
		
		// TODO: Review this code. Should position be included on content uri.
		// For instance String query = String.format("content://karega.scott.alright.provider/suggestion_query/%s ?", position);
		Intent intent = new Intent(this, GameSetupActivity.class);
		
		intent.setAction(AlrightManager.ACTION_LOCATION_SUGGESTION);
		intent.putExtra(SearchManager.QUERY, this.queryText);
		
		this.startActivity(intent);
		return true;
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		this.queryText = newText;
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		this.queryText = query;
		return false;
	}

	@Override
	public boolean onClose() {
		this.actionBar.hide();
		return true;
	}
} // end GameActivity
