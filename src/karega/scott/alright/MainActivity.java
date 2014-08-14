package karega.scott.alright;


import karega.scott.alright.models.AlrightManager.ManagerState;
import android.app.Activity;
import android.content.Intent;

import android.widget.Button;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import android.app.ActionBar;
import android.util.Log;

/**
 * Main activity for game play (NOTE: MAKE THIS SPLASH SCREEN)
 * @author kscott
 *
 */
public class MainActivity extends AlrightBaseActivity implements OnClickListener {
	private final static String LOG_TAG = "Alright Main Activity";

	private ActionBar actionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "Creating the activity");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button newgame = (Button)this.findViewById(R.id.new_game);
		newgame.setOnClickListener(this);
		
		Button helpgame = (Button)this.findViewById(R.id.help_game);
		helpgame.setOnClickListener(this);
	
		if(actionBar == null) {
			actionBar = this.getActionBar();
			actionBar.hide();
		}		
	} // end onCreate

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "Creating menu options for activity...");
		
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	} // end onCreateOptionsMenu

	@Override
	public void onClick(View v) {
		Log.d(LOG_TAG, "Starting child activity");
		
		Intent intent = null;
		
		switch(v.getId()) {		
			case R.id.new_game:
				intent = new Intent(this, GameSetupActivity.class);
				break;
			case R.id.help_game:
				intent = new Intent(this, HelpActivity.class);
				break;
		}
		
		if(intent != null) {
			this.startActivity(intent);
		}
	} // end onClick

	@Override
	protected void onAlrightBaseActivityStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling base activity state changes...");
	
	}
} // end MainActivity

