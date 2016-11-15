package cn.wycode.notifyme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * https://github.com/yihongyuelan/NotificationListenerServiceDemo
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, NotificationService.NotificationListener {

    private TextView tvHint;
    private Button btnConnect;
    private TextView tvWycode;
    private TextView tvVersion;

    private NotificationService mNotificationService;

    private boolean mIsBound;

    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private boolean isEnabledNLS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        bindListener();
        setView();
    }

    private void setView() {
        //TODO version
//        tvVersion.setText(getString(R.string.version_text,getPackageManager()));
    }

    private void findViews() {
        tvHint = (TextView) findViewById(R.id.tv_query_number);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        tvWycode = (TextView) findViewById(R.id.tv_wycode);
        tvVersion = (TextView) findViewById(R.id.tv_version);
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
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
//                Intent wyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://wycode.cn"));
//                startActivity(wyIntent);
                break;
        }
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mNotificationService = ((NotificationService.LocalBinder) service).getService();

            mNotificationService.setNotificationListener(MainActivity.this);
            // Tell the user about this for our demo.
            tvHint.append("\n服务已启动");
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mNotificationService = null;
            tvHint.append("\n服务已断开");
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
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
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        tvHint.append("\n收到通知："+sbn);
    }
}