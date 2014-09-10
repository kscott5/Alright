package karega.scott.alright.models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.SearchManager;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import android.util.Log;

/**
 * Manages the interaction with various Android and Google Services
 * 
 * @author kscott
 * 
 */
public class AlrightManager implements 
	ConnectionCallbacks,
	OnConnectionFailedListener, 
	LocationListener 
{
	private final static String LOG_TAG = "Manager";
	private final static String LOG_FILE_NAME = "manager.log";
		
	public final static String ACTION_LOCATION_SUGGESTION = "karega.scott.alright.action.LOCATION_SUGGESTION";
	public final static String SUGGESTION_QUERY_POSITION = "suggestion_query_position";

	public final static String RIGHT_TURNS_ONLY = "right_turns_only";

	public final static int MAX_RESULTS = 10;
	public final static int MIN_RESULTS = 1;

	public final static double ANGLE_0 = 0;
	public final static double ANGLE_90 = 90;
	public final static double ANGLE_180 = 180;
	public final static double ANGLE_270 = 270;
	public final static double ANGLE_360 = 360;
	
	public final static String COMPASS_HEADING_NORTH = "N";
	public final static String COMPASS_HEADING_NORTH_EAST = "NE";
	public final static String COMPASS_HEADING_EAST = "E";
	public final static String COMPASS_HEADING_SOUTH_EAST = "SE";
	public final static String COMPASS_HEADING_SOUTH = "S";
	public final static String COMPASS_HEADING_SOUTH_WEST = "SW";
	public final static String COMPASS_HEADING_WEST = "W";
	public final static String COMPASS_HEADING_NORTH_WEST = "NW";
	
	private final static long LOCATION_REQUEST_INTERVAL = 0; // seconds

	// TODO: is this the correct for Observable Pattern in Java
	private static ArrayList<ManagerStateListener> stateListeners;
	
	private ArrayList<String> compassHeadings;
	
	// Developer.Android.
	private GoogleApiClient googleApiClient;

	private boolean onlyRightTurns;
	private String currentDirection;		
	
	private Address myDestinationAddress;
	private Context context;

	private static AlrightManager manager;

	/*
	 * Adds the listener to the manager
	 * 
	 * @param listener use to observe the manager 
	 */
	public static boolean addManagerStateListener(ManagerStateListener listener) {
		if(AlrightManager.stateListeners == null)
			stateListeners = new ArrayList<ManagerStateListener>();
		
		boolean added = false;
		if (!AlrightManager.stateListeners.contains(listener)) {
			AlrightManager.stateListeners.add(listener);
			added = true;
		}
		
		return added;
	} // end addManagerStateListener

	/*
	 * Removes the listener from the manager
	 * @param listener remove from observing the manager
	 */
	public static boolean removeManagerStateListener(ManagerStateListener listener) {
		if(AlrightManager.stateListeners == null)
			AlrightManager.stateListeners = new ArrayList<ManagerStateListener>();
		
		boolean removed = false;
		if(AlrightManager.stateListeners.contains(listener)) {
			AlrightManager.stateListeners.remove(listener);
			removed = true;
		}
		
		return removed;
	} // end removeManagerStateListener
	
	/*
	 * Converts the direction to compass heading<br/>
	 * 
	 */
	public static String computeCompassDirection(double bearing) {
		if(bearing > AlrightManager.ANGLE_0 && bearing < AlrightManager.ANGLE_90) return "NE";  	
		if(bearing == AlrightManager.ANGLE_90) return "E"; 
		if(bearing > AlrightManager.ANGLE_90 && bearing < AlrightManager.ANGLE_180) return "SE"; 
		if(bearing == AlrightManager.ANGLE_180) return "S";
		if(bearing > AlrightManager.ANGLE_180 && bearing < AlrightManager.ANGLE_270) return "SW"; 
		if(bearing == AlrightManager.ANGLE_270) return "W"; 
		if(bearing > AlrightManager.ANGLE_270 && bearing < AlrightManager.ANGLE_360) return "NW"; 
				
		//if(bearing == AlrightManager.ANGLE_0) 		
			return "N";
	} // end headingToCompassValue

	/**
	 * Singleton for AlrightManager
	 * 
	 * @param context
	 * @return
	 */
	public static AlrightManager getInstance(Context context) {
		Log.d(LOG_TAG, "Get single instance...");

		// TODO: Lock to prevent others from access
		if (AlrightManager.manager == null) {
			AlrightManager.manager = new AlrightManager(context).connect();
		}

		return AlrightManager.manager;
	} // end getInstance

	/*
	 * Resets the current instance of the manager 
	 */
	public static void resetInstance(ManagerStateListener listener) {
		if(AlrightManager.manager == null)
			return;
		
		if (AlrightManager.manager.googleApiClient != null) {
			AlrightManager.manager.googleApiClient.disconnect();
		}

		AlrightManager.manager.handleManagerStateChange(new ManagerState(AlrightStateType.DISCONNECTED,
				"Manager disconnected"));

		if(listener != null)
			AlrightManager.removeManagerStateListener(listener);
		
		// Clean up
		AlrightManager.manager.context = null;
		AlrightManager.manager.googleApiClient = null;
		AlrightManager.manager.currentDirection = null;
		AlrightManager.manager.myDestinationAddress = null;
		
		AlrightManager.manager.compassHeadings.clear();
		AlrightManager.manager.compassHeadings = null;

		AlrightManager.stateListeners.clear();
		AlrightManager.stateListeners = null;
		
		AlrightManager.manager = null;
	}

	/*
	 * Handle the AlrightApplication error	
	 */
	public static void handleApplicationError(Context context, Throwable ex) {
		Log.d(LOG_TAG, "Handling application error");
		
		if(ex == null)
			return;

		// TODO: Display user friendly dialog
		// TODO: Send notification to developer
			
		try {			
			FileOutputStream fileStream = context.openFileOutput(AlrightManager.LOG_FILE_NAME, Context.MODE_PRIVATE);
			PrintStream printStream = new PrintStream(fileStream);
			ex.printStackTrace(printStream);
			fileStream.close();
		} catch(Exception e) {
			// DO NOTHING
		}

	} // end handleApplicationError
	
	/**
	 * Converts the the address to a string
	 * 
	 * @param address
	 *            object
	 * @return
	 */
	public static String addressToString(Address address) {
		Log.d(LOG_TAG, "Converting address to string");

		if (address == null)
			return null;

		StringBuffer data = new StringBuffer();
		for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
			data.append(String.format("%s ", address.getAddressLine(i)));
		}
		return data.toString();
	} // end addressToString

	/**
	 * Details related to the current state of AlrightManager
	 */
	public final static class ManagerState {
		public final AlrightStateType stateType;
		public final Object stateData;

		public ManagerState(AlrightStateType stateType, Object stateData) {
			this.stateType = stateType;
			this.stateData = stateData;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("ManagerState[stateType: %s, ", this.stateType.name()));
			
			if(this.stateData instanceof Location)
				builder.append(String.format("stateData: %s] ", ((Location)this.stateData)).toString());
			
			if(this.stateData instanceof String)
				builder.append(String.format("stateData: %s] ", ((String)this.stateData)).toString());
			
			return builder.toString();
		}
	}

	/**
	 * Listener for the Alight Manager
	 */
	public interface ManagerStateListener {
		void onManagerStateChanged(ManagerState state);
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 *            {@link Context} object
	 */
	private AlrightManager(Context context) {
		Log.d(LOG_TAG, "Constructor...");
		this.context = context.getApplicationContext();

		this.compassHeadings = new ArrayList<String>();
		this.onlyRightTurns = false;
		
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_NORTH);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_NORTH_EAST);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_EAST);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_SOUTH_EAST);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_SOUTH);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_SOUTH_WEST);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_WEST);
		this.compassHeadings.add(AlrightManager.COMPASS_HEADING_NORTH_WEST);	
	} // end constructor

	/**
	 * Connects the manager to specific system services such as <br/>
	 * 
	 * Location <br/>
	 * Sensor <br/>
	 * 
	 * @return AlrightManager
	 */
	private AlrightManager connect() {
		// NOTE: Documentation status you should check but why,
		// this service is core to standard Android devices. If
		// the device is possibly rooted and customized outside 
		// normal manufacture load, then maybe we could have 
		// a problem.
		
		// int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.context);
		//if(ConnectionResult.SUCCESS != status) {
		//	...			
		//}
		
		if(AlrightManager.stateListeners == null)
			AlrightManager.stateListeners = new ArrayList<ManagerStateListener>();
				
		this.googleApiClient = new GoogleApiClient.Builder(this.context)
			.addApi(LocationServices.API)
			.addOnConnectionFailedListener(this)
			.addConnectionCallbacks(this)
			.build();
	
		this.googleApiClient.connect();
		this.handleManagerStateChange(new ManagerState(AlrightStateType.CONNECTING, "Connecting"));
		
		return this;
	} // end connect

	/**
	 * Disconnects the manager for specific system services such as<br/>
	 * 
	 * Location <br/>
	 * Sensor <br/>
	 * 
	 */
	private void disconnect(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Disconnecting the manager");

		AlrightManager.resetInstance(listener);
	} // end disconnect

	/**
	 * Notify the observes to state changes
	 * 
	 * @param state
	 */
	private void handleManagerStateChange(ManagerState state) {
		//Log.d(LOG_TAG, "Handling manager state change");		
		for (ManagerStateListener listener : stateListeners) {
			listener.onManagerStateChanged(state);
		}
	}

	/*
	 * Start main listening to manager
	 * @param listener ManagerStateListener
	 */
	public void startMain(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Start Main");

		AlrightManager.addManagerStateListener(listener);
		
		if (this.googleApiClient != null && this.googleApiClient.isConnected()) {
			this.handleManagerStateChange(new ManagerState(AlrightStateType.CONNECTED,
					"Manager Google API Client connected"));
		}

		this.stopLocationUpdateRequests();
	} // end startMain
	
	/*
	 * Stop main from listening to manager
	 * @param listener 
	 * @param isFinishing see document for Activity.isFinishing()
	 */
	public void stopMain(ManagerStateListener listener, boolean isFinishing) {
		Log.d(LOG_TAG, "Stop Main");

		if(isFinishing) {
			this.disconnect(listener);
		}
	} // end stopMain

	/*
	 * Start help listening to manager
	 * @param listener ManagerStateListener
	 */
	public void startHelp(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Start Help");

		AlrightManager.addManagerStateListener(listener);	
	} // end startHelp	
	
	/*
	 * Stop help from listening to manager
	 * @param listener ManagerStateListener
	 */
	public void stopHelp(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Stop Help");

		AlrightManager.removeManagerStateListener(listener);	
	} // end stopHelp
	
	/**
	 * Start setup listening to manager
	 * @param listener used to observe changes on the manager
	 */
	public void startSetup(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Start Setup");

		AlrightManager.addManagerStateListener(listener);
		
		this.myDestinationAddress = null;
		this.onlyRightTurns = true;

		this.setMyLastKnownLocation();
		this.stopLocationUpdateRequests();
		
		// Notify client
		this.handleManagerStateChange(new ManagerState(AlrightStateType.SETUP_GAME,
				"Game setup ready"));
	} // end newGame

	/*
	 * Stop setup from listening to manager
	 */
	public void stopSetup(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Stop Setup");

		AlrightManager.removeManagerStateListener(listener);	
	} // end stopSetup

	/**
	 * Start tracker listening to manager
	 * @param listener used to observe changes on the manager
	 */
	public void startTracker(ManagerStateListener listener) {
		Log.d(LOG_TAG,	"Start Tracker");

		AlrightManager.addManagerStateListener(listener);
		
		if (this.myDestinationAddress == null) {
			this.handleManagerStateChange(new ManagerState(AlrightStateType.ERROR,
					"Choose a destination..."));
			return;
		}

		this.setMyLastKnownLocation();
		this.startLocationUpdateRequests();
		
		this.handleManagerStateChange(new ManagerState(AlrightStateType.GAME_STARTED, "Game started"));
	} // end startGame

	/*
	 * Stop tracker from listening to manager
	 */
	public void stopTracker(ManagerStateListener listener) {
		Log.d(LOG_TAG, "Stop Tracker");

		AlrightManager.removeManagerStateListener(listener);	
	} // end stopTracker

	/**
	 * Activate location update requests
	 */
	private void startLocationUpdateRequests() {
		Log.d(LOG_TAG, "Start listening for location updates");
		
		if(this.googleApiClient.isConnected()) {		
			LocationRequest request = LocationRequest.create();
			request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			request.setInterval(AlrightManager.LOCATION_REQUEST_INTERVAL);
			
			LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, request, this);
		}
	} // end startLocationUpdateRequests
	
	/**
	 * Deactivate location update requests
	 */
	private void stopLocationUpdateRequests() {
		Log.d(LOG_TAG, "Stop listening for location updates");

		if(this.googleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
		}
	} // end stopLocationUpdateRequests
	
	/**
	 * Sets the turn direction for game play.
	 * 
	 * @param direction
	 */
	public void completeGameSetup(boolean onlyRightTurns) {
		Log.d(LOG_TAG, "Setting the turn direction...");

		// Reserve the order and ensure NORTH is always first
		if(!(this.onlyRightTurns=onlyRightTurns)) {
			this.compassHeadings.remove(0);
			this.compassHeadings.trimToSize();
			Collections.reverse(this.compassHeadings);
			this.compassHeadings.add("N");
		}
		
		// Return copy of the compass heading
		this.handleManagerStateChange(new ManagerState(AlrightStateType.GAME_SETUP_COMPLETE,
				this.compassHeadings.clone()));
		
	} // end setTurnDirection

	/**
	 * @return the last known location
	 */
	protected void setMyLastKnownLocation() {
		Log.d(LOG_TAG, "Get last known location");
				
		Location location = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);
		
		if(location != null) {
			this.currentDirection = AlrightManager.computeCompassDirection(location.getBearing());
			
			this.handleManagerStateChange(new ManagerState(
					AlrightStateType.MY_LOCATION, 
					this.getAddress(location.getLatitude(),location.getLongitude())));
		} else {
			// TODO: Should we send this message
			this.handleManagerStateChange(new ManagerState(
					AlrightStateType.ERROR, "My location not set"));
		}
	} // end setMyLastKnownLocation

	/**
	 * Sets the destination for game play
	 * 
	 * @param myDestination
	 */
	public void setMyDestination(String myDestination) {
		this.setMyDestination(myDestination, 0);		
	} // end setMyDestination

	/**
	 * Sets the destination for game play
	 * 
	 * @param myDestination
	 * @param position
	 */
	public void setMyDestination(String myDestination, int position) {
		Log.d(LOG_TAG,	String.format("Setting my destination to %s at position %s", myDestination, position));

		this.myDestinationAddress = this.getAddress(myDestination, position);

		if(this.myDestinationAddress != null) {
			this.handleManagerStateChange(new ManagerState(
					AlrightStateType.DESTINATION_CHANGED, 
					this.myDestinationAddress));
		} else {
			this.handleManagerStateChange(new ManagerState(
					AlrightStateType.ERROR, 
					this.myDestinationAddress));
		}
	}

	/**
	 * Retrieves a {@link SearchManager} from System Services
	 * 
	 * @return {@link SearchManager}
	 */
	public SearchManager getSearchManager() {
		Log.d(LOG_TAG, "Retrieving search manager");

		return (SearchManager) this.context
				.getSystemService(Context.SEARCH_SERVICE);
	} // end getSearchManager

	/**
	 * Uses {@link Geocoder} to find the address
	 * 
	 * @param locationName
	 *            is the query text
	 * @return {@link Address} for the location name if found else null
	 */
	public Address getAddress(String locationName) {
		Log.d(LOG_TAG, String.format("Get address for %s", locationName));

		Geocoder geocoder = null;
		Address addr = null;

		try {
			geocoder = new Geocoder(this.context);

			List<Address> addrs = geocoder.getFromLocationName(locationName,
					AlrightManager.MIN_RESULTS);
			if (addrs.size() > 0) {
				addr = addrs.get(0);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}

		return addr;
	} // end getAddress

	/**
	 * Uses the {@link Geocoder} to find the address
	 * 
	 * @param latitude
	 *            postion
	 * @param longitude
	 *            position
	 * @return
	 */
	public Address getAddress(double latitude, double longitude) {
		Log.d(LOG_TAG, String.format(
				"Get address for latitude=%s and longitude=%s", latitude,
				longitude));

		Geocoder geocoder = null;
		Address addr = null;

		try {
			geocoder = new Geocoder(this.context);

			List<Address> addrs = geocoder.getFromLocation(latitude, longitude,
					AlrightManager.MIN_RESULTS);
			if (addrs.size() > 0) {
				addr = addrs.get(0);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}

		return addr;
	} // end getAddress

	/**
	 * Uses the {@link Geocoder} to find the address
	 * 
	 * @param locationName
	 * @param position
	 * @return
	 */
	public Address getAddress(String locationName, int position) {
		Log.d(LOG_TAG, String.format("Get address for %s at position %s",
				locationName, position));

		Address addr = null;
		if (position < 0)
			return addr;

		List<Address> addrs = this.getAddresses(locationName);
		if (addrs.size() > 0) {
			addr = addrs.get(position);
		}

		return addr;
	} // end getAddress

	/**
	 * Uses the {@link Geocoder} to find suggested addresses
	 * 
	 * @param locationName
	 *            is the suggestion query text
	 * @return
	 */
	public List<Address> getAddresses(String locationName) {
		Log.d(LOG_TAG,
				String.format("Get suggested addresses for %s", locationName));

		Geocoder geocoder = null;
		List<Address> addrs = null;

		if (locationName == null)
			return addrs;

		try {
			geocoder = new Geocoder(this.context);

			addrs = geocoder.getFromLocationName(locationName,
					AlrightManager.MAX_RESULTS);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		}

		return addrs;
	} // end getAddresses

	@Override
	public void onLocationChanged(Location location) {
		Log.d(LOG_TAG, String.format("Location changed to %s", location));

		// NOTE: Should we handle this using LocationManager.addProximityAlert()		
/*		if(location.getLatitude() == this.myDestination.getLatitude() && 
			location.getLongitude() == this.myDestination.getLongitude()) {
			this.handleManagerStateChange(new ManagerState(AlrightStateType.GAME_OVER_WINNER, "YOU WIN!"));
		}

		String actualDirection = AlrightManager.computeCompassDirection(location.getBearing());
		if(this.currentDirection ==  null)
			this.currentDirection = actualDirection;
		
		// Are we still on track
*/		AlrightStateType stateType = AlrightStateType.STILL_ON_TRACK;
/*		if(!this.currentDirection.equalsIgnoreCase(actualDirection)) {
			
			// Get index of next possible direction
			int index = this.compassHeadings.indexOf(this.currentDirection)+1;
			index = (index < this.compassHeadings.size())? index: 0;

			// Did you make a wrong turn?
			String expectedDirection = this.compassHeadings.get(index); 
			if(!expectedDirection.equalsIgnoreCase(actualDirection)){
				stateType = AlrightStateType.GAME_OVER_LOSER;					
			}
		} // end if
		
*/		this.handleManagerStateChange(new ManagerState(stateType, location));
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.d(LOG_TAG, "Google API Client Connection Failed");
		
		this.handleManagerStateChange(new ManagerState(AlrightStateType.ERROR,
				"Manager failed connecting with Google Api Client"));
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.d(LOG_TAG, "Google API Client Connected");
		
		this.handleManagerStateChange(new ManagerState(AlrightStateType.CONNECTED,
				"Manager connected successfully with Google Api Client"));		
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
	}
} // end AlrightManager
