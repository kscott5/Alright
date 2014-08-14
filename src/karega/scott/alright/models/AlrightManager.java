package karega.scott.alright.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.SearchManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.util.Log;
/**
 * Manages the interaction with various Android and Google Services
 * @author kscott
 *
 */
public class AlrightManager implements LocationListener {
	private final static String LOG_TAG = "Alright Manager";

	public final static String RIGHT_TURNS_ONLY = "right_turns_only";
	
	public final static int MAX_RESULTS = 10;
	public final static int MIN_RESULTS = 1;

	public final static int STATE_TYPE_ERROR = -1;
	public final static int STATE_TYPE_SUCCESS = 0;
	public final static int STATE_TYPE_NEW_GAME = 1;
	public final static int STATE_TYPE_NO_CHANGE = 2;
	public final static int STATE_TYPE_MY_LOCATION = 3;
	public final static int STATE_TYPE_DESTINATION_CHANGED = 4;
	public final static int STATE_TYPE_GAME_STARTED = 5;
	public final static int STATE_TYPE_STILL_ON_TRACK = 6;
	public final static int STATE_TYPE_GAME_OVER_LOSER = 7;
	public final static int STATE_TYPE_GAME_OVER_WINNER = 8;
	
	public final static int TURN_DIRECTION_LEFT = 0;
	public final static int TURN_DIRECTION_RIGHT = 1;
	
	private long LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES = 0;
	private float LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS = 0.914f; // 1 yard;
	
	
	private static AlrightManager manager;
	private int turnDirection = TURN_DIRECTION_RIGHT;
	private boolean isConnected = false;
	
	private Context context;
	private SearchManager searchManager;

	private LocationManager locationManager;
	private String locationProvider;
	
	private Address myCurrentAddress;
	private Address myDestination;
	
	// TODO: is this the correct for Observable Pattern in Java
	private static ArrayList<ManagerStateListener> stateListener = 
			new ArrayList<ManagerStateListener>();

	public class TrackingDetails {
		public String provider;
		public float distance;
		public float bearing;
		public int direction;
	}
	
	/**
	 * Current state of the Alright Manager
	 */
	public final class ManagerState {
		public final int stateType;
		public final Object stateData;
		
		public ManagerState(int stateType, Object stateData) {
			this.stateType = stateType;
			this.stateData = stateData;
		}
	}
	
	/**
	 * Listener for the Alight Manager
	 */
	public interface ManagerStateListener {
		void onManagerStateChanged(ManagerState state);
	}
	
	/**
	 *  Constructor
	 * @param context {@link Context} object
	 */
	private AlrightManager(Context context) {
		Log.d(LOG_TAG, "Constructor...");
		this.context = context;		
	} // end constructor
	
	/**
	 * Singleton for Alright Manager
	 * @param context
	 * @return
	 */
	public static AlrightManager getInstance(Context context) {
		Log.d(LOG_TAG, "Get single instance...");
		
		// TODO: Lock to prevent others from access 
		if(manager == null) {
			manager = new AlrightManager(context);
		}
		
		return manager;
	} // end getInstance
	
	/**
	 * Converts the the address to a string
	 * @param address object
	 * @return
	 */
	public static String addressToString(Address address) {
		Log.d(LOG_TAG, "Converting address to string");
		
		if(address == null) return null;
				
		StringBuffer data = new StringBuffer();
		for(int i=0; i<address.getMaxAddressLineIndex(); i++) {
			data.append(String.format("%s ", address.getAddressLine(i)));
		}
		return data.toString();
	} // end addressToString

	/*
	 * Add listener to manager
	 * @param listener 
	 */
	public void setManagerStateLister(ManagerStateListener listener){ 
		if(!stateListener.contains(listener)) {
			stateListener.add(listener);
		}		
	}
	
	/**
	 * Connects to the manager by attaching the listener and set my location
	 * @param listener
	 * @return AlrightManager
	 */
	public AlrightManager connect() {
		Log.d(LOG_TAG, "Connecting to manager");
		
		if(this.isConnected)
			return this;
		
		this.searchManager = (SearchManager) this.context.getSystemService(Context.SEARCH_SERVICE);
		if(this.searchManager == null) {
			this.handleManagerStateChange(
					new ManagerState(STATE_TYPE_ERROR, "Manager could not initial the Android Search Service"));
			return null;
		}
		
		this.locationManager = (LocationManager)this.context.getSystemService(Context.LOCATION_SERVICE);
		if(this.locationManager == null) {
			this.handleManagerStateChange(
					new ManagerState(STATE_TYPE_ERROR, "Manager could not initial the Android Location Service"));
			return null;
		}
		
		this.setBestProviderForLocationUpdates();
		this.setMyLocation(false);
	
		this.handleManagerStateChange(
				new ManagerState(STATE_TYPE_SUCCESS, "Manager connected successfully"));
		
		return this;
	} // end connect

