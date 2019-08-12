package com.wyl.androidweb.qr;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

/**
 * Created by wyl on 2018/4/24.
 */

public class QRThread<T> extends Thread {

    public String TAG = this.getClass().getSimpleName();
    public boolean loop = true;//是否循环
    public boolean pause = false;//是否暂停


    public long sleep_time = 2000;//默认2秒请求一次
    public int call_times = 0;//网络状态异常请求次数

    Handler mHandler;//通知线程

    private Object lock = new Object();

    public void pause() {
        pause = true;
    }

    public void OnResume() {
        synchronized (lock) {
            pause = false;
            lock.notify();
        }
    }

    public synchronized void onPause() {
        synchronized (lock) {
            pause = true;
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void run() {

        while (loop) {
            if (pause) {//是否阻塞线程

            } else {
                initData();//操作请求
            }
            SystemClock.sleep(sleep_time);//沉睡sleep time
        }
    }

    public void onDestroy() {
        call_times = 0;
        pause = true;
        loop = false;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private int what;

    public QRThread(Handler handler, int what) {
        this.what = what;
        mHandler = handler;
    }


    private synchronized void initData() {
        synchronized (lock) {
            final String str = QrManager.init().scan();
            if (str != null) {

                Message msg = Message.obtain();
                msg.what = what;
                msg.obj = str;
                if (mHandler != null)
                    mHandler.sendMessage(msg);

                onPause();

            }
        }

    }
}
