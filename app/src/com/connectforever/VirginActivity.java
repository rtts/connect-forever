package com.connectforever;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.android.c2dm.C2DMessaging;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

public class VirginActivity extends BaseActivity {
  private static final String TAG = "VirginActivity";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    
    // Put the background image on the sd card so the user can choose it
    if (SharedPrefs.getInstance().isFirstRun()) {
      if(saveBackground()) {
        SharedPrefs.getInstance().setFirstRun(false);
      }
    }

    if (! SharedPrefs.getInstance().isFreshConnect() &&
        SharedPrefs.getInstance().isConnected()) {
      startActivity(new Intent(this, MainActivity.class)
          .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
      this.finish();
    }
    
    setContentView(R.layout.virgin);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    if (SharedPrefs.getInstance().isFreshConnect()) {
      showDialog(DIALOG_FRESH_CONNECT);
    }
  }

  /**
   * Requests proposal from web api and shows it in an activity.
   * If registration with c2dm or web api is necessary, this is handled first,
   * and the set proposal request is done afterwards.
   */
  public void clickPropose(View v) {
    String id = C2DMessaging.getRegistrationId(this);
    
    if (id == null || "".equals(id)) {
      registerC2DM();
    }
    else if (SharedPrefs.getInstance().shouldSendId()) {
      Log.d(TAG, "WebAPI doesnt know c2dm id, sending it now..");
      new ApiTask(Type.REGISTER_OR_UPDATE_ID).execute();
    }
    else {
      Log.v(TAG, "Trying to get proposal key");
      ApiTask t = new ApiTask(Type.GET_PROPOSAL);
      t.execute();
    }
  }
  
  /** 
   * Opens activity where user can enter proposal code.
   */
  public void clickEnterCode(View v) {
    Log.v(TAG, "Starting Enter Proposal Activity");
    Intent i = new Intent(VirginActivity.this, EnterProposalActivity.class);
    startActivity(i);
  }
  
  @Override
  public Dialog onCreateDialog(int id, Bundle b) {
    switch (id) {
    case DIALOG_FRESH_CONNECT:
      SharedPrefs.getInstance().setIsFreshConnect(false);
      return getBuilder().setMessage(getString(R.string.alert_connected))
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              startActivity(new Intent(VirginActivity.this, MainActivity.class)
                  .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
              finish();
            }
          }).create();
    }
    return super.onCreateDialog(id, b);
  }
  
  @Override
  void onApiTaskFinished(Type t, Object o) {
    switch(t) {
    case REGISTER_OR_UPDATE_ID:
      Log.v(TAG, "Trying to get proposal key");
      new ApiTask(Type.GET_PROPOSAL).execute();
      break;
    case GET_PROPOSAL:
      Intent i = new Intent(this, ShowProposalActivity.class);
      startActivity(i);
      break;
    }
  }
  
  private boolean saveBackground() {
    // abracadabra, from getExternalFilesDir documentation
    boolean success = false;
    File path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File file = new File(path, "connect_forever_wallpaper.jpg");

    try {
        InputStream is = getResources().openRawResource(R.drawable.background);
        OutputStream os = new FileOutputStream(file);
        byte[] data = new byte[is.available()];
        is.read(data);
        os.write(data);
        is.close();
        os.close();
        
        success = true;

        MediaScannerConnection.scanFile(this,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
            }
        });
    } catch (IOException e) {
        Log.w("ExternalStorage", "Error writing " + file, e);
    }
    return success;
  }
}
