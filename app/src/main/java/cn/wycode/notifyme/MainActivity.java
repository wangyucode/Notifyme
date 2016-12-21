package cn.wycode.notifyme;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextWatcher {

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
    private static final String ID_KEY = "cn.wycode.ID_KEY";
    private boolean isEnabledNLS = false;

    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver notifyReceiver;

    private VolleyUtils volley;
    private String deviceId;
    private int notifyNumber;

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

        OKhttpUtils.init(getApplication());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isSavePower()) {
                showConfirmPowerDialog();
            }
        }
        isEnabledNLS = isEnabled();
        Log.d(TAG, "isEnabledNLS = " + isEnabledNLS);
        if (!isEnabledNLS) {
            showConfirmDialog();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isSavePower() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return !pm.isIgnoringBatteryOptimizations("cn.wycode.notifyme");

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
        svContent = (ScrollView) findViewById(R.id.sv_content);
    }

    private void bindListener() {
        btnConnect.setOnClickListener(this);
        tvWycode.setOnClickListener(this);
        tvHint.addTextChangedListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                if (btnConnect.getText().equals("开始同步")) {
                    tvHint.append("\n正在启动服务...");
                    doBindService();
                } else {
                    tvHint.append("\n正在关闭服务...");
                    doUnbindService();

                }
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

            login();
        }

        public void onServiceDisconnected(ComponentName className) {
            tvHint.append("\n服务已停止");
            btnConnect.setText("开始同步");
        }
    };

    private void login() {
        deviceId = getUUID();

        OkGo.post(Constants.URL_LOGIN)
                .params("deviceId", deviceId)
                .params("secret", Constants.REQUEST_KEY)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(String s, Call call, okhttp3.Response response) {
                        Log.d(TAG, "onResponse--->" + s);
                        ResultBean<NotificationAccount> account = null;
                        try {
                            account = JsonUtil.toJavaBean(s, NotificationAccount.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String queryId = account.data.getQueryId().toString();

                        SpannableString ss = new SpannableString("您的查询码是：" + queryId);
                        ForegroundColorSpan colorSpan = new ForegroundColorSpan(getResources().getColor(R.color.colorAccent));
                        ss.setSpan(colorSpan, 7, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        RelativeSizeSpan sizeSpan = new RelativeSizeSpan(2f);
                        ss.setSpan(sizeSpan, 7, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        tvHint.append("\n");
                        tvHint.append(ss);
                        btnConnect.setText("停止同步");
                    }
                });
        notifyNumber = 0;
    }

    private String getUUID() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String id = sp.getString(ID_KEY, null);
        if (TextUtils.isEmpty(id)) {
            id = UUID.randomUUID().toString();
            sp.edit().putString(ID_KEY, id).apply();
        }
        return id;
    }

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
            tvHint.append("\n服务已停止");
            btnConnect.setText("开始同步");
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
                .create().show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showConfirmPowerDialog() {
        new AlertDialog.Builder(this)
                .setMessage("请关闭通蜜的电池优化")
                .setTitle("请确认关闭")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                try {
                                    startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                .create().show();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        svContent.post(new Runnable() {
            public void run() {
                svContent.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    class NotificationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            StatusBarNotification sbn = intent.getParcelableExtra(EXTRA_KEY_NOTIFICATION);
            Notification notification = sbn.getNotification();

            Log.d(TAG, "Notification received:" + notification.extras);
            String appName = "unknow";
            try {
                ApplicationInfo appInfo = getPackageManager().getPackageInfo(sbn.getPackageName(), 0).applicationInfo;
                appName = getPackageManager().getApplicationLabel(appInfo).toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String when = new SimpleDateFormat("MM月dd日 HH:mm:ss").format(new Date(sbn.getPostTime()));
            String title = notification.extras.getString(Notification.EXTRA_TITLE);
            String content = notification.extras.getString(Notification.EXTRA_TEXT);
            tvHint.append("\n\n收到通知来自：" + appName);
            tvHint.append("\n时间：" + when);
            tvHint.append("\n标题：" + title);
            tvHint.append("\n内容：" + content);

            if (mIsBound) {
                addNotification(appName, when, title, content);
            }
        }
    }

    private void addNotification(String appName, String when, String title, String content) {
        OkGo.post(Constants.URL_ADD_NOTIFICATION)
                .params("deviceId", deviceId)
                .params("secret", Constants.REQUEST_KEY)
                .params("title", title)
                .params("when", when)
                .params("text", content)
                .params("appName", appName)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(String s, Call call, okhttp3.Response response) {

                        Log.d(TAG, "onResponse--->" + s);

                        notifyNumber++;
                        tvHint.append(String.format("\n已同步%d条消息", notifyNumber));

                    }
                });
        Map<String, String> params = new HashMap<>();
        params.put("deviceId", deviceId);
        params.put("secret", Constants.REQUEST_KEY);
        params.put("title", title);
        params.put("when", when);
        params.put("text", content);
        params.put("appName", appName);
    }

}