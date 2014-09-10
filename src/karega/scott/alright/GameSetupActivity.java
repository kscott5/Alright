package karega.scott.alright;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.ActionBar;
import android.app.Activity;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

/*
 * Activity used to setup game parameters for play
 */
public class GameSetupActivity extends FragmentActivity implements
		OnClickListener, 
		OnCloseListener, 
		OnSuggestionListener,
		OnQueryTextListener,
		ManagerStateListener
{
	private final static String LOG_TAG = "GameSetup";

	private AlrightManager manager;
	private ActionBar actionBar;
	private GoogleMap map;

	private TextView myDestinationText;
	private ImageView myDestinationIcon;
	private ImageView myLocationIcon;

	private TextView gameCardButtonsLeft;
	private TextView gameCardButtonsRight;

	private String queryText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_setup);

		this.manager = AlrightManager.getInstance(this.getApplicationContext());
		
		this.queryText = "";

		// Initialize all UI controls before anything else
		this.actionBar = this.getActionBar();
		this.actionBar.hide();

		this.gameCardButtonsLeft = (TextView) this
				.findViewById(R.id.game_card_buttons_left_textbox);
		this.gameCardButtonsRight = (TextView) this
				.findViewById(R.id.game_card_buttons_right_textbox);

		this.gameCardButtonsRight.setClickable(false);
		this.gameCardButtonsLeft.setClickable(false);

		this.myDestinationText = (TextView) this
				.findViewById(R.id.game_mydestination_textbox);
		this.myDestinationText.setOnClickListener(this);

		this.myLocationIcon = (ImageView) this
				.findViewById(R.id.game_mylocation_icon);
		this.myLocationIcon.requestFocus();

		InputMethodManager ime = (InputMethodManager) this
				.getSystemService(INPUT_METHOD_SERVICE);
		ime.hideSoftInputFromInputMethod(
				this.myDestinationText.getWindowToken(),
				InputMethodManager.HIDE_IMPLICIT_ONLY);

		this.myDestinationIcon = (ImageView) this
				.findViewById(R.id.game_mydestination_icon);

		if (this.map == null) {
			this.map = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.game_card_summary_map)).getMap();

			this.map.setMyLocationEnabled(true);
		} // end if
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart");
		
		super.onStart();
		
		// Activity visible start listening
		this.manager.startSetup(this);
	}
	
	@Override
	protected void onResume() {
		//Log.d(LOG_TAG, "onResume");
		// NO NEED onStart handles this case
		
		super.onResume();
		
		this.manager.startSetup(this);
	}
	
	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "onPause");
		
		super.onPause();
		
		this.manager.stopSetup(this);
	}
	
	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop");
		
		super.onStop();
		
		// Activity hidden stop listening
		this.manager.stopSetup(this);
	}
	
	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");
		
		this.actionBar = null;
		this.map = null;

		this.myDestinationText = null;
		this.myDestinationIcon = null;
		this.myLocationIcon = null;

		this.gameCardButtonsLeft = null;
		this.gameCardButtonsRight = null;

		this.queryText = null;
		
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);

		SearchView searchView = (SearchView) menu.findItem(R.id.search)
				.getActionView();
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
		Log.d(LOG_TAG, String.format("onClicked %s", ""));

		switch (v.getId()) {
			case R.id.game_card_buttons_left_textbox:
				this.finish();
				this.manager.completeGameSetup(/* onlyRightTurns */false);
				break;
	
			case R.id.game_card_buttons_right_textbox:
				this.finish();
				this.manager.completeGameSetup(/* onlyRightTurns */true);
				break;
	
			case R.id.game_mydestination_textbox:
				this.actionBar.show();
				break;
		}
	}

	public void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, String.format("onManagerStateChanged %s", state));

		this.actionBar.hide();

		switch (state.stateType) {
			case SETUP_GAME:
				Bitmap bitmapNotSelected = BitmapFactory.decodeResource(
						this.getResources(),
						R.drawable.ic_directions_form_destination_notselected);
				this.myDestinationIcon.setImageBitmap(bitmapNotSelected);
				this.myDestinationText.setText("");
				break;
	
			case MY_LOCATION:
				Bitmap bitmapMyLocation = BitmapFactory.decodeResource(
						this.getResources(),
						R.drawable.ic_directions_form_mylocation);
				this.myLocationIcon.setImageBitmap(bitmapMyLocation);
	
				this.animateCamera((Address) state.stateData, false, null, 0);
				break;
	
			case DESTINATION_CHANGED:
				Bitmap bitmapStartPoint = BitmapFactory.decodeResource(
						this.getResources(),
						R.drawable.ic_directions_form_startpoint);
				this.myDestinationIcon.setImageBitmap(bitmapStartPoint);
				this.myDestinationText.setText(AlrightManager
						.addressToString((Address) state.stateData));
				this.animateCamera((Address) state.stateData, true,
						"Your destination", BitmapDescriptorFactory.HUE_VIOLET);
	
				this.gameCardButtonsRight.setOnClickListener(this);
				this.gameCardButtonsLeft.setOnClickListener(this);
				break;
		}
	}

	/**
	 * Move the map into focus for the address
	 * 
	 * @param address
	 * @param addressTitle
	 * @param hue
	 * @param addMarker
	 */
	private void animateCamera(Address address, boolean addMarker,
			String title, float hue) {
		Log.d(LOG_TAG, String.format("Animating camera for %s", address));

		LatLng latlng = new LatLng(address.getLatitude(),
				address.getLongitude());

		if (addMarker) {
			this.map.clear();

			MarkerOptions options = new MarkerOptions().position(latlng)
					.title(title)
					.icon(BitmapDescriptorFactory.defaultMarker(hue));
			this.map.addMarker(options);
		}

		this.map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 12.0f));
	} // end animateCamera

	@Override
	public boolean onSuggestionClick(int position) {
		this.manager.setMyDestination(this.queryText, position);
		return true;
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		this.queryText = newText;
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		this.manager.setMyDestination(query);
		return true;
	}

	@Override
	public boolean onClose() {
		this.actionBar.hide();
		return true;
	}
} // end GameActivity
