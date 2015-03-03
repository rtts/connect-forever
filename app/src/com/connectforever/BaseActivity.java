package com.connectforever;

import com.google.android.c2dm.C2DMessaging;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Parent class for all Activities. Handles web API communication and exceptions.
 * Has the general list of Dialog ID's.
 */
public class BaseActivity extends Activity {
  static final String TAG = "BaseActivity";

  
  /** Key used for passing the dialog message text to the onpreparedialog method. */
  static final String KEY_DIALOG_MESSAGE = "message";
  
  /** Constant for dialog type: no internet connection available. */
  static final int DIALOG_NO_INTERNET           = 0;
  /** Constant for dialog type: An exception while contacting the API occurred. */
  static final int DIALOG_API_EXCEPTION         = 1;
  /** Constant for dialog type: The two devices are not connected. */
  static final int DIALOG_NOT_CONNECTED         = 2;
  /** Constant for dialog type: The proposal on this device is withdrawn. */
  static final int DIALOG_PROPOSAL_WITHDRAWN    = 3;
  /** Constant for dialog type: Confirm resetting the app. */
  static final int DIALOG_RESET_CONFIRMATION    = 4;
  /** Constant for dialog type: The (photo crop) activity cannot be found. */
  static final int DIALOG_ACTIVITY_NOT_FOUND    = 5;
  /** Constant for dialog type: Registering for C2DM failed. */
  static final int DIALOG_REGISTRATION_FAILED   = 6;
  /** Constant for dialog type: Other device reset the app. */
  static final int DIALOG_OTHER_RESET           = 7;
  /** Constant for dialog type: Entered key is the same as the requested proposal code. */
  static final int DIALOG_DUPLICATE_KEY         = 8;
  /** Constant for dialog type: Entered key could not be found on the api. */
  static final int DIALOG_KEY_NOT_FOUND         = 9;
  /** Constant for dialog type: Someone connected to you. */
  static final int DIALOG_FRESH_CONNECT         = 10;
  /** Constant for dialog type: The two devices have been connected. */
  static final int DIALOG_CONNECTED_BY_ENTERING_CODE = 11;
  /** Constant for dialog type: User did not enter a proposal code. */
  static final int DIALOG_NO_KEY                = 12;
  /** Constant for dialog type: User force checked connection status, which says devices are connected. */
  static final int DIALOG_CONNECTED_BY_CHECKING = 13;

  public static final String BROADCAST_UI_ACTION      = "com.connectforever.UPDATE_UI";
  public static final String BROADCAST_OTHER_LOCATION = "com.connectforever.UPDATE_OTHER_LOCATION";
  public static final String BROADCAST_CONNECTED      = "com.connectforever.UPDATE_CONNECTED";
  public static final String BROADCAST_TEST           = "com.connectforever.UPDATE_TEST";
  
  @Override
  public void onResume() {
    super.onResume();
    // If you got connected, restart app.
    if ((this instanceof EnterProposalActivity ||
         this instanceof ShowProposalActivity )
        && SharedPrefs.getInstance().isConnected()) {
      Intent i = new Intent(this, VirginActivity.class);
      i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(i);
    }
    registerReceiver(registrationFinishedReceiver, new IntentFilter(BROADCAST_UI_ACTION));
    // Priority of connected receiver > 0, so it is called before notification-maker
    IntentFilter filter = new IntentFilter(BROADCAST_CONNECTED);
    filter.setPriority(1);
    registerReceiver(connectionSucceededReceiver, filter);
  }
  
  @Override
  public void onPause() {
    super.onPause();
    // TODO: check if this is absolutely necessary
    if (dialog != null) dialog.dismiss();
    unregisterReceiver(registrationFinishedReceiver);
    unregisterReceiver(connectionSucceededReceiver);
  }
  
  /**
   * Convenience method. Puts the messages in the Exception in a Bundle, which
   * is then passed to the showDialog method.
   */
  void showExceptionDialog(WebApiException e) {
    Bundle b = new Bundle();
    b.putString(KEY_DIALOG_MESSAGE, getString(e.getMessageStringId()));
    showDialog(DIALOG_API_EXCEPTION, b);
  }
  
  @Override
  protected Dialog onCreateDialog(int id, Bundle b) {
    // General information dialogs
    Builder builder = getBuilder();
    switch (id) {
    case DIALOG_API_EXCEPTION:
      break;
    case DIALOG_NOT_CONNECTED:
      builder.setMessage(getString(R.string.alert_not_connected));
      break;
    case DIALOG_ACTIVITY_NOT_FOUND:
      builder.setMessage(getString(R.string.error_activity_not_found));
      break;
    case DIALOG_REGISTRATION_FAILED:
      builder.setMessage(getString(R.string.error_registration_failed));
      break;
    default:
      return super.onCreateDialog(id, b);
    }
    return builder.create();
  }
  
