package cn.wycode.notifyme;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.util.Map;

/**
 * use Volley library to access network
 * Created by wy on 2016/11/20.
 */

public class VolleyUtils {
    private static VolleyUtils mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private VolleyUtils(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static synchronized VolleyUtils getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyUtils(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    /**
     * add parameters to url for GET
     *
     * @param url    url request
     * @param params parameters
     * @return GET url with parameters
     */
    public static String convertUrlForGetParams(String url, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(url);
        if (params.isEmpty()) {
            return sb.toString();
        }
        boolean isFirst = true;
        for (String key : params.keySet()) {
            if (isFirst) {
                sb.append('?');
                isFirst = false;
            } else {
                sb.append('&');
            }
            sb.append(key);
            sb.append('=');
            sb.append(params.get(key));
        }
        return sb.toString();
    }
}
