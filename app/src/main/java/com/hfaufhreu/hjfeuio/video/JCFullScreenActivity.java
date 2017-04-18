package com.hfaufhreu.hjfeuio.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.alibaba.fastjson.JSON;
import com.hfaufhreu.hjfeuio.R;
import com.hfaufhreu.hjfeuio.bean.CommentInfo;
import com.hfaufhreu.hjfeuio.bean.VideoInfo;
import com.hfaufhreu.hjfeuio.util.SharedPreferencesUtil;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.AndroidDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.ViewCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.danmaku.util.SystemClock;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * <p>全屏的activity</p>
 * <p>fullscreen activity</p>
 * Created by Nathen
 * On 2015/12/01 11:17
 */
public class JCFullScreenActivity extends Activity {

    private final Handler mHandler = new MyHandler(this);
    public static final int DIALOG_TYPE_FULLVIDEO = 0;
    public static final int DIALOG_TYPE_FUNCATION = 1;
    public static final int DIALOG_TYPE_SPEED = 2;

    public static final String PARAM_CURRENT_TIME = "current_time";
    public static final String PARAM_DIALOG_TYPE = "dialog_type";
    public static final String PARAM_VIDEO_INFO = "video_info";
    public static final String PARAM_TRY_COUNT = "try_count";
    public static final String PARAM_IS_VIP = "is_vip";
    public static final String PARAM_IS_SPEED = "is_speed";
    public static final String PARAM_STR_COMMENT = "str_comment";


    private static VideoInfo videoInfo;
    private static long currentTime = 0;
    private static Timer mTimer;
    private static int isVIP = 0;
    private static int tryCount = 0;
    private static int isSpeed = 0;


    private JCVideoPlayerStandard mJcVideoPlayer;
    /**
     * 刚启动全屏时的播放状态
     */
    static int CURRENT_STATE = -1;
    public static String URL;
    static boolean DIRECT_FULLSCREEN = false;//this is should be in videoplayer
    static Class VIDEO_PLAYER_CLASS;
    //弹幕
    private BaseDanmakuParser mParser;
    private DanmakuContext mContext;
    private DanmakuView mDanmakuView;
    private int mIconWidth;
    private Timer timer = new Timer();
    private List<CommentInfo> commentInfoList;
    private int countTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .memoryCacheSizePercentage(13).build(); // default
        ImageLoader.getInstance().init(config);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initData();
        try {
            Constructor<JCVideoPlayerStandard> constructor = VIDEO_PLAYER_CLASS.getConstructor(Context.class);
            mJcVideoPlayer = constructor.newInstance(this);
            setContentView(mJcVideoPlayer);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mJcVideoPlayer.mIfCurrentIsFullscreen = true;
        mJcVideoPlayer.mIfFullscreenIsDirectly = DIRECT_FULLSCREEN;
        mJcVideoPlayer.setUp(URL, videoInfo.getTitle());
        mJcVideoPlayer.setStateAndUi(CURRENT_STATE);
        mJcVideoPlayer.addTextureView();

        if (mJcVideoPlayer.mIfFullscreenIsDirectly) {
            mJcVideoPlayer.startButton.performClick();
        } else {
            JCVideoPlayer.IF_RELEASE_WHEN_ON_PAUSE = true;
            JCMediaManager.instance().listener = mJcVideoPlayer;
            if (CURRENT_STATE == JCVideoPlayer.CURRENT_STATE_PAUSE) {
                JCMediaManager.instance().mediaPlayer.seekTo(JCMediaManager.instance().mediaPlayer.getCurrentPosition());
            }
        }
        mTimer = new Timer();
        mTimer.schedule(timerTask, 50, 50);
        initDanmuConfig();

        if (isVIP == 1) {
            mJcVideoPlayer.text2.setVisibility(View.GONE);
            mJcVideoPlayer.text3.setVisibility(View.GONE);
        } else {
            mJcVideoPlayer.text2.setVisibility(View.VISIBLE);
            mJcVideoPlayer.text3.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 初始化配置
     */
    private void initDanmuConfig() {
        mDanmakuView = mJcVideoPlayer.getDanmuView();
        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5); // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        mContext = DanmakuContext.create();
        mIconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, getResources().getDisplayMetrics());
        mContext.setDanmakuBold(true);
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.2f)
                .setCacheStuffer(new ViewCacheStuffer<MyViewHolder>() {

                    @Override
                    public MyViewHolder onCreateViewHolder(int viewType) {
                        //Log.e("DFM", "onCreateViewHolder:" + viewType);
                        return new MyViewHolder(View.inflate(getApplicationContext(), R.layout.layout_view_cache, null));
                    }

                    @Override
                    public void onBindViewHolder(int viewType, MyViewHolder viewHolder, BaseDanmaku danmaku, AndroidDisplayer.DisplayerConfig displayerConfig, TextPaint paint) {
                        if (paint != null)
                            viewHolder.mText.getPaint().set(paint);
                        viewHolder.mText.setText(danmaku.text);
                        viewHolder.mText.setTextColor(danmaku.textColor);
                        viewHolder.mText.setTextSize(TypedValue.COMPLEX_UNIT_PX, danmaku.textSize);
                        Bitmap bitmap = null;
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare != null) {
                            bitmap = imageWare.bitmap;
                        }
                        if (bitmap != null) {
                            viewHolder.mIcon.setImageBitmap(bitmap);
                        } else {
                            viewHolder.mIcon.setImageResource(R.drawable.ic_user_avatar);
                        }
                    }

                    @Override
                    public void releaseResource(BaseDanmaku danmaku) {
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare != null) {
                            ImageLoader.getInstance().cancelDisplayTask(imageWare);
                        }
                        danmaku.setTag(null);
                        Log.e("DFM", "releaseResource url:" + danmaku.text);
                    }


