package com.plnrqrzy.tjtutfa.app;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.plnrqrzy.tjtutfa.util.SharedPreferencesUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.concurrent.TimeUnit;

import me.yokeyword.fragmentation.BuildConfig;
import me.yokeyword.fragmentation.Fragmentation;
import me.yokeyword.fragmentation.helper.ExceptionHandler;
import okhttp3.OkHttpClient;

/**
 * APP初始化
 */
public class App extends Application {
    public static String IMEI = "";
    public static int CHANNEL_ID = 0;
    public static int USER_ID = 0;
    public static int isVip = 0;

    public static boolean isNeedCheckOrder = false;
    public static int orderIdInt = 0;

    //用户id
    public static final String USERID_KEY = "user_id";
    public static final String ISVIP_KEY = "is_vip";


    //上一次的登录时间
    public static final String LAST_LOGIN_TIME = "last_login_time";

    @Override
    public void onCreate() {
        super.onCreate();
        CHANNEL_ID = getChannelName();
        USER_ID = SharedPreferencesUtil.getInt(this, USERID_KEY, 0);

        Fragmentation.builder()
                // 设置 栈视图 模式为 悬浮球模式   SHAKE: 摇一摇唤出   NONE：隐藏
                .stackViewMode(Fragmentation.NONE)
                // ture时，遇到异常："Can not perform this action after onSaveInstanceState!"时，会抛出
                // false时，不会抛出，会捕获，可以在handleException()里监听到
                .debug(BuildConfig.DEBUG)
                // 线上环境时，可能会遇到上述异常，此时debug=false，不会抛出该异常（避免crash），会捕获
                // 建议在回调处上传至我们的Crash检测服务器
                .handleException(new ExceptionHandler() {
                    @Override
                    public void onException(Exception e) {
                        //捕获到的异常处理
                        Log.e("e", e.getMessage());
                    }
                })
                .install();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10000L, TimeUnit.MILLISECONDS)
                .readTimeout(10000L, TimeUnit.MILLISECONDS)
                //其他配置
                .build();

        OkHttpUtils.initClient(okHttpClient);
    }


    private int getChannelName() {
        int resultData = 0;
        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        resultData = applicationInfo.metaData.getInt("CHANNEL");
                    }
                }

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return resultData;
    }
}