  Builder getBuilder() {
    return new Builder(this)
        .setNeutralButton(R.string.ok, new OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        })
        .setMessage("");
  }
 
  /** 
   * Message of the general api exception dialog can change, setting the
   * right message is done here.
   */
  @Override
  protected void onPrepareDialog(int id, Dialog d, Bundle b) {
    switch (id) {
    case DIALOG_API_EXCEPTION:
      String message = b.getString(KEY_DIALOG_MESSAGE);
      if (message == null) {
        message = getString(R.string.error_unknown);
      }
      ((AlertDialog) d).setMessage(message);
      break;
    }
  }
  
  //-----------------------------------------------------------------//
  
  /**
   * Type of the API task that should be performed.
   */
  enum Type {
    GET_PROPOSAL,
    SET_PROPOSAL,
    DELETE_PROPOSAL,
    HAS_PROPOSAL,
    GET_LOCATION,
    SET_LOCATION,
    REGISTER_OR_UPDATE_ID,
    RESET,
    FORCE_CHECK
  }
  
  protected ProgressDialog dialog;
  
  boolean hasInternet() {
    ConnectivityManager connectivityManager 
        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null;
  }
  
  void registerC2DM() {
    Log.d(TAG, "No existing Registration ID. Registering..");
    if (hasInternet()) {
      dialog = ProgressDialog.show(this, "",
          getString(R.string.registering), true);
      C2DMessaging.register(this, [REDACTED]);
    }
    else {
      showExceptionDialog(new WebApiException(WebApiException.REASON_NO_INTERNET));
    }
  }
  
  /**
   * Updates the UI by closing progress dialogs and possibly showing error dialogs.
   * If a String with key "error" is included in the Intent, this is showed in an error dialog.
   */
  private final BroadcastReceiver registrationFinishedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String error = intent.getStringExtra("error");
      if ("ACCOUNT_MISSING".equals(error)) {
        context.startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT).
            putExtra(Settings.EXTRA_AUTHORITIES, new String[] {"gmail-ls"}));
      }
      if (dialog != null) dialog.dismiss();
      String regid = intent.getStringExtra("registration_id");
      if (regid != null) {
        // c2dm registration succeeded, now send c2dm id to web api
        new ApiTask(Type.REGISTER_OR_UPDATE_ID).execute();
      }
      else {
        showDialog(DIALOG_REGISTRATION_FAILED);
      }
    }
  };
  
  private final BroadcastReceiver connectionSucceededReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d(TAG, "Received now connected broadcast");
      Intent i = new Intent(BaseActivity.this, VirginActivity.class);
      i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      context.startActivity(i);
      abortBroadcast();
    }
  };

  class ApiTask extends AsyncTask<Void, Object, Object> {
    WebApiException e;
    Type type;
    Object input;
    
    public ApiTask(Type type) {
      this.type = type;
    }
    
    public ApiTask(Type type, Object input) {
      this.type = type;
      this.input = input;
    }
    
    protected void onPreExecute() {
      dialog = ProgressDialog.show(BaseActivity.this, "",
          getString(R.string.loading), true);
    }
    
    protected Object doInBackground(Void... voids) {
      try {
        Object returnObject = null;
        switch (type) {
        case FORCE_CHECK:
          try {
            WebAPI.getLocation();
            returnObject = true;
          }
          catch (WebApiException e) {
            if (e.getReason() == WebApiException.REASON_NOT_CONNECTED) {
              returnObject = false;
            }
            else {
              throw e;
            }
          }
        case GET_LOCATION:
          WebAPI.getLocation();
          break;
        case GET_PROPOSAL:
          returnObject = WebAPI.getProposal();
          break;
        case SET_PROPOSAL:
          returnObject = WebAPI.setProposal((String) input);
          break;
        case DELETE_PROPOSAL:
          returnObject = WebAPI.deleteProposal();
          break;
        case REGISTER_OR_UPDATE_ID:
          WebAPI.registerOrUpdateID();
          break;
        case RESET:
          returnObject = WebAPI.reset();
          break;
        }
        return returnObject;
      }
      catch (WebApiException e) {
        this.e = e;
        return null;
      }
    }
    
    protected void onPostExecute(Object o) {
      Log.d(TAG, "Dismissing progress dialog");
      dialog.dismiss();
      if(e != null) {
        setResult(0);
        Log.d(TAG, "Showing exception");
        showExceptionDialog(e);
      }
      else {
        onApiTaskFinished(type, o);
      }
    }
  }
  
  /** 
   * Called when the ApiTask finished executing and no errors occurred.
   * Override this method if you want to perform an action when apiTask is finished.
   * @param t The type of api task that was performed.
   * @param o The result of the Background Api Task.
   */
  void onApiTaskFinished(Type t, Object o) { };
}
