package com.connectforever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
/**
 * Decides on starting / stopping the Location Service if the network 
 * connectivity changed.
 */
public class ServiceManager extends BroadcastReceiver {

  public static final String TAG = "ServiceManager";
  
  @Override
  public void onReceive (Context context, Intent intent) {
    Log.d(TAG, "Received Connectivity Broadcast");
    // TODO: finish registering if in locally_registered state
    if (hasInternet(context, intent)) {
      if(SharedPrefs.getInstance().shouldSendId()) {
        try {
          WebAPI.registerOrUpdateID();
        }
        catch (WebApiException e) {
          Log.w(TAG, "Could not register with web api.");
        }
      }
    }
  }
    
  private boolean hasInternet(Context context, Intent intent) {  
    NetworkInfo info = (NetworkInfo) 
        intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
    if (info.getState() == NetworkInfo.State.CONNECTED) {
      Log.d(TAG, "Device now connected to the internet");
    }
    else {
      Log.d(TAG, "Connection to the internet lost");
    }
    return info.getState() == NetworkInfo.State.CONNECTED;
  }
  
}

