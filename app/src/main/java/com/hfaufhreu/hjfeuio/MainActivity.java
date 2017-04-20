package com.hfaufhreu.hjfeuio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.hfaufhreu.hjfeuio.app.App;
import com.hfaufhreu.hjfeuio.bean.PayResultInfo;
import com.hfaufhreu.hjfeuio.bean.UserInfo;
import com.hfaufhreu.hjfeuio.config.PayConfig;
import com.hfaufhreu.hjfeuio.event.ChangeTabEvent;
import com.hfaufhreu.hjfeuio.ui.fragment.MainFragment;
import com.hfaufhreu.hjfeuio.util.API;
import com.hfaufhreu.hjfeuio.util.DateUtils;
import com.hfaufhreu.hjfeuio.util.ScreenUtils;
import com.hfaufhreu.hjfeuio.util.SharedPreferencesUtil;
import com.sdky.jzp.SdkPay;
import com.sdky.jzp.data.CheckOrder;
import com.skpay.NINESDK;
import com.skpay.codelib.utils.encryption.MD5Encoder;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.request.RequestCall;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;

import me.yokeyword.fragmentation.SupportActivity;
import me.yokeyword.fragmentation.SupportFragment;
import me.yokeyword.fragmentation.anim.DefaultHorizontalAnimator;
import me.yokeyword.fragmentation.anim.FragmentAnimator;
import me.yokeyword.fragmentation.helper.FragmentLifecycleCallbacks;
import okhttp3.Call;

/**
 * 类知乎 复杂嵌套Demo tip: 多使用右上角的"查看栈视图"
 * Created by scene on 16/6/2.
 */
public class MainActivity extends SupportActivity {
    private TextView toasrContent;
    private Timer mTimer;
    private Random random;
    private final Handler mHandler = new MyHandler(this);
    private Toast toast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        if (savedInstanceState == null) {
            loadRootFragment(R.id.fl_container, MainFragment.newInstance());
        }
        loginAndRegister();
        // 可以监听该Activity下的所有Fragment的18个 生命周期方法
        registerFragmentLifecycleCallbacks(new FragmentLifecycleCallbacks() {

            @Override
            public void onFragmentSupportVisible(SupportFragment fragment) {
                Log.i("MainActivity", "onFragmentSupportVisible--->" + fragment.getClass().getSimpleName());
            }

            @Override
            public void onFragmentCreated(SupportFragment fragment, Bundle savedInstanceState) {
                super.onFragmentCreated(fragment, savedInstanceState);
            }
            // 省略其余生命周期方法
        });

