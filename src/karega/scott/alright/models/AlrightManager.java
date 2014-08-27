package karega.scott.alright.models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import android.util.Log;

/**
 * Manages the interaction with various Android and Google Services
 * 
 * @author kscott
 * 
 */
public class AlrightManager implements 
	LocationListener, 
	SensorEventListener,
	DialogInterface.OnClickListener
{
	private final static String LOG_TAG = "AlrightManager";
	private final static String LOG_FILE_NAME = "alright.log";
		
	public final static String ACTION_LOCATION_SUGGESTION = "karega.scott.alright.action.LOCATION_SUGGESTION";
	public final static String SUGGESTION_QUERY_POSITION = "suggestion_query_position";

	public final static String RIGHT_TURNS_ONLY = "right_turns_only";

	public final static int MAX_RESULTS = 10;
	public final static int MIN_RESULTS = 1;

	public final static int STATE_TYPE_WARNING = -2;
	public final static int STATE_TYPE_ERROR = -1;
	public final static int STATE_TYPE_SUCCESS = 0;
	public final static int STATE_TYPE_NEW_GAME = 1;
	public final static int STATE_TYPE_NO_CHANGE = 2;
	public final static int STATE_TYPE_MY_LOCATION = 3;
	public final static int STATE_TYPE_DESTINATION_CHANGED = 4;
	public final static int STATE_TYPE_GAME_STARTED = 5;
	public final static int STATE_TYPE_GAME_SETUP_COMPLETE = 6;
	public final static int STATE_TYPE_STILL_ON_TRACK = 7;
	public final static int STATE_TYPE_GAME_OVER_LOSER = 8;
	public final static int STATE_TYPE_GAME_OVER_WINNER = 9;

	public final static int TURN_DIRECTION_LEFT = 0;
	public final static int TURN_DIRECTION_RIGHT = 1;

	private final static int ANGLE_0 = 0;
	private final static int ANGLE_90 = 90;
	private final static int ANGLE_180 = 180;
	private final static int ANGLE_270 = 270;
	private final static int ANGLE_360 = 360;
	
	private long LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES = 0;
	private float LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS = 0.914f; // 1
																		// yard;
	private boolean onlyRightTurns = false;
	private boolean isConnected = false;
	
	private LocationManager locationManager;
	private String locationProvider;
	
	private SensorManager sensorManager;
	private Sensor sensorOrientation; // Yes, I know its depreciated but there's a reason for most things
	
	private ArrayList<String> compassHeadings = new ArrayList<String>();
	private String currentDirection = null;
	
	private Context context;
	private Address myDestination;

	// TODO: is this the correct for Observable Pattern in Java
	private static ArrayList<ManagerStateListener> stateListener = new ArrayList<ManagerStateListener>();
	private static AlrightManager manager;
	
	public static class TrackingDetails implements Parcelable {
		public final String Provider;
		
		/*
		 *  Sensor.TYPE_ORIENTATION all values are angles in degrees.
		 *  Does this mean we need to Math.toDegrees these values?
		 */
		public final float Pitch; // x
		public final float Roll; // y
		public final float Azimuth; // z
		
		public final String Direction;
		public final String Direction_Previous;
		
		private TrackingDetails(Parcel in) {
			this.Provider = in.readString();
			this.Pitch = in.readFloat();
			this.Roll = in.readFloat();
			this.Azimuth = in.readFloat();
			this.Direction = in.readString();
			this.Direction_Previous = in.readString();
		}
				
		public TrackingDetails(String providerName, float zAzimuth,	float xPitch, float yRoll, String previousDirection) {
			this.Provider = providerName;
			
			this.Pitch =  xPitch;
			this.Roll = yRoll;
			this.Azimuth = zAzimuth;
			
			this.Direction = AlrightManager.computeCompassDirection(this.Azimuth);
			this.Direction_Previous = previousDirection;
		}	

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("Provider: %s, ", this.Provider));
			builder.append(String.format("Pitch (X): %s, ", this.Pitch));
			builder.append(String.format("Roll (Y): %s, ", this.Roll));
			builder.append(String.format("Azimuth (Z): %s, ", this.Azimuth));
			builder.append(String.format("Direction: %s, ", this.Direction));
			builder.append(String.format("Direction Previous: %s", this.Direction_Previous));
			
			return builder.toString();
		}

		public final static Parcelable.Creator<TrackingDetails> CREATOR = new Parcelable.Creator<TrackingDetails>() {
			@Override
			public TrackingDetails createFromParcel(Parcel in){
				return new TrackingDetails(in);
			}
			public TrackingDetails[] newArray(int size) {
				return new TrackingDetails[size];
			}
		};
		
		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeArray(new Object[] { 
					this.Provider, this.Pitch, this.Roll, 
					this.Azimuth, this.Direction, this.Direction_Previous
			});
		}		
	} // end TrackingDetails

	/*
	 * Converts the direction to compass heading<br/>
	 * <br/>
	 * https://source.android.com/devices/sensors/composite_sensors.html#Magnetometer
	 * <br/>
	 * <bold>Orientation</bold><br/>
	 * <br/>
	 *	Underlying base sensor(s): Accelerometer, Magnetometer PREFERRED Gyroscope<br/>
	 *	Trigger-mode: Continuous<br/>
	 *	Wake-up sensor: No<br/>
	 *	<br/>
	 *	Note: This is an older sensor type that has been deprecated in the Android SDK although not yet in the HAL.<br/> 
	 *	It has been replaced by the rotation vector sensor, which is more clearly defined, requires a gyroscope, and <br/>
	 *	therefore provides more accurate results. Use the rotation vector sensor over the orientation sensor whenever <br/>
	 *	possible.<br/>
	 *	<br/>
	 *	The orientation sensor tracks the attitude of the device. All values are angles in degrees. Orientation sensors<br/> 
	 *	return sensor events for all three axes at a constant rate defined by setDelay().<br/>
	 *	<br/>
	 *	azimuth: angle between the magnetic north direction and the Y axis, around<br/> 
	 *	the Z axis (0<=azimuth<360). 0=North, 90=East, 180=South, 270=West<br/>
	 *	pitch: Rotation around X axis (-180<=pitch<=180), with positive values when the z-axis moves toward the y-axis.<br/>
 	 *  roll: Rotation around Y axis (-90<=roll<=90), with positive values when the x-axis moves towards the z-axis.<br/>
	 *	<br/>
	 *	Please note, for historical reasons the roll angle is positive in the clockwise direction. (Mathematically speaking,<br/> 
	 *  it should be positive in the counter-clockwise direction):<br/>
	 *	<br/>
	 */
	public static String computeCompassDirection(double angle) {
		if(angle == AlrightManager.ANGLE_0) return "N"; 		
		if(angle > AlrightManager.ANGLE_0 && angle < AlrightManager.ANGLE_90) return "NE";  	
		if(angle == AlrightManager.ANGLE_90) return "E"; 
		if(angle > AlrightManager.ANGLE_90 && angle < AlrightManager.ANGLE_180) return "SE"; 
		if(angle == AlrightManager.ANGLE_180) return "S";
		if(angle > AlrightManager.ANGLE_180 && angle < AlrightManager.ANGLE_270) return "SW"; 
		if(angle == AlrightManager.ANGLE_270) return "W"; 
		if(angle > AlrightManager.ANGLE_270 && angle < AlrightManager.ANGLE_360) return "NW"; 
				
		// TODO: What should do here
		return "";
	} // end headingToCompassValue

	/**
	 * Details related to the current state of AlrightManager
	 */
	public final static class ManagerState {
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
	 * Constructor
	 * 
	 * @param context
	 *            {@link Context} object
	 */
	private AlrightManager(Context context) {
		Log.d(LOG_TAG, "Constructor...");
		this.context = context;
		
		this.compassHeadings.add("N");
		this.compassHeadings.add("NE");
		this.compassHeadings.add("E");
		this.compassHeadings.add("SE");
		this.compassHeadings.add("S");
		this.compassHeadings.add("SW");
		this.compassHeadings.add("W");
		this.compassHeadings.add("NW");
		
	} // end constructor

	/**
	 * Singleton for AlrightManager
	 * 
	 * @param context
	 * @return
	 */
	public static AlrightManager getInstance(Context context) {
		Log.d(LOG_TAG, "Get single instance...");

		// TODO: Lock to prevent others from access
		if (manager == null) {
			manager = new AlrightManager(context);
		}

		return manager;
	} // end getInstance

	/*
	 * Handle the AlrightApplication error	
	 */
	public static void handleApplicationError(Context context, Throwable ex) {
		Log.d(LOG_TAG, "Handling application error");
		
		if(ex == null)
			return;
		
		//AlertDialog.Builder builder  = new AlertDialog.Builder(context);		
		if(/*// TODO: Debug == true && */ AlrightManager.getInstance(context) != null) {
			//builder.setMessage(ex.toString());
			//builder.setPositiveButton("OK", AlrightManager.manager).show();
			
			try {			
				FileOutputStream fileStream = context.openFileOutput(AlrightManager.LOG_FILE_NAME, Context.MODE_PRIVATE);
				PrintStream printStream = new PrintStream(fileStream);
				ex.printStackTrace(printStream);
				fileStream.close();
			} catch(Exception e) {
				// DO NOTHING
			}

		} else {
			// TODO: Display user friendly dialog
			// TODO: Send notification to developer
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

	/*
	 * Add listener to manager
	 * 
	 * @param listener
	 */
	public void setManagerStateListener(ManagerStateListener listener) {
		if (!stateListener.contains(listener)) {
			stateListener.add(listener);
		}
	}

	/**
	 * Connects the manager to specific system services such as <br/>
	 * 
	 * Location <br/>
	 * Sensor <br/>
	 * 
	 * @return AlrightManager
	 */
	public AlrightManager connect() {
		Log.d(LOG_TAG, "Connecting to manager");

		// TODO: Should this method be rename to onResume()
		if (this.isConnected)
			return this;

		// NOTE: <uses-feature> should prevent null errors
		this.sensorManager = (SensorManager) this.context
				.getSystemService(Context.SENSOR_SERVICE);
		if (this.sensorManager == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Manager could not initial the Android Sensor Service"));
			return null;
		}

		this.locationManager = (LocationManager) this.context
				.getSystemService(Context.LOCATION_SERVICE);
		if (this.locationManager == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Manager could not initial the Android Location Service"));
			return null;
		}

		this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS,
				"Manager connected successfully"));

		return this;
	} // end connect

	/**
	 * Disconnects the manager for specific system services such as<br/>
	 * 
	 * Location <br/>
	 * Sensor <br/>
	 * 
	 */
	public AlrightManager disconnect() {
		Log.d(LOG_TAG, "Disconnecting the manager");

		this.isConnected = false;

		if (this.locationManager != null) {
			this.stopLocationUpdateRequests();
			
			this.locationManager = null;
		}

		if (this.sensorManager != null) {
			this.stopOrientationSensor();
			
			this.sensorManager = null;
		}

		this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS,
				"Manager disconnected"));

		AlrightManager.manager = null;
		
		return AlrightManager.manager;
	} // end disconnect

	/**
	 * Sets the best provider for location updates
	 * @return
	 */
	public void setBestProviderForLocationUpdates() {
		if(this.locationProvider == null) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(true);
			criteria.setCostAllowed(true);
			criteria.setPowerRequirement(Criteria.POWER_LOW);

			this.locationProvider = this.locationManager.getBestProvider(criteria, true /* enabled providers only*/);
		}
	} // end getBestProviderForLocationUpdates

	/**
	 * Notify the observes to state changes
	 * 
	 * @param state
	 */
	private void handleManagerStateChange(ManagerState state) {
		//Log.d(LOG_TAG, "Handling manager state change");
		for (ManagerStateListener listener : stateListener) {
			listener.onManagerStateChanged(state);
		}
	}

	/**
	 * Starts a new game
	 */
	public void newGame() {
		Log.d(LOG_TAG, "Starting new game");

		this.myDestination = null;
		this.onlyRightTurns = true;

		this.setMyLastKnownLocation();
		this.stopLocationUpdateRequests();
		this.stopOrientationSensor();
		
		// Notify client
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_NEW_GAME,
				"New game ready for play"));
	} // end newGame

	/**
	 * Starts the game by listening to location changes
	 */
	public void startGame() {
		Log.d(LOG_TAG,	"Starting game play...");

		if (this.myDestination == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Choose a destination..."));
			return;
		}

		this.setMyLastKnownLocation();
		this.startLocationUpdateRequests();
		this.startOrientationSensor();		
		
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_GAME_STARTED, "Game started"));
	} // end startGame

	/**
	 * Activate location update requests
	 */
	private void startLocationUpdateRequests() {
		Log.d(LOG_TAG, "Start listening for location updates");
		
		this.setBestProviderForLocationUpdates();
		this.locationManager.requestLocationUpdates(this.locationProvider,
				this.LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES,
				this.LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS, this);
	} // end startLocationUpdateRequests
	
	/**
	 * Deactivate location update requests
	 */
	private void stopLocationUpdateRequests() {
		Log.d(LOG_TAG, "Stop listening for location updates");
		
		this.locationManager.removeUpdates(this);
	} // end stopLocationUpdateRequests
	
		
	/**
	 * Active the Orientation sensor for compass data
	 */
	private void startOrientationSensor() {
		Log.d(LOG_TAG, "Start listening on the orientation sensor");
		
		this.sensorOrientation = this.sensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		if (this.sensorOrientation == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Manager could not initial the Android Orientation Sensor"));
			return;
		}

		Handler handler = new Handler();
		
		
		// NOTE: These listeners should be registered when activity resumes
		this.sensorManager.registerListener(this, this.sensorOrientation,
				SensorManager.SENSOR_DELAY_FASTEST);
	} // end startOrientationSensors

	/**
	 * Deactivate the Orientation sensor for compass data
	 */
	private void stopOrientationSensor() {
		Log.d(LOG_TAG, "Stop listening on the orientation sensor");
		
		if (this.sensorOrientation != null) {
			this.sensorManager.unregisterListener(this, this.sensorOrientation);
			
			this.sensorOrientation = null;
		}
	} // end stopOrientationSensor

	/**
	 * Sets the turn direction for game play. Default is TURN_DIRECTION_RIGHT.
	 * 
	 * @param direction
	 */
	public void completeGameSetup(boolean onlyRightTurns) {
		Log.d(LOG_TAG, "Setting the turn direction...");

		if(!(this.onlyRightTurns=onlyRightTurns))
			Collections.reverse(this.compassHeadings);
		
		this.handleManagerStateChange(new ManagerState(AlrightManager.STATE_TYPE_GAME_SETUP_COMPLETE,
				String.format("Only %s turns allow during game play", (this.onlyRightTurns)?"right":"left")));
		
	} // end setTurnDirection

	/**
	 * @return the last known location
	 */
	protected void setMyLastKnownLocation() {
		Log.d(LOG_TAG, "Get last known location");
				
		this.setBestProviderForLocationUpdates();
		Location location = this.locationManager.getLastKnownLocation(this.locationProvider);
		
		if(location != null) {
			this.handleManagerStateChange(new ManagerState(
					AlrightManager.STATE_TYPE_MY_LOCATION, 
					this.getAddress(location.getLatitude(),location.getLongitude())));
		} else {
			// TODO: Should we send this message
			this.handleManagerStateChange(new ManagerState(
					AlrightManager.STATE_TYPE_ERROR, "My location not set"));
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

		this.myDestination = this.getAddress(myDestination, position);

		this.handleManagerStateChange(new ManagerState(
				AlrightManager.STATE_TYPE_DESTINATION_CHANGED, 
				this.myDestination));
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
		;

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
		if(location.getLatitude() == this.myDestination.getLatitude() && 
			location.getLongitude() == this.myDestination.getLongitude()) {
			this.handleManagerStateChange(new ManagerState(AlrightManager.STATE_TYPE_GAME_OVER_WINNER, "YOU WIN!"));
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(LOG_TAG, String.format(
				"Location provider (%s) status changed with code [%s]",
				provider, status));

	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(LOG_TAG, String.format("Provider (%s) disabled...", provider));

		if(provider.equalsIgnoreCase(this.locationProvider)) {
			this.handleManagerStateChange(new ManagerState(
					AlrightManager.STATE_TYPE_ERROR, String.format("%s was disabled.", provider.toUpperCase())));
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if((sensor.getName().equalsIgnoreCase(Sensor.STRING_TYPE_ACCELEROMETER) || 
			sensor.getName().equalsIgnoreCase(Sensor.STRING_TYPE_MAGNETIC_FIELD)) && 
			accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH)  {
			this.handleManagerStateChange(new ManagerState(AlrightManager.STATE_TYPE_WARNING, "Calibration required for best game play"));
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.d(LOG_TAG, String.format("Sensor changed for: %s", event.sensor.getName()));

		synchronized(this) {
			float azimuth = 0, pitch = 0, roll = 0;
			switch (event.sensor.getType()) {
				case Sensor.TYPE_ORIENTATION:
					azimuth = event.values[0];
					pitch = event.values[1];
					roll = event.values[2];
					break;
					
				// NOTE: Plenty of samples to use Accelerometer and Magnetic Field but
				// all did something different and never gave the results we were looking
				// for. Using the following sensors  
				case Sensor.TYPE_ACCELEROMETER: // and the following
				case Sensor.TYPE_MAGNETIC_FIELD: // objects and methods
					// WindowsManager.getDisplay() 
					// Display.getRotation()
					// SensorManager.getRotationMatrix()
					// SensorManager.remapCoordinateSystem();    
				    // SensorManager.getOrientation();			
				default:
					break;
			}
	
			TrackingDetails trackingDetails = new TrackingDetails(this.locationProvider, 
					azimuth, pitch, roll, this.currentDirection);
					
			if(this.currentDirection ==  null)
				this.currentDirection = trackingDetails.Direction;
			
			// Are we still on track
			int stateType = AlrightManager.STATE_TYPE_STILL_ON_TRACK;
			if(!this.currentDirection.equalsIgnoreCase(trackingDetails.Direction)) {	
				// Get index of next possible direction
				int index = this.compassHeadings.indexOf(this.currentDirection)+1;
				index = (index < this.compassHeadings.size())? index: 0;
	
				// Did you make a wrong turn?
				String direction = this.compassHeadings.get(index); 
				if(!direction.equalsIgnoreCase(trackingDetails.Direction)){
					stateType = AlrightManager.STATE_TYPE_GAME_OVER_LOSER;					
				}
			} // end if
			
			this.handleManagerStateChange(new ManagerState(stateType, trackingDetails));
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch(which) {
			case Dialog.BUTTON_POSITIVE:
				dialog.dismiss();
				break;
		}
	}
} // end AlrightManager
