package com.connectforever;

import java.util.Observable;
import java.util.Observer;
import android.location.Location;
import android.util.Log;

/**
 * Reads one location update and sends it to the Web API.
 */
public class SingleLocationRequester implements Observer {

  public static final String TAG = "SingleLocationRequester";
  private static final long timeout = 30 * 1000;
  private static SingleLocationRequester requester;
  private boolean shouldSendQuickFix = true;
  private long endTimeMillis;
  
  private SingleLocationRequester() {
    super();
  }
  
  public static SingleLocationRequester getInstance() {
    if (requester == null) {
      requester = new SingleLocationRequester();
    }
    return requester;
  }
  
  public void sendLocation() {
    Log.d(TAG, "Requesting location updates");
    // restart sending
    shouldSendQuickFix = true;
    endTimeMillis = System.currentTimeMillis() + timeout;
    MyDeviceLocation.getInstance().addObserver(this);
  }
  
  public void update(Observable observable, Object data) {
    Location location = (Location) data;
    try {
      if (shouldSendQuickFix) {
        Log.d(TAG, "Quick Fix Obtained.");
        WebAPI.setLocation(location);
        shouldSendQuickFix = false;
      }
      if (location.getAccuracy() < 10.0 ||
          System.currentTimeMillis() > endTimeMillis) {
        Log.d(TAG, "Accurate Fix Obtained. Stopping updates");
        WebAPI.setLocation(location);
        MyDeviceLocation.getInstance().deleteObserver(this);
      }
    }
    catch (WebApiException e) {
      Log.e(TAG, "Could not send location: " + e.getMessage());
    }
  }
  
  public void stopSending() {
    MyDeviceLocation.getInstance().deleteObserver(this);
  }
}