	private void setBestProviderForLocationUpdates() {
		Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_COARSE);	    
	    criteria.setAltitudeRequired( false );
	    criteria.setBearingRequired( true );
	    criteria.setCostAllowed( true );
	    criteria.setPowerRequirement(Criteria.POWER_LOW);
		  
		this.locationProvider = this.locationManager.getBestProvider(criteria, true);
	}
	
	/**
	 * Disconnects the manager
	 */
	public AlrightManager disconnect() {
		Log.d(LOG_TAG, "Disconnect to manager");
		
		this.isConnected = false;
		this.searchManager = null;
		
		if(this.locationManager != null) {
			// Won't request updates from location manager until this.startGame() is called
			// Stop listening for updates via GPS, Network, etc...
			this.locationManager.removeUpdates(this);
		}
		this.locationManager = null;
		
		this.handleManagerStateChange(
				new ManagerState(STATE_TYPE_SUCCESS, "Manager disconnected"));
		
		return this;
	} // end disconnect
	
	/**
	 * Notify the observes to state changes
	 * @param state
	 */
	private void handleManagerStateChange(ManagerState state) {
		Log.d(LOG_TAG, "Handling manager state change");
		for(ManagerStateListener listener : stateListener) {
			listener.onManagerStateChanged(state);
		}
	} 

	public Address getMyCurrentLocation() {
		return this.myCurrentAddress;
	}
	
	/**
	 * Starts the game by listening to location changes
	 */
	public void startGame() {
		Log.d(LOG_TAG, "Game starting to request location updates for monitoring...");
		
		if(!this.locationManager.isProviderEnabled(this.locationProvider)) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR, String.format("%s not available", this.locationProvider)));
			return;
		} 
		
		if(this.myCurrentAddress == null) {
			setMyLocation(true);
		} 
		
		if(this.myDestination == null)  {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR, "Choose a destination..."));
			return;
		} 		

		this.locationManager.requestLocationUpdates(this.locationProvider, this.LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES, this.LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS, this);
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_GAME_STARTED, "Game started"));
	} // end startGame

	/**
	 * Sets the turn direction for game play. Default is TURN_DIRECTION_RIGHT. 
	 * @param direction
	 */
	public void setTurnDirection(int direction) {
		Log.d(LOG_TAG, "Setting the turn direction...");
		
		switch(direction) {
			case TURN_DIRECTION_LEFT:
			case TURN_DIRECTION_RIGHT:		
				this.turnDirection = direction;
				this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS, 
						String.format("Turn direction set to %s", direction)));
				break;
				
			default:
				this.turnDirection = TURN_DIRECTION_RIGHT;
				this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS, 
						String.format("%s is invalid. Turn direction set to right, %s)", 
								direction, TURN_DIRECTION_RIGHT)));
				break;
		}
	} // end setTurnDirection
	
	/**
	 * Set my location
	 * @param showError
	 */
	public void setMyLocation(boolean showError) {
		Log.d(LOG_TAG, String.format("Setting my location with showError(%s)", showError));
		
		Location location = this.locationManager.getLastKnownLocation(this.locationProvider);
		if(location == null)
			return;
		
		this.myCurrentAddress = this.getAddress(location.getLatitude(), location.getLongitude());
		
		if(this.myCurrentAddress == null) {
			if(showError) {
				this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR, "Choose a destination..."));
			}
			return;
		}
		
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_MY_LOCATION, this.myCurrentAddress));
	}
	
	/**
	 * Starts a new game
	 */
	public void newGame() {
		Log.d(LOG_TAG, "Starting new game");
		
		this.myCurrentAddress = null;
		this.myDestination = null;
		this.turnDirection = TURN_DIRECTION_RIGHT;
		
		// Won't request updates from location manager until this.startGame() is called
		// Stop listening for updates via GPS, Network, etc...
		this.locationManager.removeUpdates(this);
		
		// Notify client
		this.handleManagerStateChange(
				new ManagerState(STATE_TYPE_NEW_GAME, "New game ready for play"));
	} // end newGame
	
	/**
	 * Sets the destination for game play
	 * @param myDestination is a string represent by the search query
	 */
	public void setMyDestination(String myDestination) {
		Log.d(LOG_TAG, String.format("Setting my destination to %s", myDestination));
		this.myDestination = this.getAddress(myDestination);
		
		if(this.myDestination != null) {
			setMyLocation(false /* Show Error. This will be handle when calling startGame.*/);
			
			handleManagerStateChange(new ManagerState(
					AlrightManager.STATE_TYPE_DESTINATION_CHANGED, this.myDestination));
		}
	} // end setEndPoint
		
	/**
	 * Retrieves a {@link SearchManager} from System Services
	 * @return {@link SearchManager}
	 */
	public SearchManager getSearchManager() {
		Log.d(LOG_TAG, "Retrieving search manager");
		return this.searchManager;
	} // end getSearchManager
	
	/**
	 * Uses {@link Geocoder} to find the address
	 * @param locationName is the query text
	 * @return {@link Address} for the location name if found else null
	 */
	public Address getAddress(String locationName) {
		Log.d(LOG_TAG, String.format("Get address for %s", locationName));
		
		Geocoder geocoder = null;
		Address addr = null;;
		
		if(locationName == null) return addr;
		
		try {
			geocoder = new Geocoder(this.context); 
		
			List<Address> addrs = geocoder.getFromLocationName(locationName, AlrightManager.MIN_RESULTS);
			if(addrs.size() > 0) {
				addr = addrs.get(0);
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());			
		} 
		
		return addr;
	} // end getAddress
	
	/**
	 * Uses the {@link Geocoder} to find the address
	 * @param latitude postion
	 * @param longitude position
	 * @return
	 */
	public Address getAddress(double latitude, double longitude) {
		Log.d(LOG_TAG, String.format("Get address for latitude=%s and longitude=%s", latitude, longitude));
		
		Geocoder geocoder = null;
		Address addr = null;

		if(latitude < -90d || latitude > 90d) return addr;
		if(longitude < -180d || longitude > 180d) return addr;
		
		try {
			geocoder = new Geocoder(this.context); 
		
			List<Address> addrs = geocoder.getFromLocation(latitude, longitude, AlrightManager.MIN_RESULTS);
			if(addrs.size() > 0) {
				addr = addrs.get(0);
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());			
		} 
		
		return addr;
	} // end getAddress

	/**
	 * Uses the {@link Geocoder} to find suggested addresses
	 * @param locationName is the suggestion query text
	 * @return
	 */
	public List<Address> getAddresses(String locationName) {
		Log.d(LOG_TAG, String.format("Get suggested addresses for %s", locationName));
		
		Geocoder geocoder = null;
		List<Address> addrs = null;;

		if(locationName == null) return addrs;

		try {
			geocoder = new Geocoder(this.context); 
		
			addrs = geocoder.getFromLocationName(locationName, AlrightManager.MAX_RESULTS);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());			
		} 
		
		return addrs;		
	} // end getAddresses

	@Override
	public void onLocationChanged(Location location) {
		Log.d(LOG_TAG, String.format("Location changed to %s", location));

		// TODO: Use the turnDirection value to determine game state.
		//
		// STATE_TYPE_STILL_ON_TRACK only when direction changes 
		// STATE_TYPE_GAME_OVER_LOSER only when direction is wrong or time limit up
		// STATE_TYPE_GAME_OVER_WINNER only when they reach destination
		
		// Save the previous location
		double startLatitude = this.myCurrentAddress.getLatitude();		
		double startLongitude = this.myCurrentAddress.getLongitude();
		
		// Save the current location 
		double endLatitude = location.getLatitude();		
		double endLongitude = location.getLongitude();

		// Update my address with new location
		this.myCurrentAddress = this.getAddress(
				location.getLatitude(), location.getLongitude());


		float[] results = new float[2];
		try {
			// Get bearing information
			Location.distanceBetween(startLatitude, startLongitude, 
					endLatitude, endLongitude, results);

			TrackingDetails details = new TrackingDetails();
			details.distance =  results[0];
			
			// The computed distance is stored in results[0]. 
			// If results has length 2 or greater, the initial 
			// bearing is stored in results[1]. If results has 
			// length 3 or greater, the final bearing is stored 
			// in results[2].
			if(results.length == 2) {
				details.bearing = results[1];
			}
			
			if(results.length >= 3) {
				details.bearing = results[2];
			}
			
			details.provider = this.locationProvider;
			
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_STILL_ON_TRACK, details));
		} catch(IllegalArgumentException e) {
			// TODO: WHAT SHOULD HAPPEN HERE?
			Log.e(LOG_TAG, e.toString());
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(LOG_TAG, String.format("Location provider (%s) status changed with code [%s]",provider,status));
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(LOG_TAG, String.format("Provider (%s) enabled...", provider));
		
		if(LocationManager.GPS_PROVIDER.equalsIgnoreCase(provider)) {
			this.locationManager.removeUpdates(this);
			this.setBestProviderForLocationUpdates();
			this.locationManager.requestLocationUpdates(this.locationProvider, this.LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES, this.LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS, this);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(LOG_TAG, String.format("Provider (%s) disabled...", provider));
		
		if(LocationManager.GPS_PROVIDER.equalsIgnoreCase(provider)) {
			this.locationManager.removeUpdates(this);
			this.setBestProviderForLocationUpdates();			
			this.locationManager.requestLocationUpdates(this.locationProvider, this.LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES, this.LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS, this);
		}
	}
} // end AlrightManager