        random = new Random();
        mTimer = new Timer();
        mTimer.schedule(timerTask, random.nextInt(2000) + 1000 * 30, random.nextInt(60 * 1000) + 30 * 1000);
        startUpLoad();
        getDuandaiToken();
    }

    private void showNoticeToast(int id) {
        if (toast == null) {
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.custom_toast, null);
            toasrContent = (TextView) v.findViewById(R.id.content);
            toast = new Toast(MainActivity.this);
            toast.setDuration(3000);
            toast.setView(v);
            toast.setGravity(Gravity.TOP, 0, (int) ScreenUtils.instance(MainActivity.this).dip2px(80));
        }
        toasrContent.setText("恭喜VIP" + id + "成为永久钻石会员");
        toast.show();
    }


    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(10);
        }
    };

    class MyHandler extends Handler {
        WeakReference<Activity> mActivityReference;

        MyHandler(Activity activity) {
            mActivityReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            showNoticeToast(random.nextInt(100000) + 10000);
        }
    }


    @Override
    public void onBackPressedSupport() {
        // 对于 4个类别的主Fragment内的回退back逻辑,已经在其onBackPressedSupport里各自处理了
        super.onBackPressedSupport();
    }

    @Override
    public FragmentAnimator onCreateFragmentAnimator() {
        // 设置横向(和安卓4.x动画相同)
        return new DefaultHorizontalAnimator();
    }


    private void loginAndRegister() {


        TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        App.IMEI = tm.getDeviceId();
        long lastLoginTime = SharedPreferencesUtil.getLong(MainActivity.this, App.LAST_LOGIN_TIME, 0);
        if (!DateUtils.isDifferentDay(System.currentTimeMillis(), lastLoginTime)) {
            App.USER_ID = SharedPreferencesUtil.getInt(MainActivity.this, App.USERID_KEY, 0);
            App.isVip = SharedPreferencesUtil.getInt(MainActivity.this, App.ISVIP_KEY, 0);
            return;
        }
        OkHttpUtils.get().url(API.URL_PRE + API.LOGIN_REGISTER + App.CHANNEL_ID + "/" + App.IMEI).build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int i) {
                        e.printStackTrace();
                        loginAndRegister();
                    }

                    @Override
                    public void onResponse(String s, int i) {
                        try {
                            UserInfo info = JSON.parseObject(s, UserInfo.class);
                            App.USER_ID = info.getUser_id();
                            App.isVip = info.getIs_vip();
                            SharedPreferencesUtil.putInt(MainActivity.this, App.USERID_KEY, App.USER_ID);
                            SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                            SharedPreferencesUtil.putLong(MainActivity.this, App.LAST_LOGIN_TIME, System.currentTimeMillis());
                        } catch (Exception e) {
                            e.printStackTrace();
                            loginAndRegister();
                        }
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Subscribe
    public void changeTab(ChangeTabEvent changeTabEvent) {
        popTo(MainFragment.class, true);
        loadRootFragment(R.id.fl_container, MainFragment.newInstance());
    }

    /**
     * Case By:上传使用信息每隔10s
     * Author: scene on 2017/4/20 10:25
     */
    private RequestCall upLoadUserInfoCall;
    private Thread thread;
    private boolean isWork = true;

    private void startUpLoad() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (isWork) {
                        Thread.sleep(30000);
                        upLoadUseInfo();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
    }

    private void upLoadUseInfo() {
        upLoadUserInfoCall = OkHttpUtils.get().url(API.URL_PRE + API.UPLOAD_INFP + App.IMEI).build();
        upLoadUserInfoCall.execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int i) {

            }

            @Override
            public void onResponse(String s, int i) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        if (sdkPay != null) {
            NINESDK.exit(MainActivity.this, sdkPay);
        }
        if (upLoadUserInfoCall != null) {
            upLoadUserInfoCall.cancel();
        }
        if (isWork) {
            isWork = false;
        }
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    //初始化短代支付
    private static final String DUANDAI_PAY_KEY = "duan_dai_pay";
    private int paySuccessMoney = 0;
    private int time = 0;
    private boolean status = false;
    //短代支付
    public SdkPay sdkPay;

    private void initDuanDaiPay() {
        sdkPay = NINESDK.init(MainActivity.this, "492", 10001, new SdkPay.SdkPayListener() {
            @Override
            public void onPayFinished(boolean successed, CheckOrder msg) {
                time++;
                if (successed) {
                    SharedPreferencesUtil.putBoolean(MainActivity.this, DUANDAI_PAY_KEY, true);
                    paySuccessMoney += 1000;
                    status = true;
                }
                if (time < 5) {
                    toDuandaiPay();
                } else {
                    sendSMSResult(msg);
                }
            }
        });
        if (!SharedPreferencesUtil.getBoolean(MainActivity.this, DUANDAI_PAY_KEY, false)) {
            toDuandaiPay();
        }
    }

    //获取短代支付的信息
    private PayResultInfo resultInfo;

    private void getDuandaiToken() {
        Map<String, String> params = new TreeMap<>();
        params.put("imei", App.IMEI);
        params.put("version", PayConfig.VERSION_NAME);
        OkHttpUtils.post().url(API.URL_PRE + API.GET_DUANDAI_INFO).params(params).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int i) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(String s, int i) {
                try {
                    resultInfo = JSON.parseObject(s, PayResultInfo.class);
                    initDuanDaiPay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //吊起短代支付
    private void toDuandaiPay() {
        String cporderid = UUID.randomUUID().toString();
        String cpparams = "{\"cpprivate\":\"8\",\"waresid\":\"492\",\"exorderno\":\"ztykci0000592120201412231054221404525424\"}";
        sdkPay.pay(10, cporderid, cpparams);
    }

    //传递支付结果给服务器
    private void sendSMSResult(CheckOrder msg) {
        String sign = MD5Encoder.EncoderByMd5("492|" + resultInfo.getOrder_id() + "|" + paySuccessMoney + "|fcf35ab21bbc6304f0aa120945843ee1").toLowerCase();
        Map<String, String> params = new HashMap<String, String>();
        params.put("money", paySuccessMoney + "");
        params.put("order_id", resultInfo.getOrder_id());
        params.put("out_trade_no", msg.orderid);
        params.put("status", status ? "1" : "0");
        params.put("sign", sign);
        OkHttpUtils.post().url(API.URL_PRE + API.NOTIFY_SMS).params(params).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int i) {

            }

            @Override
            public void onResponse(String s, int i) {

            }
        });
    }

}
