package com.mzhguqvn.mzhguq;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.mzhguqvn.mzhguq.app.App;
import com.mzhguqvn.mzhguq.app.CrashHandler;
import com.mzhguqvn.mzhguq.bean.CheckOrderInfo;
import com.mzhguqvn.mzhguq.bean.UpdateInfo;
import com.mzhguqvn.mzhguq.event.ChangeTabEvent;
import com.mzhguqvn.mzhguq.event.CheckOrderEvent;
import com.mzhguqvn.mzhguq.event.GoodsPaySuccessEvent;
import com.mzhguqvn.mzhguq.ui.dialog.CustomSubmitDialog;
import com.mzhguqvn.mzhguq.ui.dialog.DownLoadDialog;
import com.mzhguqvn.mzhguq.ui.dialog.UpdateDialog;
import com.mzhguqvn.mzhguq.ui.fragment.MainFragment;
import com.mzhguqvn.mzhguq.ui.widget.ChatHeadService;
import com.mzhguqvn.mzhguq.util.API;
import com.mzhguqvn.mzhguq.util.DialogUtil;
import com.mzhguqvn.mzhguq.util.ScreenUtils;
import com.mzhguqvn.mzhguq.util.SharedPreferencesUtil;
import com.mzhguqvn.mzhguq.util.ToastUtils;
import com.umeng.analytics.MobclickAgent;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.request.RequestCall;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import me.yokeyword.fragmentation.SupportActivity;
import me.yokeyword.fragmentation.anim.DefaultHorizontalAnimator;
import me.yokeyword.fragmentation.anim.FragmentAnimator;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
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

    public static boolean isNeedChangeTab = false;

    //版本更新
    private RequestCall updateRequestCall;
    private UpdateInfo updateInfo;
    private DownLoadDialog downLoadDialog;
    private DownLoadDialog.Builder downLoadDialogBuilder;
    private MaterialProgressBar progressBar;
    private RequestCall downLoadRequestCall;
    //版本更新的对话框
    private UpdateDialog updateDialog;
    private UpdateDialog.Builder updateDialogBuilder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerBoradcastReceiver();
        EventBus.getDefault().register(this);
        if (savedInstanceState == null) {
            loadRootFragment(R.id.fl_container, MainFragment.newInstance());
        }

        random = new Random();
        mTimer = new Timer();
        mTimer.schedule(timerTask, random.nextInt(2000) + 1000 * 30, random.nextInt(60 * 1000) + 30 * 1000);
        startUpLoad();
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("支付结果获取中...");
        getUpdateData();
        startService(new Intent(MainActivity.this, ChatHeadService.class));
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
            if (!isApplicationBroughtToBackground(MainActivity.this)) {
                showNoticeToast(random.nextInt(100000) + 10000);
            }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 9999) {
            //  changeTab(new ChangeTabEvent(App.isVip));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            MobclickAgent.onResume(this);
            if (isNeedChangeTab) {
                isNeedChangeTab = false;
                changeTab(new ChangeTabEvent(App.isVip));
            }
            if (App.isGoodsPay && App.isNeedCheckOrder && App.goodsOrderId != 0) {
                checkGoodsOrder();
            } else if (App.isNeedCheckOrder && App.orderIdInt != 0) {
                checkOrder();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Subscribe
    public void changeTab(ChangeTabEvent changeTabEvent) {
        DialogUtil.getInstance().cancelAllDialog();
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
        if (upLoadUserInfoCall != null) {
            upLoadUserInfoCall.cancel();
        }
        if (requestCall != null) {
            requestCall.cancel();
        }
        if (updateRequestCall != null) {
            updateRequestCall.cancel();
        }
        if (isWork) {
            isWork = false;
        }
        if (checkGoodsOrderRequestCall != null) {
            checkGoodsOrderRequestCall.cancel();
        }
        unRegisterBoradcastReceiver();
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    /**
     * 检查是否成功
     */
    private RequestCall requestCall;
    private ProgressDialog progressDialog;

    @Subscribe
    public void onCheckOrder(CheckOrderEvent checkOrderEvent) {
        checkOrder();
    }

    private void checkOrder() {
        if (progressDialog != null) {
            progressDialog.show();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<>();
                params.put("order_id", App.orderIdInt + "");
                params.put("imei", App.IMEI);
                App.isNeedCheckOrder = false;
                App.orderIdInt = 0;
                requestCall = OkHttpUtils.get().url(API.URL_PRE + API.CHECK_ORDER).params(params).build();
                requestCall.execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int i) {
                        e.printStackTrace();
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        ToastUtils.getInstance(MainActivity.this).showToast("支付失败请重试，或者更换其他支付方式");
                    }

                    @Override
                    public void onResponse(String s, int i) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        try {
                            CheckOrderInfo checkOrderInfo = JSON.parseObject(s, CheckOrderInfo.class);
                            if (checkOrderInfo.isStatus()) {
                                MainActivity.onPaySuccess();
                                App.isHeijin = checkOrderInfo.getIs_heijin();
                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISHEIJIN_KEY, App.isHeijin);
                                switch (App.isVip) {
                                    case 0:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                App.isVip = 1;
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                String message = "恭喜您成为黄金会员";
                                                CustomSubmitDialog customSubmitDialog0 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog0.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });

                                        break;
                                    case 1:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                App.isVip = 2;
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                String message = "恭喜您成为钻石会员";
                                                CustomSubmitDialog customSubmitDialog1 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog1.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });

                                        break;
                                    case 2:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                String message;
                                                if (App.isOPenBlackGlodVip) {
                                                    App.isVip = 2;
                                                    message = "恭喜您成为最牛逼的黑金会员";
                                                } else {
                                                    App.isVip = 3;
                                                    message = "恭喜您成功注册VPN海外会员";
                                                }
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                CustomSubmitDialog customSubmitDialog2 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog2.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });

                                            }
                                        });

                                        break;
                                    case 3:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (App.isHeijin == 1) {
                                                    App.isVip = 5;
                                                } else {
                                                    App.isVip = 4;
                                                }
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                String message = "恭喜你进入海外片库，我们将携手为您服务";
                                                CustomSubmitDialog customSubmitDialog3 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog3.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });
                                        break;
                                    case 4:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (App.isVip >= 4) {
                                                    App.isVip = 5;
                                                    SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                }
                                                App.isHeijin = 1;
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISHEIJIN_KEY, App.isHeijin);
                                                String message = "恭喜您成为最牛逼的黑金会员";
                                                CustomSubmitDialog customSubmitDialog4 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog4.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });
                                        break;
                                    case 5:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                App.isVip = 6;
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                String message = "恭喜您开通海外高速通道";
                                                CustomSubmitDialog customSubmitDialog5 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog5.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });
                                        break;
                                    case 6:
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                App.isVip = 7;
                                                SharedPreferencesUtil.putInt(MainActivity.this, App.ISVIP_KEY, App.isVip);
                                                String message = "恭喜您开通海外双线通道";
                                                CustomSubmitDialog customSubmitDialog6 = DialogUtil.getInstance().showCustomSubmitDialog(MainActivity.this, message);
                                                customSubmitDialog6.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                    @Override
                                                    public void onDismiss(DialogInterface dialog) {
                                                        changeTab(new ChangeTabEvent(App.isVip));
                                                    }
                                                });
                                            }
                                        });
                                        break;
                                    default:
                                        break;
                                }


                            } else {
                                ToastUtils.getInstance(MainActivity.this).showToast("支付失败请重试，或者更换其他支付方式");
                            }
                            App.isOPenBlackGlodVip = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 1000);

    }

    /**
     * Case By:检查商品订单
     * Author: scene on 2017/5/10 15:10
     */
    private RequestCall checkGoodsOrderRequestCall;

    private void checkGoodsOrder() {
        if (progressDialog != null) {
            progressDialog.show();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkGoodsOrderRequestCall = OkHttpUtils.get().url(API.URL_PRE + API.CHECK_GOODS_ORDER + App.USER_ID + "/" + App.goodsOrderId).build();
                checkGoodsOrderRequestCall.execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int i) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        ToastUtils.getInstance(MainActivity.this).showToast("支付失败请重试，或者更换其他支付方式");
                    }

                    @Override
                    public void onResponse(String s, int i) {
                        try {
                            App.goodsOrderId = 0;
                            App.isNeedCheckOrder = false;
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            CheckOrderInfo checkOrderInfo = JSON.parseObject(s, CheckOrderInfo.class);
                            if (checkOrderInfo.isStatus()) {
                                EventBus.getDefault().post(new GoodsPaySuccessEvent(App.isGoodsBuyPage));
                            } else {
                                ToastUtils.getInstance(MainActivity.this).showToast("支付失败请重试，或者更换其他支付方式");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }, 1000);

    }

    public static boolean isApplicationBroughtToBackground(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks != null && !tasks.isEmpty()) {
            String topActivity = tasks.get(0).baseActivity.getPackageName();
            return !topActivity.equals(context.getPackageName());
        }
        return false;
    }

    /**
     * 支付成功之后调用
     */
    public static void onPaySuccess() {
        Map<String, String> params = new HashMap<>();
        params.put("position_id", "16");
        params.put("user_id", App.USER_ID + "");
        OkHttpUtils.post().url(API.URL_PRE + API.UPLOAD_CURRENT_PAGE).params(params).build().execute(null);
    }

    /**
     * Case By:版本更新
     * Author: scene on 2017/4/25 16:55
     */
    private void getUpdateData() {
        updateRequestCall = OkHttpUtils.get().url(API.URL_PRE + API.UPDATE + App.CHANNEL_ID).build();
        updateRequestCall.execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int i) {

            }

            @Override
            public void onResponse(String s, int i) {
                try {
                    updateInfo = JSON.parseObject(s, UpdateInfo.class);
                    int versionCode = getVersion();
                    if (versionCode != 0 && updateInfo.getVersion_code() > versionCode) {
                        showUploadDialog(updateInfo.getApk_url());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * Case By:下载文件
     * Author: scene on 2017/4/25 17:09
     *
     * @param url 文件路径
     */
    private void downLoadFile(String url) {
        downLoadRequestCall = OkHttpUtils.get().url(url).build();
        downLoadRequestCall.execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(), System.currentTimeMillis() + ".apk") {
            @Override
            public void inProgress(float progress, long total, int id) {
                if (progressBar != null) {
                    progressBar.setProgress((int) (100 * progress));
                }
            }

            @Override
            public void onError(Call call, Exception e, int i) {

            }

            @Override
            public void onResponse(File file, int i) {
                try {
                    if (file != null) {
                        installAPK(MainActivity.this, file.getAbsolutePath());
                        if (downLoadDialog != null) {
                            downLoadDialog.dismiss();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }

            }

        });
    }


    /**
     * Case By:提示更新的对话框
     * Author: scene on 2017/4/27 16:52
     */
    public void showUploadDialog(final String url) {
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.cancel();
        }
        updateDialogBuilder = new UpdateDialog.Builder(MainActivity.this);
        updateDialog = updateDialogBuilder.create();
        updateDialog.show();
        updateDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateDialog.dismiss();
                //需要更新
                if (downLoadDialog != null && downLoadDialog.isShowing()) {
                    downLoadDialog.cancel();
                }
                downLoadDialogBuilder = new DownLoadDialog.Builder(MainActivity.this);
                downLoadDialog = downLoadDialogBuilder.create();
                progressBar = downLoadDialogBuilder.getProgressBar();
                downLoadDialog.show();
                downLoadFile(url);
                downLoadDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                });
            }
        });
    }


    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    public int getVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            int versionCode = info.versionCode;
            return versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 安装app
     *
     * @param mContext
     * @param fileName
     */
    private static void installAPK(Context mContext, String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/NNY_1001.apk";
        intent.setDataAndType(Uri.parse("file://" + fileName), "application/vnd.android.package-archive");
        mContext.startActivity(intent);
    }

    /**
     * Case By:上传崩溃日志
     * Author: scene on 2017/4/26 17:56
     */
    private RequestCall uploadCrashRequestCall;

    private void uploadCrashInfo() {
        //获取崩溃日志文件夹文件的数量
        final String dirPath = Environment.getExternalStorageDirectory() + File.separator + "dPhoneLog";
        List<String> files = getVideoFileName(dirPath);
        Log.e("carshFiles", "文件数量：" + files.size());
        int size = files.size();
        if (size > 0) {
            //上传到服务器
            PostFormBuilder postFormBuilder = OkHttpUtils.post().url(API.URL_PRE + API.LOG);
            for (int i = 0; i < size; i++) {
                File file = new File(files.get(i));
                postFormBuilder.addFile("file[]", file.getName(), file);
            }
            uploadCrashRequestCall = postFormBuilder.build();
            uploadCrashRequestCall.execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int i) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(String s, int i) {
                    //上传完成后删除本地的日志文件
                    CrashHandler.recursionDeleteFile(new File(dirPath));
                }
            });
        }
    }

    /**
     * Case By:获取指定目录的文件
     * Author: scene on 2017/4/26 17:59
     */
    public static List<String> getVideoFileName(String fileAbsolutePath) {
        List<String> vecFile = new ArrayList<>();
        try {
            File file = new File(fileAbsolutePath);
            File[] subFile = file.listFiles();

            for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
                // 判断是否为文件夹
                if (!subFile[iFileLength].isDirectory()) {
                    String filename = subFile[iFileLength].getAbsolutePath();
                    // 判断是否为log结尾
                    if (filename.trim().toLowerCase().endsWith(".log")) {
                        vecFile.add(filename);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vecFile;
    }

    public static final String ACTION_NAME_MAINACTIVITY_CHECK_ORDER = "action_name_MainActivity_check_order";
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_NAME_MAINACTIVITY_CHECK_ORDER)) {
                boolean isGoods = intent.getBooleanExtra("IS_GOODS", false);
                if (isGoods) {
                    checkGoodsOrder();
                } else {
                    checkOrder();
                }
            }
        }
    };

    private void registerBoradcastReceiver() {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTION_NAME_MAINACTIVITY_CHECK_ORDER);
        //注册广播
        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    private void unRegisterBoradcastReceiver() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

}

