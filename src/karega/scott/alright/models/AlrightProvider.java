package karega.scott.alright.models;

import java.util.List;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * General content provider that supplies application specific information
 * @author kscott
 *
 */
public class AlrightProvider extends ContentProvider {
	private final static String LOG_TAG = "ContentProvider";
	
    // List of possible content provider uri. Add more as needed
    private final static String CONTENT_PATH = "geocoder";
    
    public final static String AUTHORITY = "karega.scott.alright.provider.Location";
    
    public final static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    // MIME types used for searching the application.
    public final static String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.karega.scott.alright.location";
    public final static String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.karega.scott.alright.location";
    
    // UriMatcher stuff. Add more as needed
    private final static int LOCATION = 0;
    private final static int LOCATION_SUGGEST_SHORTCUT = 1;
    private final static int LOCATION_SUGGEST_QUERY = 2;

    private final static UriMatcher uriMatcher = buildUriMatcher();

    private AlrightManager manager;

    /**
     * Constructor
     */
	public AlrightProvider() {}

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        // to get definitions...
        matcher.addURI(AUTHORITY, CONTENT_PATH, LOCATION);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT , LOCATION_SUGGEST_SHORTCUT);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, LOCATION_SUGGEST_QUERY);
        
        return matcher;
    } // end buildUriMatcher

    /**
     * @return {@link MatrixCursor}
     */
    private static MatrixCursor getMatrixCursor() {
    	return new MatrixCursor(
    			new String[] {
	    			BaseColumns._ID,
	    			SearchManager.SUGGEST_COLUMN_TEXT_1,
	    			SearchManager.SUGGEST_COLUMN_TEXT_2,
	    			SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
	    			SearchManager.SUGGEST_COLUMN_INTENT_DATA,
	    			SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
    	});
    } // end getMatrixCursor
    
    /**
     * Search {@link Geocoder} by location name
     * @param query arguments to search by
     * @return Cursor with information
     */
    private Cursor getLocationsByName(String[] queryArgs) {
    	Log.d(LOG_TAG, String.format("Get locations by name [%s]", TextUtils.join(" ", queryArgs)));
    	
    	MatrixCursor cursor = getMatrixCursor();
    	
    	try {
	    	int id = -1;
	    	
	    	List<Address> addrs = this.manager.getAddresses(TextUtils.join(" ", queryArgs));
	    	for(Address addr : addrs) {
		    	if(addr.getMaxAddressLineIndex() > 0) {
		    		
		    		StringBuilder data = new StringBuilder();
		    		for(int index=0; index<addr.getMaxAddressLineIndex(); index++) {
		    			data.append(String.format("%s ",addr.getAddressLine(index)));
		    		}
		    		
		    		cursor.addRow(new Object[]{
		    				+id, //BaseColumns._ID,
			    			data, //SearchManager.SUGGEST_COLUMN_TEXT_1,
			    			data, //SearchManager.SUGGEST_COLUMN_TEXT_2,
			    			data, //SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
			    			data, //SearchManager.SUGGEST_COLUMN_INTENT_DATA,
			    			id //SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
			    	});
	    		} // end if
	    	} // end for
    	} catch(Exception e) {
    		Log.e(LOG_TAG, e.toString());
    	}
    	
    	return cursor;
    } // end getLocationsByName
        
    @Override
	public String getType(Uri uri) {
		Log.d(LOG_TAG, String.format("Get mime type from content provider URI[%s]",uri.toString()));
		
		switch(uriMatcher.match(uri)) {
			case LOCATION:
				return CONTENT_TYPE;

			case LOCATION_SUGGEST_SHORTCUT:
			case LOCATION_SUGGEST_QUERY:
				return SearchManager.SUGGEST_MIME_TYPE;
				
			default:
				throw new IllegalArgumentException(String.format("Unknown Uri: %", uri));
		}
	} // end getType

	@Override
	public boolean onCreate() {
		Log.d(LOG_TAG, "Creating...");
		
		this.manager = AlrightManager.getInstance(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(LOG_TAG, String.format("Querying URI [%s]", uri.toString()));
		
		switch(uriMatcher.match(uri)) {
			case LOCATION:
			case LOCATION_SUGGEST_SHORTCUT:
			case LOCATION_SUGGEST_QUERY:
				return getLocationsByName(selectionArgs);
		
			default:
				return null;
		}		
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported");
	}
}
