package com.wyl.androidweb;

import android.os.Bundle;
import android.os.Message;
import android.webkit.WebView;

import com.sjjd.wyl.baseandroid.utils.Configs;
import com.sjjd.wyl.baseandroid.utils.LogUtils;
import com.sjjd.wyl.baseandroid.utils.SPUtils;
import com.wyl.androidweb.net.SocketManager2;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ContentActivity extends BaseWebActivity {

    @BindView(R.id.webContent)
    WebView mWebContent;
    SocketManager2 mSocketManager;


    String ip, port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        ButterKnife.bind(this);

        ip = SPUtils.init(mContext).getDIYString(Configs.SP_IP);
        port = SPUtils.init(mContext).getDIYString(Configs.SP_PORT);
        mSocketManager = SocketManager2.getInstance(mContext);
        mSocketManager.setHandler(mDataHandler);
        mSocketManager.startTcpConnection(ip, port);
        mSocketManager.getTcpSocket2().setPING(null);
    }


    @Override
    public void userHandler(Message msg) {
        super.userHandler(msg);
        switch (msg.what) {
            case Configs.MSG_SOCKET_RECEIVED:
                String path = msg.obj.toString();
                LogUtils.e(TAG, "userHandler: " + path);
                mWebContent.loadUrl("file://"+path);
                break;
        }
    }
}
