package com.connectforever;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import com.google.android.c2dm.C2DMBaseReceiver;

/**
 * Receives push notifications and redirects them to the right class.
 */
public class C2DMReceiver extends C2DMBaseReceiver {
  public static final String TAG = "C2DMReceiver";
  public static final String KEY_CONNECTION_SUCCEEDED = "connection_succeeded";
  public static final String KEY_OTHER_ACTIVE = "please_send_location";
  public static final String KEY_OTHERS_LOCATION = "others_location";
  
  public C2DMReceiver() {
    super([REDACTED]);
    Log.d(TAG, "New C2DMReceiver constructed");
  }
  
  /* Message from the server */
  @Override
  protected void onMessage(Context context, Intent intent) {
    Log.d(TAG, "Got a message that has collapse_key: " + intent.getExtras().getString("collapse_key"));
    String collapse_key = intent.getStringExtra("collapse_key");
    if (KEY_CONNECTION_SUCCEEDED.equals(collapse_key)) {
      // Other has entered your proposal key, you are now connected.
      Log.d(TAG, "Setting connected state to true");
      SharedPrefs.getInstance().setConnected(true);
      // Let the user know connection succeeded
      Intent notificationIntent = new Intent(BaseActivity.BROADCAST_CONNECTED);
      Log.d(TAG, "Sending ordered broadcast");
      context.sendOrderedBroadcast(notificationIntent, null);
    }
    else if (KEY_OTHER_ACTIVE.equals(collapse_key)) {
      if (! "False".equals(intent.getStringExtra("send"))) {
        SingleLocationRequester.getInstance().sendLocation();
      }
      else {
        SingleLocationRequester.getInstance().stopSending();
      }
    }
    else if (KEY_OTHERS_LOCATION.equals(collapse_key)) {
      Intent i = new Intent(BaseActivity.BROADCAST_OTHER_LOCATION);
      i.putExtras(intent.getExtras());
      context.sendBroadcast(i);
    }
  }
  
  /* Error when in service mode (no UI!) */
  @Override
  public void onError(Context context, String errorId) {
    Log.d(TAG, "Got an error that has id: " + errorId);
    Intent i = new Intent(BaseActivity.BROADCAST_UI_ACTION);
    i.putExtra("error", errorId);
    context.sendBroadcast(i);
  }
  
  @Override
  public void onRegistrered(Context context, String registrationId) throws IOException {
    Log.d(TAG, "Registrered and got id: " + registrationId);
    SharedPrefs.getInstance().setShouldSendId(true);
    Intent i = new Intent(BaseActivity.BROADCAST_UI_ACTION);
    i.putExtra("registration_id", registrationId);
    context.sendBroadcast(i);
  }
}
