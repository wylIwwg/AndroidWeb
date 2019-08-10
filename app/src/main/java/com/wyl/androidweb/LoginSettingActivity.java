package com.wyl.androidweb;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.sjjd.wyl.baseandroid.base.BaseDataHandler;
import com.sjjd.wyl.baseandroid.utils.Configs;
import com.sjjd.wyl.baseandroid.utils.SPUtils;
import com.wyl.androidweb.net.SocketManager2;
import com.wyl.androidweb.view.MEditView;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.dmoral.toasty.Toasty;

/**
 * 配置登录的活动，用于和服务器保持连接
 */
public class LoginSettingActivity extends AppCompatActivity implements BaseDataHandler.ErrorListener {

    SocketManager2 mSocketManager;
    @BindView(R.id.etServerIp)
    MEditView mEtServerIp;
    @BindView(R.id.etServerPort)
    MEditView mEtServerPort;
    @BindView(R.id.btnConnectServer)
    Button mBtnConnectServer;
    @BindView(R.id.etUser)
    MEditView mEtUser;
    @BindView(R.id.etPsw)
    MEditView mEtPsw;
    @BindView(R.id.cbPswVisible)
    CheckBox mCbPswVisible;
    @BindView(R.id.imgLoading)
    ImageView mImgLoading;
    @BindView(R.id.tvLoadingTips)
    TextView mTvLoadingTips;
    @BindView(R.id.rlLoadingRoot)
    RelativeLayout mRlLoadingRoot;

    Context mContext;

    BaseDataHandler mDataHandler;
    String[] PERMISSIONS = new String[]{com.yanzhenjie.permission.runtime.Permission.WRITE_EXTERNAL_STORAGE,
            com.yanzhenjie.permission.runtime.Permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_setting);
        ButterKnife.bind(this);

        mContext = this;
        mDataHandler = new BaseDataHandler(this);
        mDataHandler.setErrorListener(this);

        AndPermission.with(mContext)
                .runtime()
                .permission(PERMISSIONS)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {

                    }
                }).onDenied(new Action<List<String>>() {
            @Override
            public void onAction(List<String> data) {

            }
        }).start();
    }

    @OnClick(R.id.btnConnectServer)
    public void onViewClicked() {
        String ip = mEtServerIp.getText().toString();
        String port = mEtServerPort.getText().toString();

        if (TextUtils.isEmpty(ip)) {
            Toasty.error(mContext, "请输入服务器ip", Toast.LENGTH_LONG, true).show();
            return;
        }

        if (TextUtils.isEmpty(port)) {
            Toasty.error(mContext, "请输入服务器端口", Toast.LENGTH_LONG, true).show();
            return;
        }


        mRlLoadingRoot.setVisibility(View.VISIBLE);
        mRlLoadingRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        Glide.with(mContext)
                .load(R.drawable.loading)
                .asGif()
                .into(mImgLoading);
        mTvLoadingTips.setText("连接服务器中...");

        mSocketManager = SocketManager2.getInstance(mContext);
        mSocketManager.setHandler(mDataHandler);
        mSocketManager.startTcpConnection(ip, port);
        mSocketManager.getTcpSocket2().setPING(null);
        SPUtils.init(mContext).putDIYString(Configs.SP_IP, ip);
        SPUtils.init(mContext).putDIYString(Configs.SP_PORT, port);


    }

    @Override
    public void showError(String error) {

    }

    @Override
    public void userHandler(Message msg) {
        switch (msg.what) {
            case HandlerMessage.MSG_CREATE_SOCKET_SUCCESS:

                Intent mIntent = new Intent(mContext, ContentActivity.class);
                startActivity(mIntent);
                mSocketManager.destroy();
                this.finish();

                break;
        }
    }
}
