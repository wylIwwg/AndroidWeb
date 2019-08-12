package com.wyl.androidweb.qr;

/**
 * Created by wyl on 2018/9/12.
 */
public class QrManager {
    static Vbar b;
    static QrManager mQrManager;
    boolean connected = false;
    private Object lock = new Object();
    public boolean isScaned = false;

    public static QrManager init() {
        if (b == null && mQrManager == null) {
            b = new Vbar();
            mQrManager = new QrManager();
        }
        return mQrManager;
    }

    public void b() {
        b.vbarBeep(100);
    }


    public boolean connect() {
        connected = b.vbarOpen("127.0.0.1", 0);

        if (connected) {
            b.vbarBacklight(true);//打开背光
            b.vbarAddSymbolType(1, true);//设置二维码
        }
        return connected = connected && b.vbarIsConnected();
    }

    public void closeDevice() {
        if (connected) {
            b.vbarBacklight(false);
        }
        b.vbarClose();
    }

    public boolean isConnected() {
        return connected;
    }


    public synchronized String scan() {
        synchronized (lock) {
            String str = b.vbarScan();
            if (str != null) {
                b();
            }
            return str;

        }

    }

}
