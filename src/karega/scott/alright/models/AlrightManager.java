package karega.scott.alright.models;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
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

import android.util.ArrayMap;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

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
	private final static String LOG_TAG = "Alright Manager";
	private final static String LOG_FILE_NAME = "alright.log";
		
	public final static String ACTION_LOCATION_SUGGESTION = "karega.scott.alright.action.LOCATION_SUGGESTION";
	public final static String SUGGESTION_QUERY_POSITION = "suggestion_query_position";

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

	private final static int LOCATION_PROXIMITY_BUFFER = 3;
	
	private final static int ANGLE_BUFFER_SIZE = 5;
	private final static int ANGLE_0 = 0;
	private final static int ANGLE_90 = 90;
	private final static int ANGLE_180 = 180;
	
	private long LOCATION_UPDATE_REQUESTS_TIME_IN_MINUTES = 0;
	private float LOCATION_UPDATES_REQUEST_DISTANCE_IN_METERS = 0.914f; // 1
																		// yard;
	private int turnDirection = TURN_DIRECTION_RIGHT;
	private boolean isConnected = false;

	private LocationManager locationManager;

	private SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagneticField;

	private ArrayList<String> compassHeadings = new ArrayList<String>();
	private StringBuilder currentDirection = new StringBuilder();
	
	private float[] valuesForAccelerometer = null;
	private float[] valuesForMagneticField = null;

	private Context context;
	private Address myDestination;

	// TODO: is this the correct for Observable Pattern in Java
	private static ArrayList<ManagerStateListener> stateListener = new ArrayList<ManagerStateListener>();
	private static AlrightManager manager;
	
	/**
	 * Details related to the Magnetic Field Sensor
	 * @author kscott
	 *
	 */
	public class TrackingDetails {
		public final String Provider;
		public final float Axis_X_Pitch;
		public final float Axis_Y_Roll;
		public final float Axis_Z_Heading;
		public final boolean useDegrees;
		public final String Direction;
		
		public TrackingDetails(String providerName, float zHeading,	float xPitch, float yRoll) {
			this(providerName, zHeading, xPitch, yRoll, true);
		}
		
		public TrackingDetails(String providerName, float zHeading,	float xPitch, float yRoll, boolean useDegrees) {
			this.Provider = providerName;
			
			if(this.useDegrees = useDegrees) {
				this.Axis_X_Pitch = (float)Math.toDegrees(xPitch);
				this.Axis_Y_Roll = (float)Math.toDegrees(yRoll);
				this.Axis_Z_Heading = (float)Math.toDegrees(zHeading);
			} else {
				this.Axis_X_Pitch = xPitch;
				this.Axis_Y_Roll = yRoll;
				this.Axis_Z_Heading = zHeading;
			}
			
			this.Direction = AlrightManager.headingToCompassValue(this.Axis_Z_Heading);
		}	

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("Provider: %s, ", this.Provider));
			builder.append(String.format("Pitch (X): %s, ", this.Axis_X_Pitch));
			builder.append(String.format("Roll (Y): %s, ", this.Axis_Y_Roll));
			builder.append(String.format("Heading (Z): %s", this.Axis_Z_Heading));
			builder.append(String.format("Direction: %s", this.Direction));
			
			return builder.toString();
		}		
	} // end TrackingDetails

	/*
	 * Converts the Magnetic Field Sensor heading to compass string value
	 */
	private static String headingToCompassValue(float axis_z_Heading) {
		int heading = (int)axis_z_Heading;

		// NORTH is heading >= -5 and heading <= 5
		if((heading >= AlrightManager.ANGLE_0-AlrightManager.ANGLE_BUFFER_SIZE) && 
				(heading <= AlrightManager.ANGLE_0+AlrightManager.ANGLE_BUFFER_SIZE)) return "N";
		
		// NORTH EAST is heading > 5 and heading < 85
		if((heading > AlrightManager.ANGLE_0+AlrightManager.ANGLE_BUFFER_SIZE) && 
				   (heading < AlrightManager.ANGLE_90-AlrightManager.ANGLE_BUFFER_SIZE)) return "NE";
		
		// EAST is heading >= 85 and heading <= 95
		if((heading >= AlrightManager.ANGLE_90-AlrightManager.ANGLE_BUFFER_SIZE) && 
				   (heading <= AlrightManager.ANGLE_90+AlrightManager.ANGLE_BUFFER_SIZE)) return "E";
		
		// SOUTH EAST is heading > 95 and heading < 175
		if((heading > AlrightManager.ANGLE_90+AlrightManager.ANGLE_BUFFER_SIZE) && 
				   (heading < AlrightManager.ANGLE_180-AlrightManager.ANGLE_BUFFER_SIZE)) return "SE";
		
		// SOUTH is heading <= -175 or heading >= 175
		if((heading <= (AlrightManager.ANGLE_180-AlrightManager.ANGLE_BUFFER_SIZE)*-1) || 
				   (heading <= AlrightManager.ANGLE_180+AlrightManager.ANGLE_BUFFER_SIZE)) return "S";

		// NORTH WEST is heading > -85 and heading < -5
		if((heading > AlrightManager.ANGLE_90-AlrightManager.ANGLE_BUFFER_SIZE*-1) &&
				(heading < AlrightManager.ANGLE_0-AlrightManager.ANGLE_BUFFER_SIZE)) return "NW";
		
		// WEST is heading >=-95 and heading <= -85
		if((heading >= (AlrightManager.ANGLE_90+AlrightManager.ANGLE_BUFFER_SIZE)*-1) &&
				(heading <= (AlrightManager.ANGLE_90-AlrightManager.ANGLE_BUFFER_SIZE)*-1)) return "W";
		
		// SOUTH WEST is heading < -95 and heading >= -175
		if((heading < (AlrightManager.ANGLE_90-AlrightManager.ANGLE_BUFFER_SIZE)*-1) &&
				(heading > (AlrightManager.ANGLE_180-AlrightManager.ANGLE_BUFFER_SIZE)*-1)) return "SW";

		return "@#$#%";
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
			this.stopAccelerometerSensor();
			this.stopMagneticFieldSensor();
			
			this.sensorManager = null;
		}

		this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS,
				"Manager disconnected"));

		AlrightManager.manager = null;
		
		return AlrightManager.manager;
	} // end disconnect

	/**
	 * Gets the best provider for location updates
	 * @return
	 */
	public String getBestProviderForLocationUpdates() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(true);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		return this.locationManager.getBestProvider(criteria, true /* enabled providers only*/);
	} // end getBestProviderForLocationUpdates

	/**
	 * Notify the observes to state changes
	 * 
	 * @param state
	 */
	private void handleManagerStateChange(ManagerState state) {
		Log.d(LOG_TAG, "Handling manager state change");
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
		this.turnDirection = TURN_DIRECTION_RIGHT;

		this.stopAccelerometerSensor();
		this.stopMagneticFieldSensor();
		this.stopLocationUpdateRequests();
		
		this.setMyLastKnownLocation();
		
		// Notify client
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_NEW_GAME,
				"New game ready for play"));
	} // end newGame

	/**
	 * Starts the game by listening to location changes
	 */
	public void startGame() {
		Log.d(LOG_TAG,	"Game starting to request location updates for monitoring...");

		if (this.myDestination == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Choose a destination..."));
			return;
		}

		this.startAccelerometerSensor();
		this.startMagneticFieldSensor();
		this.startLocationUpdateRequests();
		
		this.setMyLastKnownLocation();
		
		if(this.turnDirection == AlrightManager.TURN_DIRECTION_LEFT) {
			Collections.reverse(this.compassHeadings);
		}
		
		this.handleManagerStateChange(new ManagerState(STATE_TYPE_GAME_STARTED,
				"Game started"));
	} // end startGame

	/**
	 * Activate location update requests
	 */
	private void startLocationUpdateRequests() {
		Log.d(LOG_TAG, "Start listening for location updates");
		
		String locationProvider = this.getBestProviderForLocationUpdates();
		this.locationManager.requestLocationUpdates(locationProvider,
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
	 * Activate the accelerometer sensor for motion information
	 */
	private void startAccelerometerSensor() {
		Log.d(LOG_TAG, "Start listening on the accelerometer sensor");
		
		this.sensorAccelerometer = this.sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (this.sensorAccelerometer == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Manager could not initial the Android Accelerometer Sensor"));
			return;
		}

		// NOTE: These listeners should be registered when activity resumes
		this.sensorManager.registerListener(this, this.sensorAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	} // end startAccelerometerSensor

	/**
	 * Deactivate the accelerometer sensor for motion information
	 */
	private void stopAccelerometerSensor() {
		Log.d(LOG_TAG, "Stop listening on the accelerometer sensor");
		
		if (this.sensorAccelerometer != null) {
			this.sensorManager.unregisterListener(this, this.sensorAccelerometer);
			this.sensorAccelerometer = null;
		}
	} // end stopAccelerometerSensor	

	/**
	 * Active the magnetic field sensor for compass data
	 */
	private void startMagneticFieldSensor() {
		Log.d(LOG_TAG, "Start listening on the magnetic field sensor");
		
		this.sensorMagneticField = this.sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (this.sensorMagneticField == null) {
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_ERROR,
					"Manager could not initial the Android Magnetic Field Sensor"));
			return;
		}

		// NOTE: These listeners should be registered when activity resumes
		this.sensorManager.registerListener(this, this.sensorMagneticField,
				SensorManager.SENSOR_DELAY_NORMAL);
	} // end startMagneticFieldSensors

	/**
	 * Deactivate the magnetic field sensor for compass data
	 */
	private void stopMagneticFieldSensor() {
		Log.d(LOG_TAG, "Stop listening on the magnetic field sensor");
		
		if (this.sensorMagneticField != null) {
			this.sensorManager.unregisterListener(this, this.sensorMagneticField);
			
			this.sensorMagneticField = null;
		}
	} // end stopMagneticFieldSensors
	
	/**
	 * Sets the turn direction for game play. Default is TURN_DIRECTION_RIGHT.
	 * 
	 * @param direction
	 */
	public void setTurnDirection(int direction) {
		Log.d(LOG_TAG, "Setting the turn direction...");

		switch (direction) {
		case TURN_DIRECTION_LEFT:
		case TURN_DIRECTION_RIGHT:
			this.turnDirection = direction;
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS,
					String.format("Turn direction set to %s", direction)));
			break;

		default:
			this.turnDirection = TURN_DIRECTION_RIGHT;
			this.handleManagerStateChange(new ManagerState(STATE_TYPE_SUCCESS,
					String.format(
							"%s is invalid. Turn direction set to right, %s)",
							direction, TURN_DIRECTION_RIGHT)));
			break;
		}
	} // end setTurnDirection

	/**
	 * @return the last known location
	 */
	protected void setMyLastKnownLocation() {
		Log.d(LOG_TAG, "Get last known location");
				
		String locationProvider = this.getBestProviderForLocationUpdates();
		Location location = this.locationManager.getLastKnownLocation(locationProvider);
		
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
	 *            is a string represent by the search query
	 */
	public void setMyDestination(String myDestination) {
		Log.d(LOG_TAG,	String.format("Setting my destination to %s", myDestination));

		this.myDestination = this.getAddress(myDestination);

		this.handleManagerStateChange(new ManagerState(
				AlrightManager.STATE_TYPE_DESTINATION_CHANGED, 
				this.myDestination));
	} // end setMyDestination

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
//		if(location.getLatitude() >= this.myDestination.getLatitude()-AlrightManager.LOCATION_PROXIMITY_BUFFER &&
//				location.getLatitude() <= this.myDestination.getLatitude()+AlrightManager.LOCATION_PROXIMITY_BUFFER &&
//				location.getLongitude() >= this.myDestination.getLongitude()-AlrightManager.LOCATION_PROXIMITY_BUFFER &&
//				location.getLongitude() <= this.myDestination.getLongitude()+AlrightManager.LOCATION_PROXIMITY_BUFFER) {			
//		}
		
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
		Log.d(LOG_TAG, String.format("Provider (%s) enabled...", provider));

		String locationProvider = this.getBestProviderForLocationUpdates();
				
		if (locationProvider.equalsIgnoreCase(provider)) {			
			this.stopLocationUpdateRequests();
			this.startLocationUpdateRequests();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(LOG_TAG, String.format("Provider (%s) disabled...", provider));

		this.stopLocationUpdateRequests();
		this.startLocationUpdateRequests();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.d(LOG_TAG, String.format("Sensor changed for: %s", event.sensor.getName()));

		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			this.valuesForAccelerometer = event.values.clone();
			break;

		case Sensor.TYPE_MAGNETIC_FIELD:
			this.valuesForMagneticField = event.values.clone();
			break;
		}

		if(this.valuesForAccelerometer == null || this.valuesForMagneticField == null)
			return;
		
		float[] inR = new float[9];
		if(SensorManager.getRotationMatrix(inR, null, this.valuesForAccelerometer, this.valuesForMagneticField)) {
			// Remap the coordinates based on the natural device orientation.
		    int x_axis = SensorManager.AXIS_X; 
		    int y_axis = SensorManager.AXIS_Y;

			Display display = ((WindowManager) this.context
					.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

		    switch (display.getRotation()) {
		      	case (Surface.ROTATION_90):  
		      		x_axis = SensorManager.AXIS_Y; 
		      		y_axis = SensorManager.AXIS_MINUS_X; 
		      		break;
		      		
		      	case (Surface.ROTATION_180):
		    	  	x_axis = SensorManager.AXIS_X; 
		        	y_axis = SensorManager.AXIS_MINUS_Y; 
		        	break;
		        	
		      	case (Surface.ROTATION_270): 
		      		x_axis = SensorManager.AXIS_MINUS_Y; 
		      		y_axis = SensorManager.AXIS_X; 
		      		break;
		      		
		      default: 
		    	  break;
		    }

		    float[] outR = new float[9];
			float[] values = new float[3];
			
		    SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);    
			SensorManager.getOrientation(outR, values);

			String locationProvider = this.getBestProviderForLocationUpdates();
			TrackingDetails trackingDetails = new TrackingDetails(locationProvider, 
					values[0 /*AXIS_Z*/], values[1/*AXIS_X*/], values[2/*AXIS_Y*/]);

			if(this.currentDirection.length() == 0) {
				this.currentDirection = new StringBuilder(trackingDetails.Direction);
				return;
			}
			
			int stateType = AlrightManager.STATE_TYPE_STILL_ON_TRACK;
			if(!this.currentDirection.toString().equalsIgnoreCase(trackingDetails.Direction)) {	
				// Get index of next possible direction
				int index = this.compassHeadings.indexOf(this.currentDirection)+1;
				index = (index < this.compassHeadings.size())? index: 0;
				
				// Did you make a wrong turn?
				if(!this.compassHeadings.get(index).equalsIgnoreCase(trackingDetails.Direction)){
					stateType = AlrightManager.STATE_TYPE_GAME_OVER_LOSER;					
				}
			} // end if
			
			this.handleManagerStateChange(new ManagerState(stateType, trackingDetails));
		} // end if
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
