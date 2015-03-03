package com.connectforever;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.BitmapFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.IntentFilter;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.location.Location;
import android.view.Display;
import android.view.View;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.net.ConnectivityManager;
import android.net.Uri;
import java.util.Observable;
import java.util.Observer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.lang.Runtime;
import java.lang.Thread;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
/**
 * Shows the main view (with the arrow). Listens to the device's sensors,
 * and observes own and other's location. Combines all these observations
 * into a rotation, and draws this rotation to the screen.
 * The drawing of the arrow is handled by Arrow.java.
 * The activity's background can be chosen by the user.
 */
public class MainActivity extends BaseActivity implements Observer {
  private static final String TAG = "MainActivity";
  
  private static final int RESET        = 0;
  private static final int PHOTO_PICKED = 1;
  private static final int PICK_ARROW   = 2;
  
  private static final String DIRECTORY = "/.connectforever";
  private static final String WALLPAPER_FILE = "/wallpaper.jpg";
  
  private Bitmap    largePhoto;
  private Arrow     arrow;
  //private ImageView together;
  
  private Thread           arrowThread;
  private Location         me;
  private Location         other;
  private float            north;
  
  private SensorManager  sensorManager;
  private SensorEventListener sensorListener;
  
  ProgressDialog connectingDialog;
  
  private boolean shouldShowConnectingDialog = false;
  private boolean shouldGetLocation = true;
  
