package cn.wycode.notifyme;

/**
 * entity NotificationAccount
 * Created by wy on 2016/11/20.
 */

public class NotificationAccount {
    private Long queryId;
    private String deviceId;

    @Override
    public String toString() {
        return "NotificationAccount{" +
                "queryId=" + queryId +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }

    public Long getQueryId() {
        return queryId;
    }

    public void setQueryId(Long queryId) {
        this.queryId = queryId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
