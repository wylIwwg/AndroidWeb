package com.wyl.androidweb.net;

/**
 * Created by wyl on 2019/8/10.
 */

import android.content.Context;
import android.os.Environment;

import com.sjjd.wyl.baseandroid.socket.HeartbeatTimer;
import com.sjjd.wyl.baseandroid.socket.OnConnectionStateListener;
import com.sjjd.wyl.baseandroid.socket.OnMessageReceiveListener;
import com.sjjd.wyl.baseandroid.utils.Configs;
import com.sjjd.wyl.baseandroid.utils.FileUtils;
import com.sjjd.wyl.baseandroid.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by wyl on 2018/11/22.
 */
public class TcpSocket2 {
    private static final String TAG = " TCPSocket ";
    private static String LABEL = "\n";
    private ExecutorService mThreadPool;
    private Socket mSocket;
    private BufferedReader br;
    private PrintWriter pw;
    private HeartbeatTimer timer;
    private long lastReceiveTime = 0;
    private Context mContext;
    private String PING = "";
    private final Object mObject = new Object();
    private OnConnectionStateListener mListener;
    private OnMessageReceiveListener mMessageListener;
    private static final long TIME_OUT = 15 * 1000;
    private static final long HEARTBEAT_RATE = 5 * 1000;
    private static final long HEARTBEAT_MESSAGE_DURATION = 2 * 1000;
    private boolean alive = false;

    private MsgThread mMsgThread;

    public TcpSocket2(Context context) {
        mContext = context;
        int cpuNumbers = Runtime.getRuntime().availableProcessors();
        // 根据CPU数目初始化线程池
        mThreadPool = Executors.newFixedThreadPool(cpuNumbers * Configs.POOL_SIZE);
        // 记录创建对象时的时间
        lastReceiveTime = System.currentTimeMillis();
        mMsgThread = new MsgThread();

    }

    public void setPING(String PING) {
        this.PING = PING;
    }

