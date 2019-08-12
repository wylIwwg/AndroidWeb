package com.wyl.androidweb.zip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sjjd.wyl.baseandroid.utils.LogUtils;


/**
 * 压缩包下载完成监听
 */
public class DoneReceiver extends BroadcastReceiver {
    private static final String TAG = " === DoneReceiver ====";
    String action = "ZipExtractorTaskDone";//解压完成的广播

    @Override
    public void onReceive(final Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String ac = intent.getAction();
        LogUtils.e(TAG, "onReceive: " + ac);
        if (ac != null && ac.equals(action)) {
            //收到解压完成的广播  重新启动应用
            Thread mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    final Intent newintent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                    if (newintent != null) {
                        newintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(newintent);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }
            });
            mThread.start();

        }

    }
}
