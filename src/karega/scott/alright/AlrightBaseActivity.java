package karega.scott.alright;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.Activity;
import android.util.Log;

/*
 * Alright base activity
 */
public abstract class AlrightBaseActivity extends Activity implements ManagerStateListener {
	private final static String LOG_TAG = "Alright Base Activity";
	
	protected AlrightManager manager;
	
	public AlrightBaseActivity() {
		Log.d(LOG_TAG, "Constructing...");
		this.manager = AlrightManager.getInstance(this.getBaseContext());
		this.manager.setManagerStateLister(this); // Tell manager whose listening
	}
	
	/*
	 * Handles the AlrightManager changes except AlrightManager.STATE_TYPE_ERROR
	 */
	protected abstract void onAlrightBaseActivityStateChanged(ManagerState state);
	
	/**
	 * Intercepts any AlrightManager.STATE_TYPE_ERROR within the AlrightManager.
	 * @param state
	 */
	public final void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling manager state changed event...");
		if(AlrightManager.STATE_TYPE_ERROR == state.stateType) {
			// TODO: Display message dialog
			return;
		}
		
		this.onAlrightBaseActivityStateChanged(state);
	} // end onManagerStateChanged
} // end BaseActivity
