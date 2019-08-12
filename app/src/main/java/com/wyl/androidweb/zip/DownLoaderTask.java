package com.wyl.androidweb.zip;

import android.content.Context;
import android.os.AsyncTask;

import com.sjjd.wyl.baseandroid.utils.FileUtils;
import com.sjjd.wyl.baseandroid.utils.LogUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by wyl on 2018/6/5.
 * 文件下载
 */

public class DownLoaderTask extends AsyncTask<Void, Integer, Long> {
    private final String TAG = "DownLoaderTask";
    private URL mUrl;//压缩URL
    private File mFile;//压缩文件
    private int mProgress = 0;
    private int mToatalLength = 0;
    private ProgressReportingOutputStream mOutputStream;
    private Context mContext;

    private long downTime;
    private int tryTime;

    public DownLoaderTask(String url, String out, Context context) {

        downTime = System.currentTimeMillis();

        mContext = context;
        try {
            mUrl = new URL(url);//压缩URL
            FileUtils.deleteDirectory(out);

            File dir = new File(out);//输出目录 下载的压缩包存放目录
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = new File(mUrl.getFile()).getName();
            mFile = new File(out, fileName);
            LogUtils.e(TAG, " 压缩文件路径 path =  " + mFile.getAbsolutePath());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        return download();
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
            int per = (int) (mValue / mToatalLength * 100);
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

    private long download() {
        URLConnection connection = null;
        int bytesCopied = 0;
        try {
            connection = mUrl.openConnection();
            int length = connection.getContentLength();
            // String mBytes2mb = bytes2mb(length);//将字节数转为M
            mToatalLength = length;
            //存在同一文件 删除从新下载
            if (mFile.exists() && length == mFile.length()) {
                FileUtils.deleteFile(mFile);
            }
            mOutputStream = new ProgressReportingOutputStream(mFile);
            publishProgress(0, length);
            bytesCopied = copy(connection.getInputStream(), mOutputStream);
            if (bytesCopied != length && length != -1) {
                LogUtils.e(TAG, "Download incomplete bytesCopied=" + bytesCopied + ", length" + length);
            }
            mOutputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bytesCopied;
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
          /*  int trytimes = SPUtils.init(mContext).getDIYInt(Configs.SP.SETTING_TRY_TIME);
            //下载超时
            if (trytimes < 1)
                trytimes = 1;
            if (System.currentTimeMillis() - downTime > trytimes * 60 * 1000) {
                if (!isCancelled())
                    cancel(true);
                tryTime++;
                if (tryTime > 3) {
                    LogUtils.e(TAG, "write:  下载超时三次 ");
                    return;
                } else {
                    doInBackground();
                    return;
                }
            }*/
            LogUtils.e(TAG, "write: 下载进度  " + mProgress + "   " + mToatalLength);

            if (mProgress >= mToatalLength) {//下载完成 开始解压
                //通知更新
             //   String port = SPUtils.init(mContext).getDIYString(I.SP.PORT);
                //读取默认配置信息
             //   String host = String.format(Configs.HOST, port);
             //   String id = SPUtils.init(mContext).getDIYString(Configs.SP.PROGRAM_ID);

              /*  OkGo.<String>post(host + Configs.URL_ADD_PUSH)
                        .params("pushTem", id)
                        .params("pushMac", DeviceUtil.getMacAddress(mContext))
                        .params("pushState", "1")
                        .tag(this)
                        .execute(new StringCallback() {
                            @Override
                            public void onSuccess(Response<String> response) {

                            }
                        });*/


             /*   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA);
                String now = sdf.format(new Date(System.currentTimeMillis()));


                String copy = SPUtils.init(mContext).getDIYString(Configs.SP.PATH_DATA);
                String last = SPUtils.init(mContext).getDIYString(Configs.SP.PATH_DATA_BACKUP);

                //新节目文件夹
                String cur = Configs.PROGRAM_PATH + "/" + now + "/";
                LogUtils.e(TAG, "write: copy = " + copy + "  last= " + last + "  cur= " + cur);
                //保留上一次更新后的节目包
                SPUtils.init(mContext).putDIYString(Configs.SP.PATH_DATA_BACKUP, copy.length() < 1 ? cur : copy);

                SPUtils.init(mContext).putDIYString(Configs.SP.PATH_DATA, cur);


                //删除以前的文件
                File mFilePros = new File(Configs.PROGRAM_PATH);
                if (mFilePros.isDirectory()) {
                    File[] dirs = mFilePros.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isDirectory();
                        }
                    });

                    if (dirs != null && dirs.length > 2) {
                        //说明当前目录下的文件有两个
                        //此时有新的节目，删除备份的节目
                        Arrays.sort(dirs, new Comparator<File>() {
                            @Override
                            public int compare(File f1, File f2) {
                                return (int) (f1.lastModified() - f2.lastModified());
                            }
                        });

                        String c1 = SPUtils.init(mContext).getDIYString(Configs.SP.PATH_DATA_BACKUP);
                        String c2 = SPUtils.init(mContext).getDIYString(Configs.SP.PATH_DATA);

                        for (int i = 0; i < dirs.length; i++) {
                            if (dirs[i].getAbsolutePath().equals(c1) || dirs[i].getAbsolutePath().equals(c2))
                                continue;
                            FileUtils.deleteDirectory(dirs[i].getAbsolutePath());
                        }
                    }

                }
*/

                //删除以前的ZIP文件
               /* File mFileZip = new File(Configs.ZIP_PATH);
                if (mFileZip.isDirectory()) {
                    File[] dirs = mFileZip.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            return file.isFile();
                        }
                    });
                    if (dirs != null && dirs.length > 1) {
                        for (int i = 0; i < dirs.length; i++) {
                            if (dirs[i].getAbsolutePath().equals(mFile.getAbsolutePath()))
                                continue;
                            dirs[i].delete();
                        }
                    }
                }
*/
                //解压到正式目录
              /*  ZipExtractorTask task1 = new ZipExtractorTask(mFile,
                        cur, mContext, true);
                task1.execute();*/

              /*  ZipExtractorTask task2 = new ZipExtractorTask(mFile,
                        copyPath, mContext, true);
                task2.execute();*/

                /*第一次下载成功后，两个路径指向同一个，第二次下载的时候，如果文件个数少于2*/
            }
        }

    }
}
