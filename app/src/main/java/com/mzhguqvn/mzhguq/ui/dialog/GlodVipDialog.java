package com.mzhguqvn.mzhguq.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mzhguqvn.mzhguq.R;
import com.mzhguqvn.mzhguq.pay.PayUtil;
import com.mzhguqvn.mzhguq.util.ScreenUtils;
import com.mzhguqvn.mzhguq.util.ViewUtils;


/**
 * Case By:开通黄金VIP
 * package:
 * Author：scene on 2017/4/18 13:53
 */
public class GlodVipDialog extends Dialog {
    public GlodVipDialog(@NonNull Context context) {
        super(context);
    }

    public GlodVipDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    protected GlodVipDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void show() {
        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (ScreenUtils.instance(getContext()).getScreenWidth() * 0.95f); // 宽度
        dialogWindow.setAttributes(lp);
        super.show();
    }

    public static class Builder {
        private Context context;
        private int videoId;
        private boolean isVideoDetailPage;

        private int type = 1;
        private int vip_type = 1;

        public Builder(Context context, int videoId, boolean isVideoDetailPage) {
            this.context = context;
            this.videoId = videoId;
            this.isVideoDetailPage = isVideoDetailPage;
        }

        public GlodVipDialog create() {
            LayoutInflater inflater = LayoutInflater.from(context);
            final GlodVipDialog dialog = new GlodVipDialog(context, R.style.Dialog);
            View layout = inflater.inflate(R.layout.dialog_glod_vip, null);
            ((TextView) layout.findViewById(R.id.glod_old_price)).getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            ((TextView) layout.findViewById(R.id.diamond_old_price)).getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            dialog.addContentView(layout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            final ImageView glodChoosed = (ImageView) layout.findViewById(R.id.glod_choosed);
            final ImageView diamondChoosed = (ImageView) layout.findViewById(R.id.diamond_choosed);
            ImageView image = (ImageView) layout.findViewById(R.id.image);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.height = (int) ((ScreenUtils.instance(context).getScreenWidth() - ScreenUtils.instance(context).dip2px(50)) * 3f / 5f);
            image.setLayoutParams(layoutParams);
            layout.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            layout.findViewById(R.id.layout_type_glod).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vip_type = PayUtil.VIP_TYPE_1;
                    glodChoosed.setImageResource(R.drawable.ic_vip_type_choosed);
                    diamondChoosed.setImageResource(R.drawable.ic_vip_type_unchoosed);
                }
            });
            layout.findViewById(R.id.layout_type_diamond).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vip_type = PayUtil.VIP_TYPE_3;
                    glodChoosed.setImageResource(R.drawable.ic_vip_type_unchoosed);
                    diamondChoosed.setImageResource(R.drawable.ic_vip_type_choosed);
                }
            });
            RadioGroup radioGroup = (RadioGroup) layout.findViewById(R.id.radio_group);
            if (radioGroup.getCheckedRadioButtonId() == R.id.type_wechat) {
                type = 1;
            } else {
                type = 2;
            }
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {

                    if (checkedId == R.id.type_wechat) {
                        type = 1;
                    } else {
                        type = 2;
                    }
                }
            });
            layout.findViewById(R.id.open_vip).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (type == 1) {
                        PayUtil.getInstance().payByWeChat(context, dialog, vip_type, videoId, isVideoDetailPage);
                    } else {
                        PayUtil.getInstance().payByAliPay(context, dialog, vip_type, videoId, isVideoDetailPage);
                    }

                }
            });

            ViewUtils.setViewHeightByViewGroup(layout, (int) (ScreenUtils.instance(context).getScreenWidth() * 0.9f));
            dialog.setContentView(layout);
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

}
