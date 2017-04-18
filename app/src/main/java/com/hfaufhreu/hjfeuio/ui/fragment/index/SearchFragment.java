package com.hfaufhreu.hjfeuio.ui.fragment.index;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.hfaufhreu.hjfeuio.R;
import com.hfaufhreu.hjfeuio.adapter.SearchAdapter;
import com.hfaufhreu.hjfeuio.app.App;
import com.hfaufhreu.hjfeuio.base.BaseFragment;
import com.hfaufhreu.hjfeuio.base.SearchInfo;
import com.hfaufhreu.hjfeuio.pay.PayUtil;
import com.hfaufhreu.hjfeuio.ui.dialog.FunctionPayDialog;
import com.liangfeizc.flowlayout.FlowLayout;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2017/3/24.
 */

public class SearchFragment extends BaseFragment {
    @BindView(R.id.listview)
    ListView listview;

    private FunctionPayDialog dialog;
    private FunctionPayDialog.Builder builder;

    private String[] tags;
    //标签
    private View tagView;
    private TextView tag;
    //内容
    private SearchAdapter adapter;
    private List<SearchInfo> lists;

    public static SearchFragment newInstance() {
        SearchFragment fragment = new SearchFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        unbinder = ButterKnife.bind(this, view);
        initDialog();
        tags = getResources().getStringArray(R.array.search_tag);
        return view;
    }

    @Override
    protected void onEnterAnimationEnd(Bundle savedInstanceState) {
        super.onEnterAnimationEnd(savedInstanceState);
        addHead();
        if (App.isVip == 0) {
            addFooter();
        }
        lists = JSON.parseArray(getString(R.string.str_json_seach), SearchInfo.class);
        adapter = new SearchAdapter(getContext(), lists);
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.show();
            }
        });
    }

    private void initDialog() {
        builder = new FunctionPayDialog.Builder(_mActivity);
        builder.setWeChatPayClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                PayUtil.getInstance().payByWeChat(_mActivity, 1, 0);
            }
        });

        builder.setAliPayClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                PayUtil.getInstance().payByAliPay(_mActivity, 1, 0);
            }
        });
        dialog = builder.create();
    }

    private void addHead() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.fragment_search_header_view, null);
        listview.addHeaderView(v);
        addTagView(v);
    }

    private void addFooter() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.fragment_search_footer_view, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });
        listview.addFooterView(v);
    }

    /**
     * Case By:加载tag
     * Author: scene on 2017/4/14 16:49
     */
    private void addTagView(View v) {
        FlowLayout flowLayout = (FlowLayout) v.findViewById(R.id.flow_layout);
        for (int i = 0; i < tags.length; i++) {
            tagView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_search_tag_item, null);
            tag = (TextView) tagView.findViewById(R.id.tag);
            tag.setText(tags[i]);
            final int finalI = i;
            tagView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    start(SearchResultFragment.newInstance(finalI));
                }
            });
            flowLayout.addView(tagView);
        }
    }

    @OnClick(R.id.search)
    public void onClickSearch() {
        dialog.show();
    }

    @OnClick(R.id.cancel)
    public void onClickCancel() {
        _mActivity.onBackPressed();
    }

}