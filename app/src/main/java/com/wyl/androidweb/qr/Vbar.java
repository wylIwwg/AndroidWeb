package com.wyl.androidweb.qr;

import android.util.Log;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

public class Vbar {
    public interface Vdll extends Library {

        Vdll INSTANCE = (Vdll) Native.loadLibrary("vbar", Vdll.class);

        public IntByReference vbar_scanner_create();

        public boolean vbar_scanner_is_connect(IntByReference scanner);

        //背光控制
        public boolean vbar_scanner_led(IntByReference scanner, boolean on);

        //蜂鸣器控制
        public boolean vbar_scanner_beep(IntByReference scanner, int time);

        //添加要支持的码制
        public boolean vbar_scanner_add_type(IntByReference scanner, int type, boolean enable);

        //扫码
        public int vbar_scanner_decode(IntByReference scanner, IntByReference symbol_type, byte[] result_buffer, IntByReference result_size);

        //
        public int vbar_scanner_destroy(IntByReference scanner);


    }

    //初始化设备变量
    public static IntByReference device = null;
    byte[] result_buffer = new byte[1024];
    IntByReference symbol_type = new IntByReference(256);
    IntByReference result_size = new IntByReference(1024);

    //打开设备
    public boolean vbarOpen(String addr, long parm) {
        if (device == null) {
            device = Vdll.INSTANCE.vbar_scanner_create();
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (device != null) {
            Log.e("vguang ", "open success");
            return true;
        } else {
            return false;
        }
    }

    //蜂鸣器控制
    public boolean vbarBeep(int times) {
        if (Vdll.INSTANCE.vbar_scanner_is_connect(device)) {
            if (Vdll.INSTANCE.vbar_scanner_beep(device, times)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //背光控制
    public boolean vbarBacklight(boolean bool) {
        if (Vdll.INSTANCE.vbar_scanner_is_connect(device)) {
            if (Vdll.INSTANCE.vbar_scanner_led(device, bool)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }


    //判断设备是否已连接
    public boolean vbarIsConnected() {
        if (Vdll.INSTANCE.vbar_scanner_is_connect(device)) {
            return true;
        } else {
            return false;
        }
    }

    //关闭设备
    public void vbarClose() {
        Vdll.INSTANCE.vbar_scanner_destroy(device);
        device = null;

    }

    //添加要支持的码制
    // 条码符号类别ID定义
//			#define VBAR_SYM_NONE           0   /* 空类型, 用于清空 */
//			#define VBAR_SYM_QRCODE         1
//			#define VBAR_SYM_EAN8           2
//			#define VBAR_SYM_EAN13          3
//			#define VBAR_SYM_ISBN13         4
//			#define VBAR_SYM_CODE39         5
//			#define VBAR_SYM_CODE93         6
//			#define VBAR_SYM_CODE128        7
//			#define VBAR_SYM_DATABAR        8
//			#define VBAR_SYM_DATABAR_EXP    9
//			#define VBAR_SYM_PDF417         10
//			#define VBAR_SYM_DATAMATRIX     11
//			#define VBAR_SYM_ITF            12
//			#define VBAR_SYM_ISBN10         13
//			#define VBAR_SYM_UPCE           14
//			#define VBAR_SYM_UPCA           15
    public void vbarAddSymbolType(int symbol_type, boolean on) {
        boolean state = Vdll.INSTANCE.vbar_scanner_add_type(device, symbol_type, on);
        if (state) {
            Log.v("######################", "control success");
        } else {
            Log.v("######################", "control fail");
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //扫码
    public String vbarScan() {
        String decode = null;
        if (Vdll.INSTANCE.vbar_scanner_is_connect(device)) {
            if (Vdll.INSTANCE.vbar_scanner_decode(device, symbol_type, result_buffer, result_size) == 0) {
                decode = new String(result_buffer, 0, result_size.getValue());
                return decode;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}



	
	

