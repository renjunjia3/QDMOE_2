package com.mzhguqvn.mzhguq.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mzhguqvn.mzhguq.R;
import com.mzhguqvn.mzhguq.bean.VideoInfo;
import com.mzhguqvn.mzhguq.util.ScreenUtils;
import com.mzhguqvn.mzhguq.util.ViewUtils;
import com.mzhguqvn.mzhguq.video.JCUtils;

import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Case By:
 * package:
 * Author：scene on 2017/4/13 10:03
 */

public class IndexItemAdapter extends BaseAdapter {

    private Context context;
    private List<VideoInfo> list;

    private ScreenUtils screenUtils;
    private LayoutInflater inflater;
    private String colors[] = {"#3399FF", "#FF3300", "#00CC66", "#9966FF"};

    public IndexItemAdapter(Context context, List<VideoInfo> list) {
        this.context = context;
        this.list = list;

        screenUtils = ScreenUtils.instance(context);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.fragment_index_other_type_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final VideoInfo info = list.get(position);
        holder.name.setText(info.getTitle());
        holder.tag.setBackgroundColor(Color.parseColor(colors[new Random().nextInt(4)]));
        int height = (int) ((screenUtils.getScreenWidth() - screenUtils.dip2px(40f)) * 210f / 3f / 150f);
        ViewUtils.setViewHeightByViewGroup(holder.image, height);
        Glide.with(context).load(info.getThumb()).asBitmap().centerCrop().placeholder(R.drawable.bg_loading).error(R.drawable.bg_error).into(holder.image);
        holder.playTime.setText(info.getHits() + "次播放");
        holder.time.setText(JCUtils.stringForTime(info.getDuration()));
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.item_view)
        LinearLayout itemView;
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.tag)
        TextView tag;
        @BindView(R.id.play_time)
        TextView playTime;
        @BindView(R.id.time)
        TextView time;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }

}