  /**
   * Called when activity is created.
   * Registers for the location updates, sets the view's renderer.
   */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    loadBackground();
    // Initiate the image view
    //together = (ImageView) findViewById(R.id.together);
    arrow = (Arrow) findViewById(R.id.arrow);
    arrow.setScreenSize(getWindowManager().getDefaultDisplay().getWidth(), 
        getWindowManager().getDefaultDisplay().getHeight());
  }
  
  
  /**
   * Tries to request the location of the other device from the api.
   * Shows the appropriate dialogs if this goes wrong.
   */
  private class GetLocationTask extends AsyncTask<Void, Void, WebApiException> {
    @Override
    protected WebApiException doInBackground(Void... voids) {
      try {
        WebAPI.getLocation();
      }
      catch (WebApiException e) {
        return e;
      }
      return null;
    }
    
    @Override
    protected void onPostExecute(WebApiException exception) {
      if (exception != null) {
        // other reset the app
        if (WebApiException.REASON_NOT_CONNECTED == exception.getReason()) {
          SharedPrefs.getInstance().setConnected(false);
          connectingDialog.dismiss();
          connectingDialog = null;
          shouldShowConnectingDialog = false;
          showDialog(DIALOG_NOT_CONNECTED);
        }
        // could not connect to api
        else if (WebApiException.REASON_NO_INTERNET == exception.getReason()) {
          showDialog(DIALOG_NO_INTERNET);
        }
        else {
          showExceptionDialog(exception);
        }
      }
    }
  }
  
  /**
   * Called when the activity is resumed. Starts all the updates.
   */
  @Override
  public void onResume() {
    super.onResume();
    if (mainDialog != null) mainDialog.dismiss();

    if (! SharedPrefs.getInstance().isConnected()) {
      showDialog(DIALOG_NOT_CONNECTED);
    }
    else {
      // retrieve location from other device
      if (shouldGetLocation) {
        new GetLocationTask().execute();
        registerReceiver(mUpdateOtherReceiver, new IntentFilter(BROADCAST_OTHER_LOCATION));
        shouldShowConnectingDialog = true;
        shouldGetLocation = false;
      }
      if (shouldShowConnectingDialog) {
        if (hasInternet()) {
          if (mainDialog != null) mainDialog.dismiss();
          connectingDialog = ProgressDialog.show(this, "", 
              getString(R.string.dialog_connecting), true, true, 
              null);
          connectingDialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
              if (me == null || other == null) {
                arrow.setVisibility(View.GONE);
              }
            }
          });
        }
        else {
          showDialog(DIALOG_NO_INTERNET);
        }
      }
    }
  
    
    sensorListener = new SensorEventListener() {
      public void onAccuracyChanged(Sensor sensor, int accuracy) { }
      
      public void onSensorChanged(SensorEvent event) {
        float[] sensorValues = event.values;
        MainActivity.this.update(null, sensorValues[0]);
      }
    };
    sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
    sensorManager.registerListener(sensorListener, sensorManager.
        getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.
        SENSOR_DELAY_NORMAL); 
    MyDeviceLocation.getInstance().addObserver(this);
    registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
  }
  
  /**
   * Called when the activity is paused. Stops all the updates.
   */
  @Override
  public void onPause() {
    super.onPause();
    sensorListener = null;
    sensorManager.unregisterListener(sensorListener);
    MyDeviceLocation.getInstance().deleteObserver(this);
    if (arrowThread != null) {
      arrowThread = null;
    }
    unregisterReceiver(connectivityReceiver);
    if (connectingDialog != null) {
      connectingDialog.dismiss();
      connectingDialog = null;
      shouldShowConnectingDialog = true;
    }
    else {
      shouldShowConnectingDialog = false;
    }
  }
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    // Try to let the other device know you stopped. Success not guaranteed!
    new Thread(new Runnable() {
      public void run() {
        try {
          WebAPI.deleteLocation();
        }
        catch (WebApiException e) {
          Log.w(TAG, "Could not send delete location request: " + e.getMessage());
        }
      }
    }).start();

    try {
      unregisterReceiver(mUpdateOtherReceiver);
    }
    catch (Exception e) {
      // it seems sometimes unregistering isn't working,
      // we don't want the program to break in response
      Log.w(TAG, "Could not unregister update other broadcast receiver");
    }
    
    if (largePhoto != null) largePhoto.recycle();
    largePhoto = null;
    arrow.destroy();
    Runtime.getRuntime().gc();
  }

  private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver () {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (! hasInternet() && connectingDialog != null) {
        showDialog(DIALOG_NO_INTERNET);
      }
    }
  };
  
  /**
   * Gets called the first time the user opens the menu
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }
  
  /**
   * Gets called with the item the user selected.
   * User can reset the app from here, or choose a different
   * wallpaper image.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent i;
    switch (item.getItemId()) {
    case R.id.reset:
      startActivityForResult(new Intent(this, ResetConfirmationActivity.class), RESET);
      return true;
    case R.id.pick_arrow:
      i = new Intent(this, PickArrowActivity.class);
      startActivityForResult(i, PICK_ARROW);
      return true;
    case R.id.wallpaper:
      Display display = getWindowManager().getDefaultDisplay(); 
      int width = display.getWidth();
      int height = display.getHeight();
      try {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT, null)
            .setType("image/*")
            .putExtra("crop", "true")
            .putExtra("outputX", width)
            .putExtra("outputY", height)
            .putExtra("aspectX", width)
            .putExtra("aspectY", height)
            .putExtra("scale", true)
            .putExtra("return-data", false)
            .putExtra(MediaStore.EXTRA_OUTPUT, getWallpaperUri())
            , PHOTO_PICKED);
      }
      catch (ActivityNotFoundException e) {
        showDialog(DIALOG_ACTIVITY_NOT_FOUND);
      }
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private Dialog mainDialog;
  @Override
  protected Dialog onCreateDialog(int id, Bundle b) {
    switch(id) {
    case DIALOG_NO_INTERNET:
      mainDialog =  new AlertDialog.Builder(this)
          .setMessage(R.string.dialog_no_internet)
          .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              finish();
            }
          })
          .setNeutralButton(R.string.settings, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
              finish();
            }
          })
          .create();
      return mainDialog;
    case DIALOG_NOT_CONNECTED:
      mainDialog =  new AlertDialog.Builder(this)
          .setMessage(R.string.dialog_not_connected)
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // restart app
              startActivity(new Intent(MainActivity.this, VirginActivity.class));
              finish();
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
              // restart app
              startActivity(new Intent(MainActivity.this, VirginActivity.class));
              finish();
            }
          })
          .create();
      return mainDialog;
    case DIALOG_API_EXCEPTION:
      mainDialog = new AlertDialog.Builder(this)
          .setNeutralButton(R.string.retry, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              new GetLocationTask().execute();
            }
          })
          .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              finish();
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
              finish();
            }
          })
          .create();
      return mainDialog;
    }
    return super.onCreateDialog(id);
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent data) {
    Log.v(TAG, "Received activity result");
    if (requestCode == PHOTO_PICKED && resultCode == RESULT_OK && data != null) {
      Log.d(TAG, "Got the wallpaper");
      Log.d(TAG, "Environment.getExternalStorageDirectory() = " 
          + Environment.getExternalStorageDirectory());
      reloadBackground();
    }
    else if (requestCode == RESET && resultCode == 1) {
      Log.v(TAG, "Closing Main Activity");
      finish();
    }
    else if (requestCode == PICK_ARROW && data != null) {
      Log.d(TAG, "Got a new arrow");
      arrow.setArrowDrawable(data.getIntExtra("arrow", -1), this);
    }
  }
  
  private BroadcastReceiver mUpdateOtherReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String locString = intent.getExtras().getString("location");
      Log.d(TAG, "New location of other is " + locString);
      try {
        String[] locStrings = locString.split(":");
        Log.d(TAG, "lat: " + locStrings[0] + ", lon: " + locStrings[1]);
        other = new Location("");
        other.setLatitude(Double.parseDouble(locStrings[0]));
        other.setLongitude(Double.parseDouble(locStrings[1]));
        updateUI();
        //handler.post(updateUI);
      }
      catch (Exception e) {
        Log.e(TAG, "Could not parse others location: " + e.getMessage());
      }
    }
  };

  /**
   * Called when one of the observations changes (this device's location,
   * other device's location, phone orientation). Recalculates the
   * arrow's rotation.
   */
  public void update(Observable source, Object argument) {
    if (source == null) {
      // Sensor value passed
      north = (Float) argument;
      updateUI();
    }
    else if (source instanceof MyDeviceLocation) {
      if (isBetterLocation((Location) argument, me)) {
        me = (Location) argument;
        updateUI();
      }
    }
  }
  
  public void updateUI() {
    if (me != null && other != null) {
      if (connectingDialog != null) {
        connectingDialog.dismiss();
        connectingDialog = null;
        shouldShowConnectingDialog = false;
      }
      if (arrowThread == null) {
        arrowThread = new Thread(arrow);
        arrowThread.start();
      }
      // TODO: Uncomment
      /*
      if (me.distanceTo(other) < 10) {
        Log.d(TAG, "We are together!");
        arrow.setVisibility(View.GONE);
        together.setVisibility(View.VISIBLE);
      }
      */
      
        //Log.d(TAG, "We are not together");
        arrow.setVisibility(View.VISIBLE);
        //together.setVisibility(View.GONE);
        float rotation = me.bearingTo(other) - north;
        arrow.setRotation(rotation);
      
    }
  }

  
  private static final int TWO_MINUTES = 1000 * 60 * 2;
  
  /** Determines whether one Location reading is better than the current Location fix
    * @param location  The new Location that you want to evaluate
    * @param currentBestLocation  The current Location fix, to which you want to compare the new one
    */
  protected boolean isBetterLocation(Location location, Location currentBestLocation) {
      if (currentBestLocation == null) {
          // A new location is always better than no location
          return true;
      }
  
      // Check whether the new location fix is newer or older
      long timeDelta = location.getTime() - currentBestLocation.getTime();
      boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
      boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
      boolean isNewer = timeDelta > 0;
  
      // If it's been more than two minutes since the current location, use the new location
      // because the user has likely moved
      if (isSignificantlyNewer) {
          return true;
      // If the new location is more than two minutes older, it must be worse
      } else if (isSignificantlyOlder) {
          return false;
      }
  
      // Check whether the new location fix is more or less accurate
      int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
      boolean isLessAccurate = accuracyDelta > 0;
      boolean isMoreAccurate = accuracyDelta < 0;
      boolean isSignificantlyLessAccurate = accuracyDelta > 200;
  
      // Check if the old and new location are from the same provider
      boolean isFromSameProvider = isSameProvider(location.getProvider(),
              currentBestLocation.getProvider());
  
      // Determine location quality using a combination of timeliness and accuracy
      if (isMoreAccurate) {
          return true;
      } else if (isNewer && !isLessAccurate) {
          return true;
      } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
          return true;
      }
      return false;
  }
  
  /** Checks whether two providers are the same */
  private boolean isSameProvider(String provider1, String provider2) {
      if (provider1 == null) {
        return provider2 == null;
      }
      return provider1.equals(provider2);
  }     
  
  private Uri getWallpaperUri() {
    String state = Environment.getExternalStorageState();
    if (Environment.MEDIA_MOUNTED.equals(state)) {
      // Storage is readable & writable
      new File(Environment.getExternalStorageDirectory() +
          DIRECTORY).mkdirs();
      File f = new File(Environment.getExternalStorageDirectory(), 
          DIRECTORY + WALLPAPER_FILE);
      return Uri.fromFile(f);
    }
    else {
      // TODO: Handle media not readable/writable
      return null;
    }
  }
  
  private boolean loadBackground() {
    try {
      if (largePhoto == null) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        largePhoto = (Bitmap) BitmapFactory.decodeStream(
            new FileInputStream(new File(Environment.getExternalStorageDirectory(),
            DIRECTORY + WALLPAPER_FILE)), null, options);
      }
      BitmapDrawable bd = new BitmapDrawable(largePhoto);
      bd.setCallback(null);
      findViewById(R.id.main).setBackgroundDrawable(bd);
      return true;
    }
    catch (FileNotFoundException e) {
      // No wallpaper chosen; use default
      return false;
    }
  }
  
  private boolean reloadBackground() {
    if (largePhoto != null) largePhoto.recycle();
    largePhoto = null;
    return loadBackground();
  }
}
