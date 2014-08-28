package karega.scott.alright;


import karega.scott.alright.models.AlrightManager;
import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
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
public class MainActivity extends Activity implements
	ManagerStateListener,
	OnClickListener 
{
	private final static String LOG_TAG = "Main";

	private AlrightManager manager;
	private ActionBar actionBar;
	private Button newGameButton;
	private Button helpButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.manager = AlrightManager.getInstance(getApplicationContext()).connect();
		
		this.newGameButton = (Button)this.findViewById(R.id.new_game);
		this.newGameButton.setOnClickListener(this);
		this.newGameButton.setClickable(false);
		
		this.helpButton = (Button)this.findViewById(R.id.help_game);
		this.helpButton.setOnClickListener(this);
	
		if(this.actionBar == null) {
			this.actionBar = this.getActionBar();
			this.actionBar.hide();
		}		
	} // end onCreate

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
			this.manager.disconnect();
		}
	}
	
	@Override 
	protected void onResume() {
		Log.d(LOG_TAG, "onResume");
		super.onResume();
		
		// Finish any activity started with this result code
		this.finishActivity(RESULT_OK);
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
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	} // end onCreateOptionsMenu

	@Override
	public void onClick(View v) {
		Log.d(LOG_TAG, "onClick");
				
		switch(v.getId()) {		
			case R.id.new_game:
				Intent setup = new Intent(this, GameSetupActivity.class);
				this.startActivityForResult(setup, RESULT_OK);
				break;
				
			case R.id.help_game:
				Intent help = new Intent(this, HelpActivity.class);
				this.startActivityForResult(help, RESULT_OK);
				break;
		}
	} // end onClick

	public void onManagerStateChanged(ManagerState state) {
		Log.d(LOG_TAG, String.format("onManagerStateChanged %s", state));
		
		switch(state.stateType){
			case GAME_SETUP_COMPLETE:
				this.finishActivity(RESULT_OK);
				
				Intent tracker = new Intent(this, LocationTrackerActivity.class);
				this.startActivityForResult(tracker, RESULT_OK);
				break;
			
			case CONNECTED:
				this.newGameButton.setClickable(true);
				break;
				
			case ERROR:
				// TODO: Show DialogFragment
				break;
			
			case GAME_OVER_LOSER:
				// TODO: Show DialogFragment
				break;
				
			case GAME_OVER_WINNER:
				// TODO: Show DialogFragment
				break;
		}
	}
} // end MainActivity