                    @Override
                    public void prepare(BaseDanmaku danmaku, boolean fromWorkerThread) {
                        if (danmaku.isTimeOut()) {
                            return;
                        }
                        MyImageWare imageWare = (MyImageWare) danmaku.tag;
                        if (imageWare == null) {
                            String avatar = "";
                            imageWare = new MyImageWare(avatar, danmaku, mIconWidth, mIconWidth, mDanmakuView);
                            danmaku.setTag(imageWare);
                        }
                        if (danmaku.text.toString().contains("textview")) {
                            Log.e("DFM", "onAsyncLoadResource======>" + danmaku.tag + "url:" + imageWare.getImageUri());
                        }
                        ImageLoader.getInstance().displayImage(imageWare.getImageUri(), imageWare);
                    }

                }, null)
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);

        if (mDanmakuView != null) {
            mParser = createParser(this.getResources().openRawResource(R.raw.comments));
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
            mDanmakuView.prepare(mParser, mContext);
            mDanmakuView.showFPS(false);
            mDanmakuView.enableDanmakuDrawingCache(true);
        }
        timer = new Timer();
        timer.schedule(new AsyncAddTask(), 0, 5000);

        mJcVideoPlayer.closeDanmu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDanmakuView != null) {
                    if (mDanmakuView.isShown()) {
                        mDanmakuView.hide();
                        mJcVideoPlayer.closeDanmu.setImageResource(R.drawable.ic_show_danmu);
                    } else {
                        mDanmakuView.show();
                        mJcVideoPlayer.closeDanmu.setImageResource(R.drawable.ic_close_danmu);
                    }
                }
            }
        });
    }

    private void addDanmaKuShowTextAndImage(CommentInfo commentInfo) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        danmaku.text = commentInfo.getText();
        danmaku.setTag(new MyImageWare(commentInfo.getAvatar(), danmaku, mIconWidth, mIconWidth, mDanmakuView));
        danmaku.padding = 5;
        danmaku.priority = 0;  // 一定会显示, 一般用于本机发送的弹幕
        danmaku.isLive = false;
        danmaku.setTime(mDanmakuView.getCurrentTime());
        danmaku.textSize = 20f * (mParser.getDisplayer().getDensity() - 0.6f);
        danmaku.textColor = Color.WHITE;
        danmaku.textShadowColor = 0; // 重要：如果有图文混排，最好不要设置描边(设textShadowColor=0)，否则会进行两次复杂的绘制导致运行效率降低
        mDanmakuView.addDanmaku(danmaku);
    }

    private BaseDanmakuParser createParser(InputStream stream) {

        if (stream == null) {
            return new BaseDanmakuParser() {

                @Override
                protected Danmakus parse() {
                    return new Danmakus();
                }
            };
        }

        ILoader loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);

        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        BaseDanmakuParser parser = new BiliDanmukuParser();
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
        return parser;

    }


    class AsyncAddTask extends TimerTask {

        @Override
        public void run() {
            countTime++;
            for (int i = 4 * countTime; i < commentInfoList.size() && i < 4 * countTime + 4; i++) {
                SystemClock.sleep(500);
                addDanmaKuShowTextAndImage(commentInfoList.get(i));
            }
        }
    }

    private void initData() {
        Intent intent = getIntent();
        videoInfo = (VideoInfo) intent.getSerializableExtra(PARAM_VIDEO_INFO);
        currentTime = intent.getLongExtra(PARAM_CURRENT_TIME, 0);
        isVIP = intent.getIntExtra(PARAM_IS_VIP, 0);
        tryCount = intent.getIntExtra(PARAM_TRY_COUNT, 0);
        isSpeed = intent.getIntExtra(PARAM_IS_SPEED, 0);

        CURRENT_STATE = JCVideoPlayer.CURRENT_STATE_NORMAL;
        URL = videoInfo.getUrl();
        DIRECT_FULLSCREEN = true;
        VIDEO_PLAYER_CLASS = JCVideoPlayerStandard.class;
        try {
            commentInfoList = JSON.parseArray(SharedPreferencesUtil.getString(JCFullScreenActivity.this, PARAM_STR_COMMENT, "[]"), CommentInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            if (commentInfoList == null) {
                commentInfoList = new ArrayList<>();
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mDanmakuView != null) {
            mDanmakuView.release();
            mDanmakuView = null;
        }
        mJcVideoPlayer.backFullscreen();
    }


    @Override
    protected void onPause() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.pause();
        }
        super.onPause();
        JCVideoPlayer.releaseAllVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            mDanmakuView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mDanmakuView != null) {
            mDanmakuView.release();
            mDanmakuView = null;
        }
        ImageLoader.getInstance().destroy();
        super.onDestroy();
    }

    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            if (mJcVideoPlayer.mCurrentState == JCVideoPlayer.CURRENT_STATE_PLAYING) {
                //正在播放的时候需要发送信息
                Message message = new Message();
                mHandler.sendMessage(message);
            }
        }
    };

    boolean countDownFlag = false;

    class MyHandler extends Handler {
        WeakReference<Activity> mActivityReference;

        MyHandler(Activity activity) {
            mActivityReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mActivityReference.get();
            if (activity != null) {
                if (videoInfo.getVip() == 1 && isVIP == 0) {
                    //Vip视频
                    Intent intent = new Intent();
                    intent.putExtra(PARAM_CURRENT_TIME, mJcVideoPlayer.getCurrentPositionWhenPlaying());
                    intent.putExtra(PARAM_DIALOG_TYPE, DIALOG_TYPE_FULLVIDEO);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (isVIP == 0 && videoInfo.getVip() == 0 && tryCount <= VideoConfig.TRY_COUNT_TIME && mJcVideoPlayer.getCurrentPositionWhenPlaying() > 30000) {
                    //可以试看的视频 试看次数不够了
                    Intent intent = new Intent();
                    intent.putExtra(PARAM_CURRENT_TIME, mJcVideoPlayer.getCurrentPositionWhenPlaying());
                    intent.putExtra(PARAM_DIALOG_TYPE, DIALOG_TYPE_FULLVIDEO);
                    setResult(RESULT_OK, intent);
                    finish();
                } else if (isVIP == 0 && videoInfo.getVip() == 0 && tryCount > VideoConfig.TRY_COUNT_TIME) {
                    //可以试看的视频 试看次数不够了
                    Intent intent = new Intent();
                    intent.putExtra(PARAM_CURRENT_TIME, mJcVideoPlayer.getCurrentPositionWhenPlaying());
                    intent.putExtra(PARAM_DIALOG_TYPE, DIALOG_TYPE_FULLVIDEO);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    if (isSpeed == 0 && mJcVideoPlayer.getCurrentPositionWhenPlaying() > 40000) {//需要提示加速
                        mJcVideoPlayer.loadingProgressBar.setVisibility(View.VISIBLE);
                        JCMediaManager.instance().releaseMediaPlayer();
                        if (!countDownFlag) {
                            countDownFlag = true;
                            new CountDownTimer(8000, 8000) {

                                public void onTick(long millisUntilFinished) {

                                }

                                public void onFinish() {
                                    Intent intent = new Intent();
                                    intent.putExtra(PARAM_CURRENT_TIME, (long) mJcVideoPlayer.getCurrentPositionWhenPlaying());
                                    intent.putExtra(PARAM_DIALOG_TYPE, DIALOG_TYPE_SPEED);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }

                            }.start();
                        }
                    }
                }
            }
        }
    }
}

