package xyz.yhsj.update.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

import xyz.yhsj.update.R;
import xyz.yhsj.update.UpdateHelper;
import xyz.yhsj.update.utils.FileUtil;

/***
 * 升级服务
 * <p>
 * Created by LOVE on 2016/8/31 031.
 */
public class DownloadService extends Service {


    //下载进度步进
    private static final int down_step_custom = 3;
    // 超时
    private static final int TIMEOUT = 10 * 1000;
    //handler状态
    public static final int DOWN_SUCCESS = 0;
    public static final int DOWN_ERROR = -1;
    //广播-发送没有安装权限的代码
    public static final int NO_INSTALL_PERMISSION = 2;
    //广播-安装应用
    public static final int INSTALL = 3;
    //通知
    private NotificationManager mNotificationManager1 = null;
    private Notification.Builder builder;
    private NotificationChannel channel;
    private int notificationID = 111;
    //app名称
    private String app_name;
    //appUrl
    private static String down_url;
    //参数
    //app名称
    public static final String KEY_APP_NAME = "Key_App_Name";
    //appUrl
    public static final String KEY_DOWN_URL = "Key_Down_Url";
    //自动安装
    public static final String KEY_AUTO_INSTALL = "Key_Auto_Install";

    //广播的action
    public static final String ACTION_BROADCAST = "xyz.yhsj.update.service.DownloadService";
    //广播
    private final Intent broadcast_intent = new Intent(ACTION_BROADCAST);
    //消息类型
    public static final String KEY_BROADCAST_TYPE = "Key_Broadcast_Type";


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * 方法描述：onStartCommand方法
     *
     * @param intent, int flags, int startId
     * @return int
     * @see DownloadService
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            app_name = intent.getStringExtra(KEY_APP_NAME);
            down_url = intent.getStringExtra(KEY_DOWN_URL);
        }
        // create file,应该在这个地方加一个返回值的判断SD卡是否准备好，文件是否创建成功，等等！
        if (true == FileUtil.createFile(app_name) && !TextUtils.isEmpty(down_url)) {
            createNotification();
            createThread();
        }

        return super.onStartCommand(intent, flags, startId);
    }


    /*********
     * update UI
     ******/
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWN_SUCCESS:
                    // 震动提示
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // 参数是震动时间(long类型)
                    vibrator.vibrate(500L);

                    //检测8.0是否有安装应用的权限
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        boolean installAllowed = getPackageManager().canRequestPackageInstalls();
                        //有权限
                        if (installAllowed) {
                            //广播安装
                            sendInstallBroadcast(DownloadService.this);
                        } else {
                            //广播申请权限
                            broadcast_intent.putExtra(KEY_BROADCAST_TYPE, NO_INSTALL_PERMISSION);
                            sendBroadcast(broadcast_intent);
                        }
                    } else {
                        sendInstallBroadcast(DownloadService.this);
                    }
                    mNotificationManager1.cancel(notificationID);
                    break;
                case DOWN_ERROR:
                    //广播
                    broadcast_intent.putExtra(KEY_BROADCAST_TYPE, DOWN_ERROR);
                    sendBroadcast(broadcast_intent);
                    builder.setAutoCancel(true);
                    Toast.makeText(DownloadService.this, "下载停止", Toast.LENGTH_SHORT).show();
                    mNotificationManager1.cancel(notificationID);
                    /***stop service*****/
                    stopSelf();
                    break;

                default:

                    break;
            }
        }
    };

    /**
     * 安装应用的通知
     * @param context
     */
    public static void sendInstallBroadcast(Context context) {
        Intent broadcast_intent = new Intent(ACTION_BROADCAST);
        broadcast_intent.putExtra(KEY_BROADCAST_TYPE, INSTALL);
        context.sendBroadcast(broadcast_intent);
    }

    /**
     * 方法描述：createThread方法, 开线程下载
     *
     * @param
     * @return
     * @see DownloadService
     */
    public void createThread() {
        new DownLoadThread().start();
    }

    private class DownLoadThread extends Thread {
        @Override
        public void run() {
            try {
                downloadUpdateFile(down_url, FileUtil.updateFile.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Message message = new Message();
                message.what = DOWN_ERROR;
                handler.sendMessage(message);
            }
        }
    }


    /**
     * 方法描述：createNotification方法
     *
     * @param
     * @return
     * @see DownloadService
     */
    public void createNotification() {
        mNotificationManager1 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setTicker("正在下载新版本");
        builder.setContentTitle(app_name);
        builder.setContentText("正在下载,请稍后...");
        builder.setNumber(0);
        builder.setAutoCancel(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("appupdate", "appupdate", NotificationManager.IMPORTANCE_LOW);
            mNotificationManager1.createNotificationChannel(channel);
            builder.setChannelId("appupdate");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNotificationManager1.notify(notificationID, builder.build());
        } else {
            mNotificationManager1.notify(notificationID, builder.getNotification());
        }
    }

    /***
     * down file
     *
     * @return
     * @throws
     */
    public long downloadUpdateFile(String down_url, String file) throws Exception {
        int down_step = down_step_custom;// 提示step
        int totalSize;// 文件总大小
        int downloadCount = 0;// 已经下载好的大小
        int updateCount = 0;// 已经上传的文件大小

        InputStream inputStream;
        OutputStream outputStream;

        URL url = new URL(down_url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setConnectTimeout(TIMEOUT);
        httpURLConnection.setReadTimeout(TIMEOUT);
        // 获取下载文件的size
        totalSize = httpURLConnection.getContentLength();

        inputStream = httpURLConnection.getInputStream();
        outputStream = new FileOutputStream(file, false);// 文件存在则覆盖掉

        byte buffer[] = new byte[1024];
        int readsize = 0;

        while ((readsize = inputStream.read(buffer)) != -1) {

            outputStream.write(buffer, 0, readsize);
            downloadCount += readsize;// 时时获取下载到的大小

            if (UpdateHelper.getInstance().isDownloadCancel()) {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                    inputStream.close();
                    outputStream.close();
                    UpdateHelper.getInstance().setDownloadCancel(false);
                }
            }


            /*** 每次增张3%**/
            if (updateCount == 0 || (downloadCount * 100.0 / totalSize) >= updateCount) {
                updateCount += down_step;

                // 改变通知栏
                builder.setProgress(totalSize, downloadCount, false);
                builder.setContentInfo(getPercent(downloadCount, totalSize));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mNotificationManager1.notify(notificationID, builder.build());
                } else {
                    mNotificationManager1.notify(notificationID, builder.getNotification());
                }
            }
            //下载完了
            if (downloadCount == totalSize) {
                builder.setProgress(totalSize, downloadCount, false);
                builder.setContentInfo(getPercent(downloadCount, totalSize));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mNotificationManager1.notify(notificationID, builder.build());
                } else {
                    mNotificationManager1.notify(notificationID, builder.getNotification());
                }
                Message message = new Message();
                message.what = DOWN_SUCCESS;
                handler.sendMessage(message);
            }
        }

        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }

        inputStream.close();
        outputStream.close();

        return downloadCount;
    }

    /**
     * @param x     当前值
     * @param total 总值
     * @return 当前百分比
     * @Description:返回百分之值
     */
    private String getPercent(int x, int total) {
        String result = "";// 接受百分比的值
        double x_double = x * 1.0;
        double tempresult = x_double / total;
        // 百分比格式，后面不足2位的用0补齐 ##.00%
        DecimalFormat df1 = new DecimalFormat("0.0%");
        result = df1.format(tempresult);
        return result;
    }
}