package cn.wycode.notifyme;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * https://github.com/yihongyuelan/NotificationListenerServiceDemo
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    private TextView tvHint;
    private Button btnConnect;
    private TextView tvWycode;
    private TextView tvVersion;
    private ScrollView svContent;

    private boolean mIsBound;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    public static final String ACTION_NOTIFICATION_BROADCAST = "cn.wycode.ACTION_NOTIFICATION_BROADCAST";
    public static final String EXTRA_KEY_NOTIFICATION = "EXTRA_KEY_NOTIFICATION";
    private boolean isEnabledNLS = false;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver notifyReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        bindListener();
        setView();

        broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter notifyFilter = new IntentFilter(ACTION_NOTIFICATION_BROADCAST);
        notifyReceiver = new NotificationBroadcastReceiver();
        broadcastManager.registerReceiver(notifyReceiver, notifyFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isEnabledNLS = isEnabled();
        Log.d(TAG, "isEnabledNLS = " + isEnabledNLS);
        if (!isEnabledNLS) {
            showConfirmDialog();
        }
    }

    private boolean isEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void setView() {
        String versionName = "unknow";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        tvVersion.setText(getString(R.string.version_text, versionName));
    }

    private void findViews() {
        tvHint = (TextView) findViewById(R.id.tv_content);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        tvWycode = (TextView) findViewById(R.id.tv_wycode);
        tvVersion = (TextView) findViewById(R.id.tv_version);
        svContent= (ScrollView) findViewById(R.id.sv_content);
    }

    private void bindListener() {
        btnConnect.setOnClickListener(this);
        tvWycode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                tvHint.append("\n正在启动服务...");
                doBindService();
                break;
            case R.id.tv_wycode:
                Intent wyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://wycode.cn"));
                startActivity(wyIntent);
                break;
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            tvHint.append("\n服务已启动");
        }

        public void onServiceDisconnected(ComponentName className) {
            tvHint.append("\n服务已断开");
        }
    };

    void doBindService() {
        bindService(new Intent(this,
                NotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        broadcastManager.unregisterReceiver(notifyReceiver);
    }

    private void openNotificationAccess() {
        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("请打开通知访问权限")
                .setTitle("通知访问")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                openNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }

    class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            StatusBarNotification sbn = intent.getParcelableExtra(EXTRA_KEY_NOTIFICATION);
            Notification notification = sbn.getNotification();

            Log.d(TAG,"Notification received:" + notification.extras);
            String appName = "unknow";
            try {
                ApplicationInfo appInfo = getPackageManager().getPackageInfo(sbn.getPackageName(),0).applicationInfo;
                appName = getPackageManager().getApplicationLabel(appInfo).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String when = new SimpleDateFormat("MM月dd日 mm分ss秒").format(new Date(sbn.getPostTime()));
            tvHint.append("\n收到通知来自："+appName);
            tvHint.append("\n时间："+ when);
            tvHint.append("\n标题："+ notification.extras.get(Notification.EXTRA_TITLE));
            tvHint.append("\n内容："+ notification.extras.get(Notification.EXTRA_TEXT));
            svContent.post(new Runnable() {
                public void run() {
                    svContent.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

}