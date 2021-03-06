package com.mzhguqvn.mzhguq.ui.fragment.magnet;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.mzhguqvn.mzhguq.R;
import com.mzhguqvn.mzhguq.adapter.SearchAdapter;
import com.mzhguqvn.mzhguq.app.App;
import com.mzhguqvn.mzhguq.base.BaseBackFragment;
import com.mzhguqvn.mzhguq.bean.SearchInfo;
import com.mzhguqvn.mzhguq.ui.dialog.DownLoadDialog;
import com.mzhguqvn.mzhguq.util.API;
import com.mzhguqvn.mzhguq.util.DialogUtil;
import com.mzhguqvn.mzhguq.util.GetAssestDataUtil;
import com.mzhguqvn.mzhguq.util.NetWorkUtils;
import com.mzhguqvn.mzhguq.util.ToastUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.request.RequestCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import okhttp3.Call;
import wiki.scene.statuslib.StatusViewLayout;

/**
 * Case By: 磁力链结果页
 * package:
 * Author：scene on 2017/4/18 11:43
 */
public class MagnetResultFragment extends BaseBackFragment implements SearchAdapter.OnClickDownloadListener {
    public static final String PARAMS_SEARCH_TAG_POSITION = "params_search_tag_position";
    public static final String PARAMS_SEARCH_TAG_KEYWORD = "params_search_tag_keyword";
    @BindView(R.id.listview)
    ListView listview;
    @BindView(R.id.search_layout)
    LinearLayout searchLayout;
    @BindView(R.id.statusViewLayout)
    StatusViewLayout statusViewLayout;
    @BindView(R.id.search)
    EditText search;

    //内容
    private SearchAdapter adapter;
    private List<SearchInfo> lists;

    //标签下标
    private int position = -1;
    private String keyWord = "";

    private RequestCall requestCall;

    //下载
    private DownLoadDialog downLoadDialog;
    private DownLoadDialog.Builder builder;
    private MaterialProgressBar progressBar;

    public static MagnetResultFragment newInstance(int position, String keyWord) {
        MagnetResultFragment fragment = new MagnetResultFragment();
        Bundle args = new Bundle();
        args.putInt(PARAMS_SEARCH_TAG_POSITION, position);
        args.putString(PARAMS_SEARCH_TAG_KEYWORD, keyWord);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            position = args.getInt(PARAMS_SEARCH_TAG_POSITION);
            keyWord = args.getString(PARAMS_SEARCH_TAG_KEYWORD);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        unbinder = ButterKnife.bind(this, view);
        return attachToSwipeBack(view);
    }

