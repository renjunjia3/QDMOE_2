package com.mzhguqvn.mzhguq.ui.widget;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.mzhguqvn.mzhguq.MainActivity;
import com.mzhguqvn.mzhguq.R;
import com.mzhguqvn.mzhguq.bean.VideoInfo;
import com.mzhguqvn.mzhguq.bean.VipInfo;
import com.mzhguqvn.mzhguq.util.SharedPreferencesUtil;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.leolin.shortcutbadger.ShortcutBadger;

public class ChatHeadService extends Service {
    private WindowManager windowManager;
    private View chatHead;
    private ImageView image;
    private TextView title;

    private boolean viewIsadded = false;
    private WindowManager.LayoutParams params;

    private final Handler mHandler = new MyHandler(getApplication());

    private long exitTime = 0;
    private int count = 0;

    private List<VideoInfo> list = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        chatHead = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_service_view, null);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP;

        image = (ImageView) chatHead.findViewById(R.id.image);
        title = (TextView) chatHead.findViewById(R.id.title);
        chatHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent mainIntent = new Intent(ChatHeadService.this, MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainIntent);
                    if (viewIsadded) {
                        windowManager.removeViewImmediate(chatHead);
                        viewIsadded = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        new Timer().schedule(timerTask, 1 * 60 * 1000, 1 * 60 * 1000);
    }

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(1000);
        }
    };

    class MyHandler extends Handler {
        WeakReference<Context> mActivityReference;

        MyHandler(Context context) {
            mActivityReference = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                if (MainActivity.isApplicationBroughtToBackground(getApplicationContext())) {
                    if (exitTime == 0) {
                        exitTime = System.currentTimeMillis();
                    } else {
                        if (list.size() <= 0) {
                            try {
                                String jsonStr = SharedPreferencesUtil.getString(ChatHeadService.this, "NOTIFY_DATA", "");
                                VipInfo vipInfo = JSON.parseObject(jsonStr, VipInfo.class);
                                for (int i = 0; i < vipInfo.getOther().size(); i++) {
                                    for (int j = 0; j < vipInfo.getOther().get(i).getData().size(); j++) {
                                        VideoInfo videoInfo2 = vipInfo.getOther().get(i).getData().get(j);
                                        videoInfo2.setTilteType(false);
                                        list.add(videoInfo2);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (count == 0) {
                            //第一次是5分钟提示
                            if (System.currentTimeMillis() - exitTime > 5 * 60 * 1000) {
                                if (!viewIsadded && list.size() > 0) {
                                    title.setText(list.get(count % list.size()).getTitle());
                                    Glide.with(ChatHeadService.this).load(list.get(count % list.size()).getThumb()).asBitmap().centerCrop().into(image);
                                    windowManager.addView(chatHead, params);
                                    viewIsadded = true;
                                    count++;
                                    exitTime = 0;
                                    boolean success = ShortcutBadger.applyCount(ChatHeadService.this, 1);
                                    if (!success) {
                                        setBadgeOfMIUI(ChatHeadService.this, 1);
                                    }
                                }
                            }
                        } else {
                            //之后是每隔3h提示一次
                            if (System.currentTimeMillis() - exitTime > 3 * 60 * 60 * 1000) {
                                if (!viewIsadded) {
                                    title.setText(list.get(count % list.size()).getTitle());
                                    Glide.with(ChatHeadService.this).load(list.get(count % list.size()).getThumb()).asBitmap().centerCrop().into(image);
                                    windowManager.addView(chatHead, params);
                                    viewIsadded = true;
                                    count++;
                                    exitTime = 0;
                                    boolean success = ShortcutBadger.applyCount(ChatHeadService.this, 1);
                                    if (!success) {
                                        setBadgeOfMIUI(ChatHeadService.this, 1);
                                    }
                                }
                            }
                        }

                    }

                } else {
                    if (viewIsadded) {
                        windowManager.removeViewImmediate(chatHead);
                        viewIsadded = false;
                        exitTime = 0;
                        count = 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (chatHead != null) {
            if (viewIsadded) {
                windowManager.removeViewImmediate(chatHead);
                viewIsadded = false;
                exitTime = 0;
                count = 0;
            }
        }
        super.onDestroy();
    }


    /**
     * 设置MIUI的Badge
     *
     * @param context context
     * @param count   count
     */
    private static void setBadgeOfMIUI(Context context, int count) {
        Log.d("xys", "Launcher : MIUI");
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("title").setContentText("text").setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        try {
            Field field = notification.getClass().getDeclaredField("extraNotification");
            Object extraNotification = field.get(notification);
            Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
            method.invoke(extraNotification, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mNotificationManager.notify(0, notification);
    }

    /**
     * Case By:移除角标
     * Author: scene on 2017/5/4 14:56
     */
    private void removeBadge() {
        boolean success = ShortcutBadger.applyCount(ChatHeadService.this, 0);
        if (!success) {
            removeBadge();
            setBadgeOfMIUI(ChatHeadService.this, 0);
        }
    }
}