    public void startTcpSocket(final String ip, final String port) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (startTcpConnection(ip, Integer.valueOf(port))) {// 尝试建立 TCP 连接
                    if (mListener != null) {
                        mListener.onSuccess();
                    }
                    alive = true;
                    startReceiveTcpThread();//TCP创建成功 开启数据接收线程
                    if (PING != null)
                        startHeartbeatTimer();//发送心跳
                } else {
                    if (mListener != null) {
                        alive = false;
                        stopTcpConnection();//创建失败 关闭资源
                        mListener.onFailed(Configs.MSG_CREATE_TCP_ERROR);
                    }
                }
            }
        });
    }

    public void setOnConnectionStateListener(OnConnectionStateListener listener) {
        this.mListener = listener;
    }

    public void addOnMessageReceiveListener(OnMessageReceiveListener listener) {
        mMessageListener = listener;
    }

    /**
     * 创建接收线程
     */
    class MsgThread implements Runnable {

        @Override
        public void run() {
            while (alive) {
                String line = "";
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                byte[] buffer = new byte[1024 * 1024 * 10];//缓存接收10M以下的数据
                byte[] bufferTag = new byte[10];
                try {
                    if (mSocket == null) {
                        continue;
                    }
                    //  String message = "";
                    if (mSocket.isClosed()) {
                        continue;
                    }
                    InputStream is = mSocket.getInputStream();
                    synchronized (mObject) {
                        int index = 0;
                        String contentLength = "0";
                        int tag = 0;
                        while (alive && mSocket != null && !mSocket.isClosed() && !mSocket.isInputShutdown()
                                ) {
                            int receiveLength = 0;

                            if (alive && mSocket != null && !mSocket.isClosed() && mSocket.isConnected()) {

                                if (index < 1) {
                                    receiveLength = is.read(bufferTag);
                                    for (int i = 0; i < bufferTag.length; i++) {
                                        if (i == 0) {
                                            tag = bufferTag[i];
                                        } else {
                                            //计算长度
                                            if (bufferTag[i] > 0)
                                                contentLength += (char) bufferTag[i];
                                        }
                                    }
                                } else {
                                    receiveLength = is.read(buffer, index, 1024);
                                }
                                if (receiveLength > 0) {
                                    index += receiveLength;
                                } else {
                                    index = 0;
                                }
                                if (contentLength.length() < Integer.MAX_VALUE && (index - 10 == Integer.parseInt(contentLength))) {
                                    LogUtils.e(TAG, "run: 已经读取到末尾  " + contentLength);
                                    handleReceiveTcpMessage(tag, buffer, index);
                                    index = 0;
                                    contentLength = "0";
                                    tag = 0;
                                }
                            }
                        }
                    }

                } catch (IOException e) {
                    //e.printStackTrace();
                }

            }
        }
    }

    private void handleReceiveTcpMessage(int tag, byte[] buffer, int length) throws UnsupportedEncodingException {
        if (tag <= 0) {
            handleReceiveTcpMessage(buffer);
        } else {
            byte[] content = Arrays.copyOfRange(buffer, 10, length);
            switch (tag) {
                case 1://字符串
                    String result = new String(content, "utf-8");
                    LogUtils.e(TAG, "handleReceiveTcpMessage: " + result);
                    break;
                case 2://txt文件内容
                    break;
                case 3://zip压缩文件
                    break;
                case 4://html文件
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + System.currentTimeMillis() + ".html");
                    String html = new String(content, "utf-8");
                    LogUtils.e(TAG, "handleReceiveTcpMessage: " + html);
                    FileUtils.byte2File(file.getPath(), html.getBytes());
                    if (mMessageListener != null) {
                        mMessageListener.onMessageReceived(file.getPath());
                    }
                    break;
            }
        }

    }


    public static void byte2File(String path, byte[] source) {
        try {
            File mFile = new File(path);
            if (mFile.exists()) {
                mFile.delete();
            }
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayInputStream bais = new ByteArrayInputStream(source);

            int len;
            byte[] b = new byte[1024];
            while ((len = bais.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            fos.flush();
            fos.close();
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleReceiveTcpMessage(byte[] buffer) {
        if (buffer != null && buffer.length > 0) {
            LogUtils.e(TAG, "handleReceiveTcpMessage: " + buffer.length);
            LogUtils.e(TAG, "handleReceiveTcpMessage: " + buffer[0]);
        }
    }

    private void startReceiveTcpThread() {

        if (mMsgThread != null)
            mThreadPool.execute(mMsgThread);
    }

    /**
     * 处理 tcp 收到的消息
     *
     * @param line
     */
    String result = "";
    String remainder = "";

    private void handleReceiveTcpMessage(String line) {
        LogUtils.e(TAG, "\n接收 tcp 消息：" + line + "\n");
        lastReceiveTime = System.currentTimeMillis();

        if (line.contains("pong")) {
            return;
        }
       /* if (line.contains(LABEL)) {
            int mIndex = line.indexOf(LABEL);//获取标识符索引

            result += line.replace(LABEL, "");
            //result += line.substring(0, mIndex);//获取标识符前段字符
            // remainder = line.substring(mIndex, line.length() - 1);//获取标识符后段字符
            LogUtils.e(TAG, "找到标识符 : " + result);
            if (result.startsWith("{") && result.endsWith("}")) {
                if (mMessageListener != null) {
                    mMessageListener.onMessageReceived(result);
                    result = "";
                }
            }

        } else {
            result += line;
            LogUtils.e(TAG, "handleReceiveTcpMessage: " + result);
        }*/


    }

    public void sendTcpMessage(String json) {
        if (pw != null)
            pw.println(json);
        LogUtils.e(TAG, "tcp 消息发送成功..." + json);
    }

    /**
     * 启动心跳
     */
    private void startHeartbeatTimer() {
        if (timer == null) {
            timer = new HeartbeatTimer();
        }
        timer.setOnScheduleListener(new HeartbeatTimer.OnScheduleListener() {
            @Override
            public void onSchedule() {
                long duration = System.currentTimeMillis() - lastReceiveTime;
                LogUtils.e(TAG, "timer is onSchedule..." + " duration:" + duration);
                if (duration > TIME_OUT) {//若超过十五秒都没收到我的心跳包，则认为对方不在线。
                    LogUtils.e(TAG, "tcp ping 超时， 断开连接");
                    stopTcpConnection();
                    if (mListener != null) {
                        alive = false;
                        mListener.onFailed(Configs.MSG_PING_TCP_TIMEOUT);
                    }
                } else if (duration > HEARTBEAT_MESSAGE_DURATION) {//若超过两秒他没收到我的心跳包，则重新发一个。
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put(Configs.TYPE, Configs.PING);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendTcpMessage(PING);
                    //sendTcpMessage(jsonObject.toString());
                }
            }

        });
        timer.startTimer(0, HEARTBEAT_RATE);
    }

    public void stopHeartbeatTimer() {
        if (timer != null) {
            timer.exit();
            timer = null;
        }
    }

    /**
     * 尝试建立tcp连接
     *
     * @param ip
     * @param port
     */
    private boolean startTcpConnection(final String ip, final int port) {
        try {
            if (mSocket == null) {
                mSocket = new Socket(ip, port);
                mSocket.setKeepAlive(true);
                mSocket.setTcpNoDelay(true);
                mSocket.setReuseAddress(true);
            }
            InputStream is = mSocket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            OutputStream os = mSocket.getOutputStream();
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)), true);
            LogUtils.e(TAG, "tcp 创建成功...");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtils.e(TAG, "tcp 创建失败...");
        return false;
    }

    public void stopTcpConnection() {
        try {
            alive = false;
            stopHeartbeatTimer();
            if (br != null) {
                br.close();
            }
            if (pw != null) {
                pw.close();
            }
            if (mThreadPool != null) {
                mThreadPool.shutdown();
                mThreadPool = null;
            }

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }

            if (mMsgThread != null) {
                mMsgThread = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
