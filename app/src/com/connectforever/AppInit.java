package com.connectforever;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.location.LocationManager;
import com.connectforever.WebAPI;
import com.connectforever.MyDeviceLocation;

/**
 * Handles one-time only settings. Sets the static Context-related
 * variables in the WebApi and MyDeviceLocation classes.
 */
public class AppInit extends Application {
  private static final String TAG = "AppInit";
  
  @Override
  public void onCreate() {
    Log.d(TAG, "Connect Forever started.");
    super.onCreate();
    SharedPrefs.getInstance().setSharedPreferences(
        PreferenceManager.getDefaultSharedPreferences(this));
    WebAPI.setContext(this);
    WebAPI.setConnectivityManager( (ConnectivityManager)
        getSystemService(Context.CONNECTIVITY_SERVICE));
    MyDeviceLocation.setLocationManager( (LocationManager)
        getSystemService(Context.LOCATION_SERVICE));
  }
}
