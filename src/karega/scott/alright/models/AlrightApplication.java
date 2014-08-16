package karega.scott.alright.models;

import java.io.FileOutputStream;
import java.io.PrintStream;

import android.app.Application;
import android.content.Context;

public class AlrightApplication extends Application implements Thread.UncaughtExceptionHandler {
	public final static String LOG_FILE_NAME = "alright.log";
	
	public AlrightApplication() {}
	
	@Override
	public void onCreate() {
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		FileOutputStream writer = null;
		try {
			writer = openFileOutput(LOG_FILE_NAME, Context.MODE_PRIVATE);
			ex.printStackTrace(new PrintStream(writer));
			writer.close();
			
		}  catch(Exception e){
			// DO NOTHING
		} // end try-catch
	}
	
} // end AlrightApplication
