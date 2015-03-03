package com.connectforever;

import java.util.Observable;
import java.util.Observer;

import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

/**
 * Wrapper class for the android Location updates, keeps track
 * of user's location.
 */
public class MyDeviceLocation extends Observable {
  private static final String TAG = "MyDeviceLocation";
  private static MyDeviceLocation meUpdater;
  private static LocationManager manager;
  private LocationListener listener;
  
  /**
   * Creates the listener that keeps track of the location once
   * it is connected to the location manager in the 
   * {@link #startUpdating() startUpdating()} method.
   * This listener notifies observers if the location changes.
   */
  private MyDeviceLocation() {
    Log.d(TAG, "Created a new MyDeviceLocation object");
    listener = new LocationListener() {
      public void onLocationChanged(Location newMe) {
        if (newMe != null) {
          setChanged();
          notifyObservers(newMe);
        }
      }
      
      public void onStatusChanged(String provider, int status, 
          Bundle extras) { }
      
      public void onProviderEnabled(String provider) { }
      
      public void onProviderDisabled(String provider) { }
    };
  }
  
  public static MyDeviceLocation getInstance() {
    if (meUpdater == null) {
      meUpdater = new MyDeviceLocation();
    }
    return meUpdater;
  }
  
  @Override
  public void addObserver(Observer observer) {
    super.addObserver(observer);
    Log.d(TAG, "Added observer, now " + countObservers());
    if (countObservers() == 1) {
      Log.d(TAG, "Starting updates");
      startUpdating();
    }
  }
  
  @Override
  public void deleteObserver(Observer observer) {
    super.deleteObserver(observer);
    Log.d(TAG, "Removed observer, now " + countObservers());
    if (countObservers() == 0) {
      Log.d(TAG, "Stopping updates");
      stopUpdating();
    }
  }
  
  /**
   * Sets the Context that is associated with this tracker.
   * This is used for finding the @link{LocationManager}.
   */
  public static void setLocationManager(LocationManager m) {
    manager = m;
  }
  
  /**
   * Stops updating the user's location.
   *
   * @return If stopping the updates succeeded. It can only succeed
   *         when the location manager has been set.
   */
  protected boolean stopUpdating() {
    if (manager == null) return false;
    Log.d(TAG, "PUSH stopped");
    manager.removeUpdates(listener);
    return true;
  }
  
  /**
   * Starts updating the user's location.
   *
   * @return If starting the updates succeeded. It can only succeed
   *         when the context has been set.
   */
  protected boolean startUpdating() {
    Log.d(TAG, "Requesting location updates...");
    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,
        listener, Looper.getMainLooper());
    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, 
        listener, Looper.getMainLooper());
    return true;
  }                                                                                           
}
