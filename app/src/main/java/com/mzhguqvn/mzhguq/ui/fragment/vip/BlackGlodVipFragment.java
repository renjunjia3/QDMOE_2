package com.mzhguqvn.mzhguq.ui.fragment.vip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.mzhguqvn.mzhguq.R;
import com.mzhguqvn.mzhguq.VideoDetailActivity;
import com.mzhguqvn.mzhguq.adapter.BlackGlodAdapter;
import com.mzhguqvn.mzhguq.app.App;
import com.mzhguqvn.mzhguq.base.BaseMainFragment;
import com.mzhguqvn.mzhguq.bean.VideoInfo;
import com.mzhguqvn.mzhguq.bean.VipInfo;
import com.mzhguqvn.mzhguq.util.API;
import com.mzhguqvn.mzhguq.util.DialogUtil;
import com.mzhguqvn.mzhguq.util.NetWorkUtils;
import com.mzhguqvn.mzhguq.util.SharedPreferencesUtil;
import com.tmall.ultraviewpager.UltraViewPager;
import com.tmall.ultraviewpager.transformer.UltraScaleTransformer;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.request.RequestCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;
import okhttp3.Call;
import wiki.scene.statuslib.StatusViewLayout;

/**
 * Case By:黑金会员专区
 * package:com.hfaufhreu.hjfeuio.ui.fragment.vip
 * Author：scene on 2017/4/17 10:17
 */

public class BlackGlodVipFragment extends BaseMainFragment {

    @BindView(R.id.statusViewLayout)
    StatusViewLayout statusViewLayout;
    @BindView(R.id.ultraViewPager)
    UltraViewPager ultraViewPager;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.score)
    MaterialRatingBar score;
    @BindView(R.id.update_number)
    TextView updateNumber;
    @BindView(R.id.update_time)
    TextView updateTime;

    private BlackGlodAdapter adapter;

    private RequestCall requestCall;

    public static BlackGlodVipFragment newInstance() {
        Bundle args = new Bundle();
        BlackGlodVipFragment fragment = new BlackGlodVipFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_black_glod_vip, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onLazyInitView(@Nullable Bundle savedInstanceState) {
        super.onLazyInitView(savedInstanceState);
        getBlackGloadVipData();
        uploadCurrentPage();
    }

    /**
     * Case By:上报当前页面
     * Author: scene on 2017/4/27 17:05
     */
    private void uploadCurrentPage() {
        Map<String, String> params = new HashMap<>();
        params.put("position_id","4");
        params.put("user_id", App.USER_ID+"");
        OkHttpUtils.post().url(API.URL_PRE+API.UPLOAD_CURRENT_PAGE).params(params).build().execute(null);
    }

    private void initView(final List<VideoInfo> list) {
        ultraViewPager.setScrollMode(UltraViewPager.ScrollMode.HORIZONTAL);
        adapter = new BlackGlodAdapter(getContext(), list);
        ultraViewPager.setAdapter(adapter);
        ultraViewPager.setMultiScreen(0.8f);
        ultraViewPager.setPageTransformer(false, new UltraScaleTransformer());
        title.setText(list.get(0).getTitle());
        score.setRating(list.get(0).getScore());

        ultraViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                title.setText(list.get(position).getTitle());
                score.setRating(list.get(position).getScore());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        adapter.setOnItemClickBlackGlodListener(new BlackGlodAdapter.OnItemClickBlackGlodListener() {
            @Override
            public void onItemClickBlackGlod(int position) {
                if (App.isVip < 5 && App.isHeijin != 1) {
                    DialogUtil.getInstance().showSubmitDialog(getContext(), false, "该片为黑金会员视频，请升级黑金会员后观看", App.isVip, true, true, list.get(position).getVideo_id(),false, 4);
                } else {
                    toVideoDetail(list.get(position));
                }
            }
        });
    }

    private void getBlackGloadVipData() {
        if (NetWorkUtils.isNetworkConnected(getContext())) {
            statusViewLayout.showLoading();
            requestCall = OkHttpUtils.get().url(API.URL_PRE + API.VIP_INDEX + "4").build();
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
                        SharedPreferencesUtil.putString(getContext(), "NOTIFY_DATA", s);
                        VipInfo vipInfo = JSON.parseObject(s, VipInfo.class);
                        if (vipInfo.getOther().get(0) != null && vipInfo.getOther().size() > 0
                                && vipInfo.getOther().get(0).getData() != null && vipInfo.getOther().get(0).getData().size() > 0) {
                            initView(vipInfo.getOther().get(0).getData());
                        }
                        statusViewLayout.showContent();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (statusViewLayout != null) {
                            statusViewLayout.showFailed(retryListener);
                        }
                    }
                }
            });

        } else {
            if (statusViewLayout != null) {
                statusViewLayout.showNetError(retryListener);
            }
        }
    }

    /**
     * Case By:跳转到视频详情页
     * Author: scene on 2017/4/19 9:33
     *
     * @param videoInfo 视频信息
     */
    private void toVideoDetail(VideoInfo videoInfo) {
        Intent intent = new Intent(_mActivity, VideoDetailActivity.class);
        intent.putExtra(VideoDetailActivity.ARG_VIDEO_INFO, videoInfo);
        intent.putExtra(VideoDetailActivity.ARG_IS_ENTER_FROM_TRY_SEE, false);
        _mActivity.startActivityForResult(intent, 9999);
    }


    @Override
    public void onDestroyView() {
        if (requestCall != null) {
            requestCall.cancel();
        }
        ultraViewPager.setAdapter(null);
        super.onDestroyView();
    }

    private View.OnClickListener retryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getBlackGloadVipData();
        }
    };
}
