package karega.scott.alright.models;

import karega.scott.alright.models.AlrightManager.ManagerState;
import karega.scott.alright.models.AlrightManager.ManagerStateListener;
import android.app.Application;
import android.content.Context;
import android.util.Log;

public class AlrightApplication extends Application implements 
	ManagerStateListener,
	Thread.UncaughtExceptionHandler 
{
	private final static String LOG_TAG = "Application";
	private AlrightManager manager;
	
	@Override
	public void onCreate() {
		Thread.setDefaultUncaughtExceptionHandler(this);
		
		this.manager = AlrightManager.getInstance(this.getBaseContext());
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.d(LOG_TAG, "Handling uncaught exception...");
		
		try {
			AlrightManager.handleApplicationError(this.getApplicationContext(), ex);
		} catch(Exception e) {
			// DO NOTHING
		} // end try-catch
	}

	@Override
	public void onManagerStateChanged(ManagerState state) {
		// TODO Auto-generated method stub
	}
} // end AlrightApplication
