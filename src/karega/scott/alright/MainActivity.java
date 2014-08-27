package karega.scott.alright;


import karega.scott.alright.models.AlrightManager;
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
public class MainActivity extends AlrightBaseActivity implements 
	OnClickListener 
{
	private final static String LOG_TAG = "Main";

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
				
		switch(v.getId()) {		
			case R.id.new_game:
				Intent setup = new Intent(this, GameSetupActivity.class);
				this.startActivityForResult(setup, this.RESULT_OK);
				break;
				
			case R.id.help_game:
				Intent help = new Intent(this, HelpActivity.class);
				this.startActivityForResult(help, this.RESULT_OK);
				break;
		}
	} // end onClick

	public void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, "Handling manager state changes...");
		
		switch(state.stateType){
			case AlrightManager.STATE_TYPE_GAME_SETUP_COMPLETE:
				this.finishActivity(RESULT_OK);
				
				Intent tracker = new Intent(this, LocationTrackerActivity.class);
				this.startActivity(tracker);
				break;
				
			case AlrightManager.STATE_TYPE_ERROR:
				// TODO: Show DialogFragment
				break;
			
			case AlrightManager.STATE_TYPE_GAME_OVER_LOSER:
				// TODO: Show DialogFragment
				break;
				
			case AlrightManager.STATE_TYPE_GAME_OVER_WINNER:
				// TODO: Show DialogFragment
				break;
		}
	}
} // end MainActivity

