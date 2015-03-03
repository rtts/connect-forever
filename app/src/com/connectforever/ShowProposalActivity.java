package com.connectforever;

import android.os.Bundle;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.app.Dialog;

public class ShowProposalActivity extends BaseActivity {
  public static final String TAG = "ShowProposalActivity";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.proposal_pending);
    
    ((TextView) findViewById(R.id.proposal_pending_key)).setText(
        SharedPrefs.getInstance().getProposalCode());
  }
  
  @Override
  void onApiTaskFinished(Type t, Object o) {
    switch(t) {
    case DELETE_PROPOSAL:
      Boolean succeeded = (Boolean) o;
      if (succeeded) {
        showDialog(DIALOG_PROPOSAL_WITHDRAWN);
      }
      break;
    case FORCE_CHECK:
      Boolean connected = (Boolean) o;
      if (connected) {
        SharedPrefs.getInstance().setConnected(true);
        showDialog(DIALOG_CONNECTED_BY_CHECKING);
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.show_proposal_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
    case R.id.menu_force_check:
      new ApiTask(Type.FORCE_CHECK).execute();
      return true;
    case R.id.menu_withdraw:
      new ApiTask(Type.DELETE_PROPOSAL).execute();
      return true;
    }
    return false;
  }

  @Override
  protected Dialog onCreateDialog(int id, Bundle b) {
    switch(id) {
    case DIALOG_CONNECTED_BY_CHECKING:
      SharedPrefs.getInstance().setIsFreshConnect(false);
      return getBuilder().setMessage(getString(R.string.dialog_connected_by_checking))
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              Intent i = new Intent(ShowProposalActivity.this, VirginActivity.class);
              i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            }
          }).create();
    case DIALOG_PROPOSAL_WITHDRAWN:
      return getBuilder().setMessage(getString(R.string.alert_proposal_deleted))
          .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              Intent i = new Intent(ShowProposalActivity.this, VirginActivity.class);
              i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
              Intent i = new Intent(ShowProposalActivity.this, VirginActivity.class);
              i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(i);
            }
          }).create();
    }
    return super.onCreateDialog(id, b);
  }
}
