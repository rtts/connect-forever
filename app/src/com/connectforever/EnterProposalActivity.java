package com.connectforever;

import com.google.android.c2dm.C2DMessaging;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class EnterProposalActivity extends BaseActivity {
  protected EditText editText1;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.enter_proposalkey);
    
    editText1 = (EditText) findViewById(R.id.enter_proposalkey_edit_text);
    editText1.setText(SharedPrefs.getInstance().getEnteredKey());
    
    // Make sure user can press "enter" or "Done" and this removes the soft keyboard
    editText1.setOnEditorActionListener(new OnEditorActionListener() {
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE ||
            (event.getAction() == KeyEvent.ACTION_DOWN &&
            event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            InputMethodManager imm = (InputMethodManager)v.getContext().
                getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;  
        }
        return false;
      }
    });
  }
  
  @Override
  public void onPause() {
    super.onPause();
    // Remember the code for the next time.
    if (! SharedPrefs.getInstance().isConnected()) {
      SharedPrefs.getInstance().setEnteredKey(editText1.getText().toString());
    }
  }
  
  @Override
  protected Dialog onCreateDialog(int id, Bundle b) {
    switch(id) {
    case DIALOG_NO_KEY:
      return getBuilder().setMessage(getString(R.string.dialog_no_key)).create();
    case DIALOG_DUPLICATE_KEY:
      return getBuilder().setMessage(R.string.duplicate_key_popup_message).create();
    case DIALOG_KEY_NOT_FOUND:
      return getBuilder().setMessage(getString(R.string.error_key_not_found)).create();
    case DIALOG_CONNECTED_BY_ENTERING_CODE:
      SharedPrefs.getInstance().setIsFreshConnect(false);
      return getBuilder().setMessage(getString(R.string.dialog_connected_by_entering_code))
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // restart app
              Intent i = new Intent(EnterProposalActivity.this, VirginActivity.class);
              i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            }
          }).create();
    }
    return super.onCreateDialog(id);
  }
  
  /**
   * Connects to the device with the entered proposal code.
   * If there is no entered code, user is told the other's code should be entered.
   * If the user enters his own code, he is told he should enter the other's code.
   */
  public void clickConnect(View v) {
    String key = EnterProposalActivity.this.editText1.getText().toString();
    
    if (key.length() == 0) {
      showDialog(DIALOG_NO_KEY);
    }
    else if (key.equals(SharedPrefs.getInstance().getProposalCode())) {
      showDialog(DIALOG_DUPLICATE_KEY);
    }
    else {
      setProposal(key);
    }
  }
  
  /**
   * Sends the entered code to the API.
   * If the device should register with c2dm or web API, this is handled first,
   * and the entered code is sent in the onApiTaskFinished method.
   */
  private void setProposal(String key) {
    String id = C2DMessaging.getRegistrationId(this);
    if (id == null || "".equals(id)) {
      registerC2DM();
    }
    else if (SharedPrefs.getInstance().shouldSendId()) {
      Log.d(TAG, "WebAPI doesnt know c2dm id, sending it now..");
      new ApiTask(Type.REGISTER_OR_UPDATE_ID).execute();
    }
    else {
      new ApiTask(Type.SET_PROPOSAL, key).execute();
    }
  }

  @Override
  void onApiTaskFinished(Type t, Object o) {
    switch(t) {
    case REGISTER_OR_UPDATE_ID:
      new ApiTask(Type.SET_PROPOSAL, editText1.getText().toString()).execute();
      break;
    case SET_PROPOSAL:
      Boolean succeeded = (Boolean) o;
      if (succeeded) {
        showDialog(DIALOG_CONNECTED_BY_ENTERING_CODE);
      }
      else {
        showDialog(DIALOG_KEY_NOT_FOUND);
      }
    }
  }
}
