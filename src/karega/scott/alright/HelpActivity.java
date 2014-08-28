package karega.scott.alright;

import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class HelpActivity extends Activity implements
	ManagerStateListener
{
	private final static String LOG_TAG = "Help";
	
	private AlrightManager manager;
	private ActionBar actionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		this.manager = AlrightManager.getInstance(this.getBaseContext()).connect();
		
		if(actionBar == null) {
			actionBar = this.getActionBar();
			actionBar.hide();
		}
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart");
		
		super.onStart();
		
		// Activity visible start listening
		this.manager.addManagerStateListener(this);
	}
	
	@Override
	protected void onPause() {
		Log.d(LOG_TAG, "onPause");
		
		super.onPause();
		
		if(this.isFinishing()) {
			// NOTE: This should only be done once in MainActivity 
			// Application is closing for good
			//this.manager.disconnect();
		}
	}
	
	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onStop");
		
		super.onStop();
		
		// Activity hidden stop listening
		this.manager.removeManagerStateListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
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
	public void onManagerStateChanged(ManagerState state) {
		// TODO Auto-generated method stub
		
	}
} // end HelpActivity
