package karega.scott.alright;

import karega.scott.alright.models.AlrightManager.ManagerState;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class HelpActivity extends AlrightBaseActivity {
	private final static String LOG_TAG = "Alright Help Activity";
	private ActionBar actionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Creating the activity");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		if(actionBar == null) {
			actionBar = this.getActionBar();
			actionBar.hide();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "Creating menu options for this activity");
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.help, menu);
		return true;
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
	protected void onAlrightBaseActivityStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling base activity state changes...");
		
	}
} // end HelpActivity
