package com.wyl.androidweb;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.sjjd.wyl.baseandroid.utils.Configs;
import com.sjjd.wyl.baseandroid.utils.LogUtils;
import com.sjjd.wyl.baseandroid.utils.SPUtils;
import com.wyl.androidweb.net.SocketManager2;
import com.wyl.androidweb.print.PrintUtils;
import com.wyl.androidweb.qr.QRThread;
import com.wyl.androidweb.qr.QrManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;

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

        if (ip.length() == 0 || port.length() == 0) {
            Toasty.error(mContext, "ip或端口为空！", Toast.LENGTH_LONG, true).show();
            //  return;
        } else {
           /* mSocketManager = SocketManager2.getInstance(mContext);
            mSocketManager.setHandler(mDataHandler);
            mSocketManager.startTcpConnection(ip, port);
            mSocketManager.getTcpSocket2().setPING(null);*/
        }
        //   String path = "file:///" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/sjjd/lottory/index.html";
        String path = "file:///android_asset/print.html";

        LogUtils.e(TAG, "onCreate: " + path);
        mWebContent.loadUrl(path);
        webSetting();

        //连接扫描器
        connectQRDevice();

        getUsbDriverService();

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!PrintUtils.init(mContext).PrintConnStatus()) {
            Toasty.error(mContext, "打印机连接异常！", Toast.LENGTH_LONG, true).show();
        }
    }

    private void webSetting() {

        // 处理网页内的连接（自身打开）
        mWebContent.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @TargetApi(21)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                String url = request.getUrl().toString();
                LogUtils.e(TAG, "onReceivedHttpError: " + url + "   " + errorResponse.getReasonPhrase());
                if (url.contains("favicon.ico")) {

                } else {
                }

            }

            @TargetApi(23)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                String url = request.getUrl().toString();
                LogUtils.e(TAG, "onReceivedError: " + url + "  " + error.getDescription());
                if (url.contains("favicon.ico")) {

                } else {
                    //  mWebContent.loadUrl(mainUrl);
                }

            }


            // 处理ssl请求
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                LogUtils.e(TAG, "onPageStarted: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //mWebContent.loadUrl("javascript:(function() { var videos = document.getElementsByTagName('video'); for(var i=0;i<videos.length;i++){videos[i].play();}})()");
                LogUtils.e(TAG, "onPageFinished: " + url);
                // mWebContent.loadUrl("javascript:try{autoplay();}catch(e){}");


                //mWebContent.loadUrl("javascript:myFunction()");

            }
        });
        mWebContent.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (!TextUtils.isEmpty(title) && title.toLowerCase().contains("error")) {
                }
            }
        });


        // 使用返回键的方式防止网页重定向
        mWebContent.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && mWebContent.canGoBack()) {
                        mWebContent.goBack();
                        return true;
                    }
                }
                return false;
            }
        });


        WebSettings webSettings = mWebContent.getSettings();

        // 支持javascript
        webSettings.setJavaScriptEnabled(true);

        // 支持使用localStorage(H5页面的支持)
        webSettings.setDomStorageEnabled(true);

        // 支持数据库
        webSettings.setDatabaseEnabled(true);

        // 禁止支持缓存
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        /*
        String appCaceDir = this.getApplicationContext().getDir("cache", Context.MODE_PRIVATE).getPath();
        webSettings.setAppCachePath(appCaceDir);
        */

        // 设置可以支持缩放
        webSettings.setUseWideViewPort(false);

        // 扩大比例的缩放
        webSettings.setSupportZoom(false);

        webSettings.setBuiltInZoomControls(false);

        // 隐藏缩放按钮
        webSettings.setDisplayZoomControls(false);

        // 自适应屏幕
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setLoadWithOverviewMode(true);


        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // 隐藏滚动条
        mWebContent.setHorizontalScrollBarEnabled(false);
        mWebContent.setVerticalScrollBarEnabled(false);

        mWebContent.addJavascriptInterface(this, "android");
        mWebContent.addJavascriptInterface(this, "android");
    }

    @Override
    public void userHandler(Message msg) {
        super.userHandler(msg);
        switch (msg.what) {
            case Configs.MSG_SOCKET_RECEIVED:
                String path = msg.obj.toString();
                LogUtils.e(TAG, "userHandler: " + path);
                mWebContent.loadUrl("file://" + path);
                break;

            case 20001:
                String qr = (String) msg.obj;
                scannerCallback(qr);
                break;
        }
    }


    PrintUtils mPrintUtils;

    private void getUsbDriverService() {
        mPrintUtils = PrintUtils.init(mContext);
    }

    @JavascriptInterface
    public void JsPrint(final String content) {
        LogUtils.e(TAG, "JsPrint: 开始调用打印机 " + content);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int mStatus = PrintUtils.init(mContext).getPrinterStatus();
                if (mStatus == 0) {
                    mPrintUtils.PrintTicketData(content);
                } else {
                    Toasty.error(mContext, checkState(mStatus), Toast.LENGTH_LONG, true).show();
                }
            }
        });

    }

    private String checkState(int status) {
        StringBuilder sMsg = new StringBuilder();
        //0 打印机正常 、1 打印机未连接或未上电、2 打印机和调用库不匹配
        //3 打印头打开 、4 切刀未复位 、5 打印头过热 、6 黑标错误 、7 纸尽 、8 纸将尽
        switch (status) {
            case 1:
                sMsg.append(status + " 打印机未连接或未上电");
                break;
            case 2:
                sMsg.append(status + " 打印机和调用库不匹配");
                break;
            case 3:
                sMsg.append(status + " 打印头打开"); //打印头打开
                break;
            case 4:
                sMsg.append(status + " 切刀未复位");
                break;
            case 5:
                sMsg.append(status + " 打印头过热");
                break;
            case 6:
                sMsg.append(status + " 黑标错误");
                break;
            case 7:
                sMsg.append(status + " 纸尽缺纸");     // 纸尽==缺纸
                break;
            default:
                sMsg.append(status + " 打印机异常");     // 异常
                break;
        }
        return sMsg.toString();

    }

    QRThread mScanThread;
    Thread mQRConnectThread;//链接扫码设备的线程

    /**
     * 二维码扫码结果处理 此处通知网页处理扫码结果
     *
     * @param qr
     */
    private void scannerCallback(final String qr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String script = "javascript:receiveAndroidScanner('" + qr + "')";
                mWebContent.evaluateJavascript(script, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        LogUtils.e(TAG, "onReceiveValue: " + value);
                        mScanThread.OnResume();
                    }
                });
            }
        });
    }

    /**
     * 网页js调用 通知启动二维码扫码
     */
    @JavascriptInterface
    public void JsStartScanner() {

        LogUtils.e(TAG, "JsStartScanner: 开启二维码扫描");
        if (QrManager.init().isConnected()) {
            //开启扫码线程
            if (mScanThread != null) {
                mScanThread.onDestroy();
                mScanThread = null;
            }
            mScanThread = new QRThread(mDataHandler, 20001);
            mScanThread.start();

        } else {
            //连接扫描器
            connectQRDevice();
            Toasty.error(mContext, "扫码器连接故障！", Toast.LENGTH_LONG, true).show();

        }
    }

    private void connectQRDevice() {
        mQRConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (!QrManager.init().isConnected()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    QrManager.init().connect();

                    LogUtils.e(TAG, "run: ");

                }

            }
        });
        mQRConnectThread.start();
    }
}
