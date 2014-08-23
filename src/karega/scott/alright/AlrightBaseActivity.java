package karega.scott.alright;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

/*
 * Alright base activity
 */
public abstract class AlrightBaseActivity extends Activity implements 
	ManagerStateListener,
	OnClickListener
{
	private final static String LOG_TAG = "Alright Base Activity";
	
	protected AlrightManager manager;
	
	/*
	 * Handles the AlrightManager changes except AlrightManager.STATE_TYPE_ERROR
	 */
	protected abstract void onAlrightBaseActivityStateChanged(ManagerState state);
	
	/**
	 * Intercepts the following STATE_TYPES <br/>
	 * <br/>
	 *  AlrightManager.STATE_TYPE_ERROR<br/>
	 *  AlrightManager.STATE_TYPE_ORIENTATION_CHANGED<br/>
	 *  <br/>
	 * @param state
	 */
	public final void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling manager state changed event...");
		if(AlrightManager.STATE_TYPE_ERROR == state.stateType) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this.getBaseContext());
			builder.setMessage(state.stateData.toString()).setPositiveButton("OK", this).show();
			return;
		}
		
		if(AlrightManager.STATE_TYPE_ORIENTATION_CHANGED == state.stateType) {
			Log.d(LOG_TAG, String.format("Orientation changed: %s\n", (AlrightManager.TrackingDetails)state.stateData));
		}
		
		this.onAlrightBaseActivityStateChanged(state);
	} // end onManagerStateChanged

	
	public void onClick(DialogInterface dialog, int id) {
        switch(id) {
        	case DialogInterface.BUTTON_POSITIVE:
        		dialog.dismiss();
        		break;
        }
    }

	// In general the movement through an activity's lifecycle looks like this:	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Create the activity");
		
		super.onCreate(savedInstanceState);
		
		if(this.manager == null) {
			this.manager = AlrightManager.getInstance(
					this.getBaseContext()).connect();
			
			this.manager.setManagerStateListener(this); // Tell manager whose listening
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
     
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "Destroy the activity");
				
		if(this.manager != null) {
			this.manager = this.manager.disconnect();			
		}
		
		super.onDestroy();
	}
} // end BaseActivity
