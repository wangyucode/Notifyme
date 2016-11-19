package cn.wycode.notifyme;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by wWX383516 on 2016/11/14.
 */
public class NotificationService extends NotificationListenerService{

    private static final String TAG = "NotificationService";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
    }

    @Override
    public void onListenerConnected() {
        Log.d(TAG,"onListenerConnected");
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG,"onNotificationPosted->"+sbn);
        Intent notifyIntent = new Intent(MainActivity.ACTION_NOTIFICATION_BROADCAST);
        notifyIntent.putExtra(MainActivity.EXTRA_KEY_NOTIFICATION,sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(notifyIntent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}