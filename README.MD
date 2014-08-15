== Alright 
This is a work in progress but the concept is design a game around navigation. The app
would allow the user to pick a destination address. Then from there current location restrict
the direction (bearing) to straight and making either all right or left turns. Until they
reach there destination. Seems simply enough considering most, if not, all mobile devices 
today come equipped with GPS hardware. The idea is sound but raises some questions.

How accurate will the location information be from GPS or Network (Gyroscope out 
cause I don't have a device with this senor)?

How quickly will the location information be provided?

How will I detect and restrict direction to help determine if you win or lose?

Finally, how will I test?


This application touches on the following concepts:

* Customer Search integration
Using the SearchManager, SearchVew and custom ContentProvider, this application leverges
the native search functionality built into Android. The custom content provider simply
provides the data the Search function needs.

* Location and Geocoder
Android and Google provide a couple services that provide Android application access
to GPS information. Using the LoctionManager, GoogleMap and MapFragment UI, you
can get your current location, bearings and distance to some know endpoint.

* UI Layout and Resource
Well, there's alot to learn here. First starters, UI is handling XML base files that
separates UI development from the code that supports it. Much like developing HTML
pages for the web but more powerful. Like HTML controls, Android provides a rich 
set of UI controls that includes layouts, inputs, buttons, images etc. The advantage 
is built absolute and relative positioning without the need to common CSS found with
HTML design. Plus you can still provide further style similar to CSS to extend its
flexibility.

All the layouts, styling and images for various device resolutions is found a the
res folder of you Android application.

* Configuration
I have to mention this. Convention over Configuration. The idea is using a common
understanding of how your application is structured and classes are named, the 
runtime engine could inspect the objects and perform some action BASED on this 
know structure and naming style. So, why some many configuration files. 

With that said, the AndroidManifest.xml file is King. It allows you to configure
the layouts, target a minimal device specific OS version, and configuring access
to internal and external services such as Search, Google Map V2 API and the
Custom Alright ContentProvider.

* Free Tools used for this project
Eclipse
GitHub 
Git
Team Foundation Services (TFS) (free www.visualstudio.com)
Notepad++
Apktool.jar
ADB

* Android Studio Beta
I did try it, but in my opinion to much, especially with the caching of offline stuff and
configuration of gradle. I believe this are great tools but for someone with no real internet
connection not worth it.

* Apktool.jar
This is a great reverse engineering tool for existing .APK. I used to to look at how the UI 
was handled for Maps.apk found on most stanard stock Android devices. It helping with consist
look and feel, since this is important.

* SPECIAL NOTES
The Google Map API requires service API keys that are only provided to Google Account
holders via Console Developer Google site. For the purpose of this application, I
intensionally exclude the AndroidManifest.xml file for security reason. However, 
you should create two API keys used for development and production.

You will also need to understanding code signing for Java and how this applies to
the generating development or production APK files, AND, this process could be
different depending on the IDE you use.


Below is the AndroidManifest.xml, but you will need to add your own API keys

<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="karega.scott.alright"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk
        android:minSdkVersion="17"
        android:targetSdkVersion="20" />

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <uses-feature
        android:glEsVersion="0x000200000"
        android:required="true" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >

        <provider
            android:name=".models.AlrightProvider"
            android:authorities="karega.scott.alright.provider.Location"
            android:enabled="true"
            android:exported="false" >
        </provider>
        
        <meta-data
            android:name="com.google.android.gms.version"
            android:value="5089000" />
        <meta-data
            android:name="com.google.android.maps.v2.API_KEY"
            android:value="@string/google_dev_api_key" />

        <activity
            android:name=".MainActivity"
            android:label="@string/title_activity_main"
            android:launchMode="singleTop"
            android:theme="@style/AppTheme" >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <activity
            android:name=".GameSetupActivity"
            android:launchMode="singleTop"
            android:label="@string/title_activity_game_setup"
            android:parentActivityName=".AlrightBaseActivity" >
            <intent-filter>
                <action android:name="android.intent.action.SEARCH" />
                <!--
                action android:name="karega.scott.alright.GameActivity.LOCATION_SUGGESTION_ACTION"/>
                <data android:mimeType="vnd.android.cursor.dir/vnd.karega.scott.alright.location"/

                -->
            </intent-filter>

            <meta-data
                android:name="android.app.searchable"
                android:resource="@xml/searchable" />
            <meta-data
                android:name="android.support.PARENT_ACTIVITY"
                android:value="karega.scott.alright.AlrightBaseActivity" />
        </activity>
        <activity
            android:name=".LocationTrackerActivity"
            android:label="@string/title_activity_game_location_tracker"
            android:launchMode="singleTop"
            android:theme="@style/AppTheme" >
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
            </intent-filter>
        </activity>
        <activity
            android:name=".HelpActivity"
            android:label="@string/title_activity_game_help"
            android:launchMode="singleTop"
            android:theme="@style/AppTheme" >
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
            </intent-filter>
        </activity>    </application>

</manifest>
