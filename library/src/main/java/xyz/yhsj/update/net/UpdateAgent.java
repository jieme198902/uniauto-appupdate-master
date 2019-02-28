package xyz.yhsj.update.net;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Map;

import xyz.yhsj.update.R;
import xyz.yhsj.update.UpdateHelper;
import xyz.yhsj.update.bean.UpdateEntity;
import xyz.yhsj.update.listener.NetCallBack;
import xyz.yhsj.update.service.DownloadService;
import xyz.yhsj.update.utils.FileUtil;

/**
 * 更新代理
 */
public class UpdateAgent {
    private static UpdateAgent updater;
    //下载进度的广播
    private UpdateProgressBroadcastReceiver broadcastReceiver;
    private Activity activity;

    public static UpdateAgent getInstance() {
        if (updater == null) {
            updater = new UpdateAgent();
        }
        return updater;
    }


    /**
     * 检测更新
     *
     * @param activity 上下文
     */
    public void checkUpdate(final Activity activity) {
        this.activity = activity;
        // 动态注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_BROADCAST);
        broadcastReceiver = new UpdateProgressBroadcastReceiver();
        activity.registerReceiver(broadcastReceiver, filter);

        //获取appKey
        String appKey = UpdateHelper.getInstance().getAppKey();
        //请求地址
        String url = UpdateHelper.getInstance().getUrl();
        if (TextUtils.isEmpty(url)) {
            throw new RuntimeException("please setUrl before request !!!");
        }
        //检测是否含有appKey
        if (TextUtils.isEmpty(appKey)) {
            throw new RuntimeException("please setAppKey before request !!!");
        }
        Map<String, String> param = UpdateHelper.getInstance().getParams();
        param.put("appKey", appKey);
        if (UpdateHelper.getInstance().getHttpMethod() == HttpMetHod.GET) {
            url += (url.indexOf("?") < 0 ? "?" : "&");
            url += "appKey" + "=" + appKey;
        }
        new NetUtils(url,
                UpdateHelper.getInstance().getHttpMethod(),
                param,
                new NetCallBack() {
                    @Override
                    public void onSuccess(String result) {
                        UpdateEntity updateEntity = UpdateHelper.getInstance().getJsonParser().parse(result);
                        if (updateEntity == null || TextUtils.isEmpty(updateEntity.getUpdateUrl())) {
                            //通知前台更新状态
                            if (UpdateHelper.getInstance().getUpdateListener() != null) {
                                UpdateHelper.getInstance().getUpdateListener().Update(false, null);
                                //销毁监听器，防止因为单例模式下监听器未消毁导致的异常
                                UpdateHelper.getInstance().setUpdateListener(false, null);
                            }
                        } else {
                            if (!UpdateHelper.getInstance().isOnlyCheck()) {
                                showAlertDialog(activity, updateEntity);
                            }
                            //通知前台更新状态
                            if (UpdateHelper.getInstance().getUpdateListener() != null) {
                                UpdateHelper.getInstance().getUpdateListener().Update(true, updateEntity);
                                //销毁监听器，防止因为单例模式下监听器未消毁导致的异常
                                UpdateHelper.getInstance().setUpdateListener(false, null);
                            }
                        }
                    }

                    @Override
                    public void onFail() {

                    }
                });
    }

    /**
     * 信息提示的dialog
     *
     * @param activity     上下文
     * @param updateEntity 请求参数对象
     */
    private void showAlertDialog(final Activity activity, final UpdateEntity updateEntity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_result_view, null);
        TextView content = view.findViewById(R.id.content);
        Button btn_cancel = view.findViewById(R.id.btn_cancle);
        Button btn_update = view.findViewById(R.id.btn_update);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setView(view);
        content.setText(updateEntity.getContent());
        final AlertDialog dialog = builder.show();
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                downloadApp(activity, updateEntity);
            }
        });
    }


    /**
     * 下载APP
     *
     * @param activity     上下文
     * @param updateEntity 更新对象
     */
    private void downloadApp(Activity activity, UpdateEntity updateEntity) {
        this.activity = activity;
        //判断服务是否运行，防止重复启动产生错误
        if (!isServiceWork(activity, DownloadService.class.getName())) {

            Intent it = new Intent(activity, DownloadService.class);
            it.putExtra(DownloadService.KEY_APP_NAME, updateEntity.getName());
            it.putExtra(DownloadService.KEY_DOWN_URL, updateEntity.getUpdateUrl());

            activity.startService(it);

        } else {
            Toast.makeText(activity, "正在进行下载任务，请稍后", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 定义广播接收器（内部类）
     *
     * @author LOVE
     */
    private class UpdateProgressBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int type = intent.getIntExtra(DownloadService.KEY_BROADCAST_TYPE, -1);

            //无权限 申请权限
            if (type == DownloadService.NO_INSTALL_PERMISSION) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    boolean installAllowed = activity.getPackageManager().canRequestPackageInstalls();
                    if (!installAllowed) {
                        Uri uri = Uri.parse("package:" + activity.getPackageName());
                        Intent perIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri);
                        activity.startActivityForResult(perIntent, 777);
                        return;
                    }
                }
            }

            if (type == DownloadService.INSTALL) {
                File apk = FileUtil.updateFile;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    boolean installAllowed = activity.getPackageManager().canRequestPackageInstalls();
                    if (installAllowed) {
                        install(apk);
                    }
                } else {
                    install(apk);
                }
            }
        }
    }


    /**
     * 安卓7.0做适配
     *
     * @param apkFile
     */
    private void install(File apkFile) {
        //判读版本是否在7.0以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //在AndroidManifest中的android:authorities值
            Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            activity.startActivity(install);
        } else {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(install);
        }

        if (broadcastReceiver != null) {
            activity.unregisterReceiver(broadcastReceiver);
        }
    }


    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext    上下文
     * @param serviceName 是包名+服务的类名（例如：xyz.yhsj.upadte.service.DownloadService）
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public boolean isServiceWork(Context mContext, String serviceName) {
        ActivityManager myAM = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(30);
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName();
            if (mName.equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}
