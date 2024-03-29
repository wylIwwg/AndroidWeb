package com.wyl.androidweb.net;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.sjjd.wyl.baseandroid.socket.OnConnectionStateListener;
import com.sjjd.wyl.baseandroid.socket.OnMessageReceiveListener;
import com.sjjd.wyl.baseandroid.socket.UDPSocket;
import com.sjjd.wyl.baseandroid.utils.Configs;
import com.sjjd.wyl.baseandroid.utils.LogUtils;
import com.wyl.androidweb.HandlerMessage;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by wyl on 2018/11/22.
 */
public class SocketManager2 {
    private static volatile SocketManager2 instance = null;
    private static String TAG = "SocketManager";
    private UDPSocket udpSocket;
    private TcpSocket2 TcpSocket2;
    private Context mContext;
    private Handler mHandler;
    private String IP;
    private String PORT;
    private int delayRequest = 5000;

    public void setDelayRequest(int delayRequest) {
        this.delayRequest = delayRequest;
    }

    private SocketManager2(Context context) {
        mContext = context.getApplicationContext();
    }

    public static SocketManager2 getInstance(Context context) {
        if (instance == null) {
            synchronized (SocketManager2.class) {
                if (instance == null) {
                    instance = new SocketManager2(context);
                }
            }
        }
        return instance;
    }

    public SocketManager2 setHandler(Handler handler) {
        mHandler = handler;
        return this;
    }

    public void startUdpConnection() {

        if (udpSocket == null) {
            udpSocket = new UDPSocket(mContext);
        }

        // 注册接收消息的接口
        udpSocket.addOnMessageReceiveListener(new OnMessageReceiveListener() {
            @Override
            public void onMessageReceived(String message) {
                handleUdpMessage(message);
            }
        });

        udpSocket.startUDPSocket();

    }

    public TcpSocket2 getTcpSocket2() {
        return TcpSocket2;
    }

    /**
     * 处理 udp 收到的消息
     *
     * @param message
     */
    private void handleUdpMessage(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            String ip = jsonObject.optString(IP);
            String port = jsonObject.optString(PORT);
            if (!TextUtils.isEmpty(ip) && !TextUtils.isEmpty(port)) {
                startTcpConnection(ip, port);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始 TCP 连接
     *
     * @param ip
     * @param port
     */
    public synchronized void startTcpConnection(String ip, String port) {
        IP = ip;
        PORT = port;
        TcpSocket2 = new TcpSocket2(mContext);

        TcpSocket2.startTcpSocket(ip, port);
        if (TcpSocket2 != null) {
            TcpSocket2.setOnConnectionStateListener(new OnConnectionStateListener() {
                @Override
                public void onSuccess() {// tcp 创建成功
                    //udpSocket.stopHeartbeatTimer();
                    LogUtils.e(TAG, "onSuccess: tcp 创建成功");
                    mHandler.sendEmptyMessage(HandlerMessage.MSG_CREATE_SOCKET_SUCCESS);
                }

                @Override
                public void onFailed(int errorCode) {// tcp 异常处理

                    switch (errorCode) {
                        case Configs.MSG_CREATE_TCP_ERROR:
                            LogUtils.e(TAG, "onFailed: 连接失败");
                            TcpSocket2 = null;
                            if (mHandler != null) {
                                mHandler.sendEmptyMessage(Configs.MSG_CREATE_TCP_ERROR);
                                //延迟时间去连接
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startTcpConnection(IP, PORT);
                                    }
                                }, delayRequest);
                            }
                            break;
                        case Configs.MSG_PING_TCP_TIMEOUT:
                            LogUtils.e(TAG, "onFailed: 连接超时");
                            TcpSocket2 = null;
                            if (mHandler != null) {
                                mHandler.sendEmptyMessage(Configs.MSG_PING_TCP_TIMEOUT);
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startTcpConnection(IP, PORT);
                                    }
                                }, delayRequest);
                            }
                            break;
                    }
                }
            });
            TcpSocket2.addOnMessageReceiveListener(new OnMessageReceiveListener() {
                @Override
                public void onMessageReceived(String message) {
                    if (mHandler != null) {
                        Message msg = Message.obtain();
                        msg.what = Configs.MSG_SOCKET_RECEIVED;
                        msg.obj = message;
                        mHandler.sendMessage(msg);
                    } else {
                        LogUtils.e(TAG, "onMessageReceived: " + message);
                    }
                }
            });
        }


    }

    public void destroy() {
        stopSocket();
        instance = null;
    }

    public void stopSocket() {

        if (udpSocket != null) {
            udpSocket.stopHeartbeatTimer();
            udpSocket.stopUDPSocket();
            udpSocket = null;
        }
        if (TcpSocket2 != null) {
            TcpSocket2.stopHeartbeatTimer();
            TcpSocket2.stopTcpConnection();
            TcpSocket2 = null;
        }
    }
}
