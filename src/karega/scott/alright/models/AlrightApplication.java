package karega.scott.alright.models;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class AlrightApplication extends Application implements 
	Thread.UncaughtExceptionHandler 
{
	private final static String LOG_TAG = "Alright Application";
		
	public AlrightApplication() {
	}
	
	@Override
	public void onCreate() {
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.d(LOG_TAG, "Handling uncaught exception...");
		
		try {
			AlrightManager.handleApplicationError(this.getBaseContext(), ex);
		} catch(Exception e) {
			// DO NOTHING
		} // end try-catch
	}
} // end AlrightApplication
