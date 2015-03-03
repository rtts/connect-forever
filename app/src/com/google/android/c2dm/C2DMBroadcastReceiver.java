/*
 */
package com.google.android.c2dm;

//import com.connectforever.BaseActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * Helper class to handle BroadcastReciver behavior.
 * - can only run for a limited amount of time - it must start a real service 
 * for longer activity
 * - must get the power lock, must make sure it's released when all done.
 * 
 */
public class C2DMBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "C2DMBroadcastReceiver";
    
    @Override
    public final void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received Broadcast..");
        // TEST: uncomment to see status bar notification on receive c2dm push message.
        //context.sendOrderedBroadcast(new Intent(BaseActivity.BROADCAST_TEST), null);
        // To keep things in one place.
        C2DMBaseReceiver.runIntentInService(context, intent);
        setResult(Activity.RESULT_OK, null /* data */, null /* extra */);        
    }
}
