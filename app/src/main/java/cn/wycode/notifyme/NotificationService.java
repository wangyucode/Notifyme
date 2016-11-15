package cn.wycode.notifyme;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by wWX383516 on 2016/11/14.
 */
public class NotificationService extends NotificationListenerService{

    private static final String TAG = "NotificationService";

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    private NotificationListener mListener;

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
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG,"onNotificationPosted->"+sbn);
        if(mListener!=null){
            mListener.onNotificationPosted(sbn);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        NotificationService getService() {
            return NotificationService.this;
        }
    }

    public void setNotificationListener(NotificationListener listener){
        this.mListener = listener;
    }

    public interface NotificationListener{
        void onNotificationPosted(StatusBarNotification sbn);
    }
}