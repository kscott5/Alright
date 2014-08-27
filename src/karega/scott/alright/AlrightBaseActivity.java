package karega.scott.alright;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/*
 * Alright base activity
 */
public abstract class AlrightBaseActivity extends Activity implements 
	ManagerStateListener
{ 	
	private final static String LOG_TAG = "AlrightBase";
	
	protected AlrightManager manager;
	
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