    @Override
    protected void onEnterAnimationEnd(Bundle savedInstanceState) {
        super.onEnterAnimationEnd(savedInstanceState);
        if (App.isVip == 0) {
            addFooter();
        }
        searchLayout.setBackgroundColor(getResources().getColor(R.color.black));

        if (position != -1) {
            statusViewLayout.showLoading();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lists = JSON.parseArray(GetAssestDataUtil.getAssestJson(getContext(), "search_tag_"+(position+1)+".json"), SearchInfo.class);
                    handler.sendEmptyMessage(0);
                }
            }).start();

        } else {
            lists = new ArrayList<>();
        }

        initDialog();
        uploadCurrentPage();
    }


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            adapter = new SearchAdapter(getContext(), lists);
            listview.setAdapter(adapter);
            adapter.setOnClickDownloadListener(MagnetResultFragment.this);
            statusViewLayout.showContent();
            if (position == -1) {
                getData();
            }
        }
    };

    /**
     * Case By:上报当前页面
     * Author: scene on 2017/4/27 17:05
     */
    private void uploadCurrentPage() {
        Map<String, String> params = new HashMap<>();
        params.put("position_id", "13");
        params.put("user_id", App.USER_ID + "");
        params.put("keyword", keyWord);
        OkHttpUtils.post().url(API.URL_PRE + API.UPLOAD_CURRENT_PAGE).params(params).build().execute(null);
    }

    private void initDialog() {
        builder = new DownLoadDialog.Builder(getContext());
        downLoadDialog = builder.create();
        progressBar = builder.getProgressBar();
    }

    private void addFooter() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.fragment_search_footer_view, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App.isVip == 0) {
                    DialogUtil.getInstance().showSubmitDialog(getContext(), false, "该功能为会员功能，请开通会员后使用", App.isVip, true, 13);
                } else if (App.isVip == 1) {
                    DialogUtil.getInstance().showSubmitDialog(getContext(), false, "该功能为钻石会员功能，请升级钻石会员后使用", App.isVip, true, 13);
                }
            }
        });
        listview.addFooterView(v);
    }

    @OnClick(R.id.btn_search)
    public void onClickSearch() {
        if (search.getText().toString().trim().isEmpty()) {
            ToastUtils.getInstance(getContext()).showToast("请输入你要搜索的关键词");
        } else {
            hideSoftInput();
            keyWord = search.getText().toString().trim();
            getData();
            uploadCurrentPage();
        }
    }

    @Override
    public void onDestroyView() {
        if (requestCall != null) {
            requestCall.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        listview.setAdapter(null);
        super.onDestroyView();
    }


    private void getData() {
        if (!NetWorkUtils.isNetworkConnected(getContext())) {
            statusViewLayout.showNetError(retryListener);
            return;
        }
        statusViewLayout.showLoading();
        requestCall = OkHttpUtils.get().url(API.URL_PRE + API.MAGNET + keyWord).build();
        requestCall.execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int i) {
                e.printStackTrace();
                if (statusViewLayout != null) {
                    statusViewLayout.showFailed(retryListener);
                }
            }

            @Override
            public void onResponse(String s, int i) {
                try {
                    lists.clear();
                    lists.addAll(JSON.parseArray(s, SearchInfo.class));
                    adapter.notifyDataSetChanged();
                    statusViewLayout.showContent();
                } catch (Exception e) {
                    e.printStackTrace();
                    statusViewLayout.showFailed(retryListener);
                }
            }
        });
    }

    private View.OnClickListener retryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getData();
        }
    };

    @Override
    public void onPause() {
        hideSoftInput();
        super.onPause();
    }

    /**
     * Case By:点击下载按钮
     * Author: scene on 2017/4/25 10:13
     */
    @Override
    public void onClickDownLoad(final int position) {
        if (progressBar.getProgress() != 0) {
            progressBar.setProgress(0);
        }
        if (downLoadDialog != null) {
            downLoadDialog.show();
        }
        new CountDownTimer(3000, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (progressBar != null) {
                    progressBar.setProgress(progressBar.getProgress() + 20 < 100 ? progressBar.getProgress() + 20 : 100);
                }
            }

            @Override
            public void onFinish() {
                try {
                    if (progressBar != null) {
                        progressBar.setProgress(100);
                    }
                    if (downLoadDialog != null && downLoadDialog.isShowing()) {
                        downLoadDialog.cancel();
                    }
                    lists.get(position).setShowPlay(true);
                    adapter.notifyDataSetChanged();
                    DialogUtil.getInstance().showCustomSubmitDialog(getContext(), "文件下载完毕，可以在线播放");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    /**
     * Case By:点击播放按钮
     * Author: scene on 2017/4/25 10:13
     */
    @Override
    public void onClickPlay() {
        if (App.isVip == 0) {
            DialogUtil.getInstance().showSubmitDialog(getContext(), false, "该栏目只对会员开放，请先开通会员", App.isVip, true, 13);
        } else if (App.isVip == 1) {
            DialogUtil.getInstance().showSubmitDialog(getContext(), false, "您的会员权限不足，请先升级钻石会员", App.isVip, true, 13);
        }
    }
}
