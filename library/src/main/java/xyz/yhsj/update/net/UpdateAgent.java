package xyz.yhsj.update.net;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import xyz.yhsj.update.R;
import xyz.yhsj.update.UpdateHelper;
import xyz.yhsj.update.bean.UpdateEntity;
import xyz.yhsj.update.listener.NetCallBack;
import xyz.yhsj.update.service.DownloadService;


public class UpdateAgent {
    private static UpdateAgent updater;
    //检查更新的弹窗
    private ProgressDialog checkDialog;
    //下载的进度弹窗
    private AlertDialog downDialog;
    //下载进度的广播
    private UpdateProgressBroadcastReceiver broadcastReceiver;

    private ProgressBar progressBar;
    private TextView progresscontent;


    public static UpdateAgent getInstance() {
        if (updater == null) {
            updater = new UpdateAgent();
        }
        return updater;
    }


    /**
     * 检测更新
     *
     * @param activity
     */
    public void checkUpdate(final Activity activity) {

        if (UpdateHelper.getInstance().getCheckType() == UpdateHelper.CheckType.check_with_Dialog) {

            checkDialog = new ProgressDialog(activity);
            checkDialog = new ProgressDialog(activity);
            checkDialog.setMessage("正在检查更新...");
            checkDialog.setCancelable(false);
            if (!activity.isFinishing()) {
                checkDialog.show();
            } else {
                return;
            }
        }
        String appKey = UpdateHelper.getInstance().getAppKey();
        String url = UpdateHelper.getInstance().getUrl();
        if (TextUtils.isEmpty(url)) {
            throw new RuntimeException("please setUrl before request !!!");
        }
        //检测是否含有appKey
        if (TextUtils.isEmpty(appKey)) {
            throw new RuntimeException("please setAppKey before request !!!");
        }
        HashMap<String, String> param = UpdateHelper.getInstance().getParams();
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

                        if (checkDialog != null && checkDialog.isShowing()) {
                            checkDialog.dismiss();
                        }

                        UpdateEntity updateEntity = UpdateHelper.getInstance().getJsonParser().parse(result);
                        if (updateEntity == null || TextUtils.isEmpty(updateEntity.getUpdateUrl())) {

                            //是否仅仅检测
                            if (!UpdateHelper.getInstance().isOnlyCheck()) {

                                if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_dialog) {
                                    showNoUpdateDialog(activity);
                                } else if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_toast) {
                                    Toast.makeText(activity, "已是最新版本", Toast.LENGTH_SHORT).show();
                                } else if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_without) {

                                }

                            }

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
                        if (checkDialog != null && checkDialog.isShowing()) {
                            checkDialog.dismiss();
                        }
                        if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_dialog) {
                            showNoUpdateDialog(activity);
                        } else if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_toast) {
                            Toast.makeText(activity, "已是最新版本", Toast.LENGTH_SHORT).show();
                        } else if (UpdateHelper.getInstance().getUpdateWithOut() == UpdateHelper.UpdateWithOut.tip_without) {

                        }
                    }
                });
    }

    /**
     * 信息提示的dialog
     *
     * @param activity
     * @param updateEntity
     */
    private void showAlertDialog(final Activity activity, final UpdateEntity updateEntity) {


        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_result_view, null);

        TextView content = (TextView) view.findViewById(R.id.content);
        Button btn_cancle = (Button) view.findViewById(R.id.btn_cancle);
        Button btn_update = (Button) view.findViewById(R.id.btn_update);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setView(view);

        content.setText(updateEntity.getContent());

        final AlertDialog dialog = builder.show();

        btn_cancle.setOnClickListener(new View.OnClickListener() {
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
     * 信息提示的dialog
     *
     * @param activity
     */
    private void showNoUpdateDialog(final Activity activity) {


        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_no_update_view, null);

        Button btn_update = (Button) view.findViewById(R.id.btn_update);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setView(view);

        final AlertDialog dialog = builder.show();

        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

    }

    /**
     * 下载APP
     *
     * @param activity
     * @param updateEntity
     */
    private void downloadApp(Activity activity, UpdateEntity updateEntity) {
        //判断服务是否运行，防止重复启动产生错误
        if (!isServiceWork(activity, DownloadService.class.getName())) {

            Intent it = new Intent(activity, DownloadService.class);
            it.putExtra(DownloadService.KEY_APP_NAME, updateEntity.getName());
            it.putExtra(DownloadService.KEY_DOWN_URL, updateEntity.getUpdateUrl());

            //是否自动安装
            if (UpdateHelper.getInstance().getDownType() == UpdateHelper.DownType.down_auto_Install) {
                it.putExtra(DownloadService.KEY_AUTO_INSTALL, true);
            } else {
                it.putExtra(DownloadService.KEY_AUTO_INSTALL, false);
            }

            activity.startService(it);
            //判断是否显示进度弹窗
            if (UpdateHelper.getInstance().isShowProgressDialog()) {
                downProgressDialog(activity);
            }

        } else {
            Toast.makeText(activity, "正在进行下载任务，请稍后", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 下载时的进度条弹窗
     *
     * @param activity
     */
    private void downProgressDialog(final Activity activity) {

        // 动态注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_BROADCAST);
        broadcastReceiver = new UpdateProgressBroadcastReceiver();
        activity.registerReceiver(broadcastReceiver, filter);


        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_update_progress_view, null);

        progresscontent = (TextView) view.findViewById(R.id.content);
        Button btn_cancle = (Button) view.findViewById(R.id.btn_cancle);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setView(view);

        downDialog = builder.show();

        downDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // 注销广播
                if (broadcastReceiver != null) {
                    activity.unregisterReceiver(broadcastReceiver);
                }
            }
        });

        btn_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downDialog.dismiss();
                UpdateHelper.getInstance().setDownloadCancle(true);
            }
        });


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

            if (type == DownloadService.DOWN_LOADING) {

                if (!downDialog.isShowing()) {
                    downDialog.show();
                }
                //更新弹窗进度
                progressBar.setMax(intent.getIntExtra(DownloadService.KEY_BROADCAST_TOTAL, 0) / 1000);
                progressBar.setProgress(intent.getIntExtra(DownloadService.KEY_BROADCAST_COUNT, 0) / 1000);

                progresscontent.setText((formatDouble(intent.getIntExtra(DownloadService.KEY_BROADCAST_COUNT, 0) / 1024.0 / 1024.0)) + "M/" + (formatDouble(intent.getIntExtra(DownloadService.KEY_BROADCAST_TOTAL, 0) / 1024.0 / 1024.0)) + "M");

            }

            //关闭弹窗
            if (type == DownloadService.DOWN_SUCCESS) {
                if (downDialog != null) {
                    downDialog.dismiss();
                }
            }
            //关闭弹窗
            if (type == DownloadService.DOWN_ERROR) {

                if (downDialog != null) {
                    downDialog.dismiss();
                }
            }
        }
    }

    private BigDecimal formatDouble(double d) {

        BigDecimal bd = new BigDecimal(d);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP);
    }


    /**
     * 判断某个服务是否正在运行的方法
     *
     * @param mContext
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
