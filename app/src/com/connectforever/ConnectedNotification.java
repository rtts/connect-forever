package com.connectforever;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectedNotification extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    NotificationManager nm = (NotificationManager) 
        context.getSystemService(Context.NOTIFICATION_SERVICE);
    int icon = R.drawable.status_icon;
    CharSequence tickerText = context.getString(R.string.notification_ticker);
    long when = System.currentTimeMillis();
    
    Notification notification = new Notification(icon, tickerText, when);
    notification.ledARGB = 0xFFAACC;
    notification.ledOnMS = 100;
    notification.ledOffMS = 100;
    notification.flags = Notification.FLAG_AUTO_CANCEL;
    Context cxt = context.getApplicationContext();
    CharSequence contentTitle = context.getString(R.string.notification_title);
    CharSequence contentText = context.getString(R.string.notification_text);
    Intent notificationIntent = new Intent(context, VirginActivity.class);
    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notificationIntent.putExtra("fresh_connect", true);
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
    
    notification.setLatestEventInfo(cxt, contentTitle, contentText, contentIntent);
    
    nm.notify(1, notification);
  }
  

}
