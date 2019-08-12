package com.wyl.androidweb.print;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;

import com.printsdk.cmd.PrintCmd;
import com.printsdk.usbsdk.UsbDriver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;


public class PrintUtils {

    private static final String ACTION_USB_PERMISSION = "com.usb.sample.USB_PERMISSION";


    private int cutter = 0;       // 默认0，  0 全切、1 半切
    private static Context mContext;
    static UsbDriver mUsbDriver;
    static UsbDevice mUsbDev1;        //打印机
    static UsbDevice mUsbDev2;        //打印机
    public UsbManager mUsbManager;
    static PrintUtils mPrintUtils;

    public PrintUtils() {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mUsbDriver = new UsbDriver(mUsbManager, mContext);
        PendingIntent permissionIntent1 = PendingIntent.getBroadcast(mContext, 0,
                new Intent(ACTION_USB_PERMISSION), 0);
        mUsbDriver.setPermissionIntent(permissionIntent1);
        // Broadcast listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mUsbReceiver, filter);

        /*mUsbDriver= PrintReceiver.mUsbDriver;
        mUsbDev1=PrintReceiver.mUsbDev1;        //打印机
        mUsbDev2=PrintReceiver.mUsbDev2;        //打印机
*/

    }

    public static PrintUtils init(Context context) {
        mContext = context;
        if (mPrintUtils == null) {
            mPrintUtils = new PrintUtils();
        }

        return mPrintUtils;
    }


    public void unRegisterReceiver() {
        if (mUsbReceiver != null && mContext != null) {
            mContext.unregisterReceiver(mUsbReceiver);
        }
    }


