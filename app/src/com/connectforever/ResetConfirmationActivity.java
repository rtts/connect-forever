package com.connectforever;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

/**
 * Activity that has the potential to destroy the device's connection.
 * This only happens after 2 confirmative clicks.
 * Resetting the connection is done by resetting the device's UUID.
 */
public class ResetConfirmationActivity extends BaseActivity {
  private static final String TAG = "ResetConfirmation";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    setContentView(R.layout.reset_confirmation);
    
    Log.v(TAG, "Started reset confirmation activity");
    
    // Button 1 shows are-you-sure dialog
    Button button1 = (Button) findViewById(R.id.reset_confirmation_button1);
    button1.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Log.v(TAG, "Button 1 clicked");
        showDialog(DIALOG_RESET_CONFIRMATION);
      }
    });
    
    // Button 2 closes this activity, returns to previous
    Button button2 = (Button) findViewById(R.id.reset_confirmation_button2);
    button2.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        setResult(0);
        finish();
      }
    });
  }
  
  @Override
  public void onResume() {
    super.onResume();
    Log.v(TAG, "onResume");
  }
  
  @Override
  void onApiTaskFinished(Type t, Object o) {
    switch(t) {
    case RESET:
      Boolean succeeded = (Boolean) o;
      if (succeeded) {
        Log.d(TAG, "Reset succeeded, starting Virgin Activity...");
        setResult(1);
        Intent i = new Intent(this, VirginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
      }
    }
  }
  
  @Override
  protected Dialog onCreateDialog(int id) {
    switch(id) {
    case DIALOG_RESET_CONFIRMATION:
      // Shows an are-you-sure dialog. If user responds positively, the
      // connection is destroyed, otherwise it is maintained.
      return new AlertDialog.Builder(this)
          .setMessage(R.string.reset_confirmation_popup_text)
          .setTitle(R.string.reset_confirmation_popup_title)
          .setPositiveButton(R.string.reset_confirmation_popup_ok,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  Intent unregIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
                  unregIntent.putExtra("app", PendingIntent.getBroadcast(
                      ResetConfirmationActivity.this, 0, new Intent(), 0));
                  startService(unregIntent);
                  new ApiTask(Type.RESET).execute();
                }
              })
          .setNegativeButton(R.string.reset_confirmation_popup_cancel,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                }
              }).create();
    }
    return super.onCreateDialog(id);
  }
}
