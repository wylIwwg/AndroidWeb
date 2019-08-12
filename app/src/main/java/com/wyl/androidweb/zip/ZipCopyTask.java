package com.wyl.androidweb.zip;

import android.content.Context;
import android.os.AsyncTask;

import com.sjjd.wyl.baseandroid.utils.FileUtils;
import com.sjjd.wyl.baseandroid.utils.LogUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URL;


/**
 * Created by wyl on 2018/6/5.
 */

public class ZipCopyTask extends AsyncTask<Void, Integer, Long> {
    private final String TAG = "ZipCopyTask";
    private URL mUrl;//压缩URL
    private File mFile;//压缩文件
    private File destDir;
    private int mProgress = 0;
    private float mToatalLenght = 0;
    private ProgressReportingOutputStream mOutputStream;
    private Context mContext;

    public ZipCopyTask(File zip, String out, Context context ) {
        super();
        mContext = context;
        mFile = zip;
        destDir = new File(out, mFile.getName());
        LogUtils.e(TAG, "ZipCopyTask: " + zip.getAbsolutePath());
        LogUtils.e(TAG, "ZipCopyTask: " + destDir.getAbsolutePath());
        if (destDir.exists()) {
            destDir.delete();
        }
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        //super.onPreExecute();
    }

    @Override
    protected Long doInBackground(Void... params) {
        // TODO Auto-generated method stub
        return copy();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        // TODO Auto-generated method stub
        //super.onProgressUpdate(values);
        if (values.length > 1) {
            int contentLength = values[1];
            if (contentLength == -1) {

            } else {

            }
        } else {
            float mValue = values[0];
            int per = (int) (mValue / mToatalLenght * 100);
        }


    }

    @Override
    protected void onPostExecute(Long result) {
        // TODO Auto-generated method stub
        //super.onPostExecute(result);
        if (isCancelled())
            return;
        // ((MainActivity) mContext).showUnzipDialog();
    }

    private long copy() {
        int bytesCopied = 0;
        InputStream fosfrom = null;
        try {
            fosfrom = new FileInputStream(mFile);
            float length = mFile.length();
            LogUtils.e(TAG, "copy: mToatalLenght " + length);
            // String mBytes2mb = bytes2mb(length);//将字节数转为M
            mToatalLenght = length;
            mOutputStream = new ProgressReportingOutputStream(destDir);
            publishProgress(0, (int) length);
            bytesCopied = copy(fosfrom, mOutputStream);
            if (bytesCopied != length && length != -1) {
                LogUtils.e(TAG, "Download incomplete bytesCopied=" + bytesCopied + ", length" + length);
            }
            fosfrom.close();
            mOutputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bytesCopied;
    }

    public static String bytes2mb(long bytes) {
        BigDecimal filesize = new BigDecimal(bytes);
        BigDecimal megabyte = new BigDecimal(1024 * 1024);
        float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1)
            return (returnValue + "MB");
        BigDecimal kilobyte = new BigDecimal(1024);
        returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        return (returnValue + "KB");
    }

    private int copy(InputStream input, OutputStream output) {
        byte[] buffer = new byte[1024 * 8];
        BufferedInputStream in = new BufferedInputStream(input, 1024 * 8);
        BufferedOutputStream out = new BufferedOutputStream(output, 1024 * 8);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, 1024 * 8)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    private final class ProgressReportingOutputStream extends FileOutputStream {

        public ProgressReportingOutputStream(File file)
                throws FileNotFoundException {
            super(file);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void write(byte[] buffer, int byteOffset, int byteCount)
                throws IOException {
            // TODO Auto-generated method stub
            super.write(buffer, byteOffset, byteCount);
            mProgress += byteCount;
            publishProgress(mProgress);
            if (mProgress >= mToatalLenght) {//下载完成 开始解压
                ZipExtractorTask task1 = new ZipExtractorTask(destDir,
                        FileUtils.SDCard_OUT_Path, mContext, true);
                task1.execute();
            }
        }

    }
}