    /*
     *  BroadcastReceiver when insert/remove the device USB plug into/from a USB port
     *  创建一个广播接收器接收USB插拔信息：当插入USB插头插到一个USB端口，或从一个USB端口，移除装置的USB插头
     */
    static BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (mUsbDriver.usbAttached(intent)) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if ((device.getProductId() == 8211 && device.getVendorId() == 1305)
                            || (device.getProductId() == 8213 && device
                            .getVendorId() == 1305)) {
                        if (mUsbDriver.openUsbDevice(device)) {
                            if (device.getProductId() == 8211)
                                mUsbDev1 = device;
                            else
                                mUsbDev2 = device;
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent
                        .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if ((device.getProductId() == 8211 && device.getVendorId() == 1305)
                        || (device.getProductId() == 8213 && device
                        .getVendorId() == 1305)) {
                    mUsbDriver.closeUsbDevice(device);
                    if (device.getProductId() == 8211)
                        mUsbDev1 = null;
                    else
                        mUsbDev2 = null;
                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if ((device.getProductId() == 8211 && device.getVendorId() == 1305)
                                || (device.getProductId() == 8213 && device
                                .getVendorId() == 1305)) {
                            if (mUsbDriver.openUsbDevice(device)) {
                                if (device.getProductId() == 8211)
                                    mUsbDev1 = device;
                                else
                                    mUsbDev2 = device;
                            }
                        }
                    } else {
                    }
                }
            }
        }
    };

    // Get UsbDriver(UsbManager) service
    public boolean PrintConnStatus() {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mUsbDriver = new UsbDriver(mUsbManager, mContext);
        boolean blnRtn = false;
        try {
            if (!mUsbDriver.isConnected()) {
//				UsbManager m_UsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                // USB线已经连接
                for (UsbDevice device : mUsbManager.getDeviceList().values()) {
                    if ((device.getProductId() == 8211 && device.getVendorId() == 1305)
                            || (device.getProductId() == 8213 && device
                            .getVendorId() == 1305)) {
                        blnRtn = mUsbDriver.usbAttached(device);
                        if (blnRtn == false)
                            break;
                        blnRtn = mUsbDriver.openUsbDevice(device);
                        // 打开设备
                        if (blnRtn) {
                            if (device.getProductId() == 8211)
                                mUsbDev1 = device;
                            else
                                mUsbDev2 = device;
                            break;
                        } else {
                            break;
                        }
                    }
                }
            } else {
                blnRtn = true;
            }
        } catch (Exception e) {
        }
        return blnRtn;
    }

    /**
     * 获取当前系统的语言环境
     *
     * @param context
     * @return boolean
     */
    public static boolean isZh(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    // 检测打印机状态
    public int getPrinterStatus() {
        int iRet = -1;
        if (mUsbDev1 == null)
            return iRet;
        UsbDevice usbDev = mUsbDev1;

        byte[] bRead1 = new byte[1];
        byte[] bWrite1 = PrintCmd.GetStatus1();
        if (mUsbDriver.read(bRead1, bWrite1, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus1(bRead1[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead2 = new byte[1];
        byte[] bWrite2 = PrintCmd.GetStatus2();
        if (mUsbDriver.read(bRead2, bWrite2, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus2(bRead2[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead3 = new byte[1];
        byte[] bWrite3 = PrintCmd.GetStatus3();
        if (mUsbDriver.read(bRead3, bWrite3, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus3(bRead3[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead4 = new byte[1];
        byte[] bWrite4 = PrintCmd.GetStatus4();
        if (mUsbDriver.read(bRead4, bWrite4, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus4(bRead4[0]);
        }

        return iRet;
    }


    // 检测打印机状态
    public int getPrinterStatus(UsbDevice usbDev) {
        int iRet = -1;

        byte[] bRead1 = new byte[1];
        byte[] bWrite1 = PrintCmd.GetStatus1();
        if (mUsbDriver.read(bRead1, bWrite1, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus1(bRead1[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead2 = new byte[1];
        byte[] bWrite2 = PrintCmd.GetStatus2();
        if (mUsbDriver.read(bRead2, bWrite2, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus2(bRead2[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead3 = new byte[1];
        byte[] bWrite3 = PrintCmd.GetStatus3();
        if (mUsbDriver.read(bRead3, bWrite3, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus3(bRead3[0]);
        }

        if (iRet != 0)
            return iRet;

        byte[] bRead4 = new byte[1];
        byte[] bWrite4 = PrintCmd.GetStatus4();
        if (mUsbDriver.read(bRead4, bWrite4, usbDev) > 0) {
            iRet = PrintCmd.CheckStatus4(bRead4[0]);
        }

        return iRet;
    }


    /**
     * -------------------------------1D 72 01 指令-----------------------------------
     */
    // 获取打印完成状态 1D 72 01发送指令
    public static byte[] getPrintStatus() {
        byte[] bCmd = new byte[3];
        int iIndex = 0;
        bCmd[iIndex++] = 0x1D;
        bCmd[iIndex++] = 0x72;
        bCmd[iIndex++] = 0x01;
        return bCmd;
    }


    /**
     * 2.小票打印
     *
     * @param data
     * @param code
     */
    public int PrintTicketData(String title, String data, String code) {
        if (mUsbDev1 == null)
            return 0;
        UsbDevice usbDev = mUsbDev1;
        int iStatus = getPrinterStatus(usbDev);
        if (checkStatus(iStatus) != 0)
            return 0;
        try {
           /* mUsbDriver.write(PrintCmd.SetClean(), usbDev);  // 初始化，清理缓存
            // 小票标题
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(1), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(1, 1), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(title, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            // 小票号码
            mUsbDriver.write(PrintCmd.PrintFeedline(2), usbDev);
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetLeftmargin(130), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(data, 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintQrcode(code, 20, 8, 1), usbDev);


            mUsbDriver.write(PrintCmd.PrintFeedline(5), usbDev);*/
            mUsbDriver.write(PrintCmd.SetClean(), usbDev);  // 初始化，清理缓存
            // 小票号码
            mUsbDriver.write(PrintCmd.SetBold(1), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(1), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(title, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintFeedline(1), usbDev); // 打印走纸2行
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            // 小票主要内容
            mUsbDriver.write(PrintCmd.PrintQrcode(code, 50, 6, 0), usbDev);           // 【2】MS-D245,MSP-100二维码，左边距、size、环绕模式0
            mUsbDriver.write(PrintCmd.PrintString(data, 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            // 走纸换行、切纸、清理缓存
            SetFeedCutClean(cutter, usbDev);

            return 1;
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            return 0;
        }
    }


    /**
     * 2.小票打印
     */
    public int PrintTicketData(String content) {
        if (mUsbDev1 == null)
            return 0;
        UsbDevice usbDev = mUsbDev1;
        int iStatus = getPrinterStatus(usbDev);
        if (checkStatus(iStatus) != 0)
            return 0;
        try {
           /* mUsbDriver.write(PrintCmd.SetClean(), usbDev);  // 初始化，清理缓存
            // 小票标题
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(1), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(1, 1), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(title, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            // 小票号码
            mUsbDriver.write(PrintCmd.PrintFeedline(2), usbDev);
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetLeftmargin(130), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(data, 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintQrcode(code, 20, 8, 1), usbDev);


            mUsbDriver.write(PrintCmd.PrintFeedline(5), usbDev);*/
            mUsbDriver.write(PrintCmd.SetClean(), usbDev);  // 初始化，清理缓存
            // 小票号码
            mUsbDriver.write(PrintCmd.SetBold(1), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(1), usbDev);
            mUsbDriver.write(PrintCmd.PrintString(content, 0), usbDev);
            mUsbDriver.write(PrintCmd.SetAlignment(0), usbDev);
            mUsbDriver.write(PrintCmd.SetSizetext(0, 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintFeedline(1), usbDev); // 打印走纸2行
            mUsbDriver.write(PrintCmd.SetBold(0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            mUsbDriver.write(PrintCmd.PrintString("  ", 0), usbDev);
            // 走纸换行、切纸、清理缓存
            SetFeedCutClean(cutter, usbDev);

            return 1;
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            return 0;
        }
    }


    // 走纸换行、切纸、清理缓存
    private void SetFeedCutClean(int iMode, UsbDevice usbDev) {
       /* mUsbDriver.write(PrintCmd.PrintFeedline(5), usbDev);      // 走纸换行
        mUsbDriver.write(PrintCmd.PrintCutpaper(iMode), usbDev);  // 切纸类型
        mUsbDriver.write(PrintCmd.SetClean(), usbDev);            // 清理缓存*/

        mUsbDriver.write(PrintCmd.PrintMarkpositioncut(), usbDev);  // 进纸到切纸位置
        mUsbDriver.write(PrintCmd.PrintMarkcutpaper(1), usbDev);  // 切纸类型 打印黑标切纸

        mUsbDriver.write(PrintCmd.SetClean(), usbDev);            // 清理缓存
    }


    // 通过系统语言判断Message显示
    String receive = "", state = ""; // 接收提示、状态类型
    String normal = "", notConnectedOrNotPopwer = "", notMatch = "",
            printerHeadOpen = "", cutterNotReset = "", printHeadOverheated = "",
            blackMarkError = "", paperExh = "", paperWillExh = "", abnormal = "";


    private int checkStatus(int iStatus) {
        int iRet = -1;

        StringBuilder sMsg = new StringBuilder();

        //0 打印机正常 、1 打印机未连接或未上电、2 打印机和调用库不匹配
        //3 打印头打开 、4 切刀未复位 、5 打印头过热 、6 黑标错误 、7 纸尽 、8 纸将尽
        switch (iStatus) {
            case 0:
                sMsg.append(normal);       // 正常
                iRet = 0;
                break;
            case 8:
                sMsg.append(paperWillExh); // 纸将尽
                iRet = 0;
                break;
            case 3:
                sMsg.append(printerHeadOpen); //打印头打开
                break;
            case 4:
                sMsg.append(cutterNotReset);
                break;
            case 5:
                sMsg.append(printHeadOverheated);
                break;
            case 6:
                sMsg.append(blackMarkError);
                break;
            case 7:
                sMsg.append(paperExh);     // 纸尽==缺纸
                break;
            case 1:
                sMsg.append(notConnectedOrNotPopwer);
                break;
            default:
                sMsg.append(abnormal);     // 异常
                break;
        }
        return iRet;

    }


    /**
     * 获取Assets子文件夹下的文件数据流数组InputStream[]
     *
     * @param context
     * @return InputStream[]
     */
    @SuppressWarnings("unused")
    private static InputStream[] getAssetsImgaes(String imgPath, Context context) {
        String[] list = null;
        InputStream[] arryStream = null;
        try {
            list = context.getResources().getAssets().list(imgPath);
            arryStream = new InputStream[3];
            for (int i = 0; i < list.length; i++) {
                InputStream is = context.getResources().getAssets()
                        .open(imgPath + File.separator + list[i]);
                arryStream[i] = is;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return arryStream;
    }

    /*
     * 未转换为十六进制字节的字符串
     *
     * @param paramString
     *
     * @return byte[]
     */
    public static byte[] hexStr2Bytesnoenter(String paramString) {
        String[] paramStr = paramString.split(" ");
        byte[] arrayOfByte = new byte[paramStr.length];

        for (int j = 0; j < paramStr.length; j++) {
            arrayOfByte[j] = Integer.decode("0x" + paramStr[j]).byteValue();
        }
        return arrayOfByte;
    }

    /**
     * 统计指定字符串中某个符号出现的次数
     *
     * @param str
     * @return int
     */
    public static int Count(String strData, String str) {
        int iBmpNum = 0;
        for (int i = 0; i < strData.length(); i++) {
            String getS = strData.substring(i, i + 1);
            if (getS.equals(str)) {
                iBmpNum++;
            }
        }
        //System.out.println(str + "出现了:" + iBmpNum + "次");
        return iBmpNum;
    }

    /**
     * 字符串转换为16进制
     *
     * @param strPart
     * @return
     */
    @SuppressLint({"UseValueOf", "DefaultLocale"})
    public static String stringTo16Hex(String strPart) {
        if (strPart == "")
            return "";
        try {
            byte[] b = strPart.getBytes("gbk"); // 数组指定编码格式，解决中英文乱码
            String str = "";
            for (int i = 0; i < b.length; i++) {
                Integer I = new Integer(b[i]);
                @SuppressWarnings("static-access")
                String strTmp = I.toHexString(b[i]);
                if (strTmp.length() > 2)
                    strTmp = strTmp.substring(strTmp.length() - 2) + " ";
                else
                    strTmp = strTmp.substring(0, strTmp.length()) + " ";
                str = str + strTmp;
            }
            return str.toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * @param a   转化数据
     * @param len 占用字节数
     * @return String
     * @Title:intToHexString
     * @Description:10进制数字转成16进制
     */
    public static String intToHexString(int a, int len) {
        len <<= 1;
        String hexString = Integer.toHexString(a);
        int b = len - hexString.length();
        if (b > 0) {
            for (int i = 0; i < b; i++) {
                hexString = "0" + hexString;
            }
        }
        return hexString;
    }

    /**
     * 通过选择文件获取路径
     *
     * @param context
     * @param uri
     * @return String
     */
    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection,
                        null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    // ------------------20161216 Add-----------------------

    /**
     * 获取SD卡路径
     *
     * @return String
     */
    private static String getSDCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator;
    }

    /**
     * BitmapOption 位图选项
     *
     * @param inSampleSize
     * @return
     */
    private static Options getBitmapOption(int inSampleSize) {
        System.gc();
        Options options = new Options();
        options.inPurgeable = true;
        options.inSampleSize = inSampleSize;
        options.inPreferredConfig = Config.ARGB_4444; // T4 二维码图片效果最佳
        return options;
    }

    /**
     * 获取Bitmap数据
     *
     * @param imgPath
     * @return
     */
    public static Bitmap getBitmapData(String imgPath) {
        Bitmap bm = BitmapFactory.decodeFile(imgPath, getBitmapOption(1)); // 将图片的长和宽缩小味原来的1/2
        return bm;
    }

    /**
     * 获取SDCard图片路径,指定已知的路径
     *
     * @param fileName
     * @return
     */
    public static String getBitmapPath(String fileName) {
        String imgPath = getSDCardPath() + "DCIM" + File.separator + "BMP"
                + File.separator + fileName;
        return imgPath;
    }

    /**
     * 将彩色图转换为纯黑白二色
     *
     * @param bmp 位图
     * @return 返回转换好的位图
     */
    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                // 分离三原色
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                // 转化成灰度像素
                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        // 新建图片
        Bitmap newBmp = Bitmap.createBitmap(width, height, Config.RGB_565); // RGB_565
        // 设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, 380, 460);
        return resizeBmp;
    }

    /**
     * SharedPreferences存储数据方式工具类
     *
     * @author zuolongsnail
     */
    public final static String SETTING = "masung";

    // 移除数据
    public static void removeValue(Context context, String key) {
        Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
        sp.clear();
        sp.commit();
    }

    public static void putValue(Context context, String key, int value) {
        Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
        sp.putInt(key, value);
        sp.commit();
    }

    public static void putValue(Context context, String key, boolean value) {
        Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
        sp.putBoolean(key, value);
        sp.commit();
    }

    public static void putValue(Context context, String key, String value) {
        Editor sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE).edit();
        sp.putString(key, value);
        sp.commit();
    }

    public static int getValue(Context context, String key, int defValue) {
        SharedPreferences sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        int value = sp.getInt(key, defValue);
        return value;
    }

    public static boolean getValue(Context context, String key, boolean defValue) {
        SharedPreferences sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        boolean value = sp.getBoolean(key, defValue);
        return value;
    }

    public static String getValue(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(SETTING, Context.MODE_PRIVATE);
        String value = sp.getString(key, defValue);
        return value;
    }
}
