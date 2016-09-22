package xyz.yhsj.update;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import xyz.yhsj.update.listener.UpdateListener;
import xyz.yhsj.update.net.HttpMetHod;
import xyz.yhsj.update.net.UpdateAgent;

/**
 * 升级帮助类
 * Created by LOVE on 2016/8/31 031.
 */
public class UpdateHelper {
    private Context mContext;
    private String checkUrl;
    private Map<String, String> params;
    private ParseData jsonParser;
    private boolean showProgressDialog;

    private static UpdateHelper instance;

    //双重嵌套一级是否强制更新
    private boolean updateForce = false;

    private boolean downloadCancel = false;

    private String appKey;
    //联网请求方式
    private HttpMetHod httpMetHod = HttpMetHod.GET;

    //默认需要检测更新
    private CheckType checkType = CheckType.check_no_Dialog;
    private DownType downType = DownType.down_auto_Install;
    private UpdateWithOut updateWithOut = UpdateWithOut.tip_without;

    //辅助获取检测结果的回调
    private UpdateListener updateListener;

    private boolean onlyCheck;


    //检测更新类型
    public enum CheckType {
        check_with_Dialog,
        check_no_Dialog
    }

    //无更新类型
    public enum UpdateWithOut {
        tip_without,
        tip_toast,
        tip_dialog
    }

    //下载更新类型
    public enum DownType {
        down_auto_Install,
        down_click_Install
    }

    //获取单例对象
    public static UpdateHelper getInstance() {
        if (instance == null) {
            throw new RuntimeException("UpdateHelper not initialized , must call init first !!!");
        } else {
            return instance;
        }
    }

    /**
     * 设置appKey
     *
     * @param appKey
     * @return
     */
    public UpdateHelper appKey(String appKey) {
        this.appKey = appKey;
        return this;
    }

    public static void init(Context appContext) {
        instance = new UpdateHelper(appContext);
    }

    public UpdateHelper(Context context) {
        this.mContext = context;
    }

    public UpdateHelper setUpdateListener(boolean onlyCheck, UpdateListener updateListener) {
        this.onlyCheck = onlyCheck;
        this.updateListener = updateListener;
        return this;
    }

    public UpdateHelper get(String url) {
        this.checkUrl = url;
        this.httpMetHod = HttpMetHod.GET;
        return this;
    }

    public UpdateHelper post(String url, HashMap<String, String> params) {
        this.checkUrl = url;
        this.params = params;
        this.httpMetHod = HttpMetHod.POST;
        return this;
    }

    public UpdateHelper setJsonParser(ParseData jsonParser) {
        this.jsonParser = jsonParser;
        return this;
    }

    public UpdateHelper setCheckType(CheckType checkType) {
        this.checkType = checkType;
        return this;
    }

    public UpdateHelper setUpdateWithOut(UpdateWithOut updateWithOut) {
        this.updateWithOut = updateWithOut;
        return this;
    }

    public UpdateHelper setDownType(DownType downType) {
        this.downType = downType;
        return this;
    }

    public UpdateHelper showProgressDialog(boolean showProgressDialog) {
        this.showProgressDialog = showProgressDialog;
        return this;
    }


    public Context getContext() {
        if (mContext == null) {
            throw new RuntimeException("should call UpdateHelper.init first");
        }
        return mContext;
    }

    public CheckType getCheckType() {
        return checkType;
    }

    public DownType getDownType() {
        return downType;
    }

    public String getUrl() {
        if (TextUtils.isEmpty(checkUrl)) {
            throw new IllegalArgumentException("Url is null");
        }
        return checkUrl;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getAppKey() {
        return appKey;
    }

    public HttpMetHod getHttpMethod() {
        return httpMetHod;
    }

    public ParseData getJsonParser() {
        if (jsonParser == null) {
            throw new IllegalStateException("update parser is null");
        }
        return jsonParser;
    }

    public boolean isOnlyCheck() {
        return onlyCheck;
    }

    public UpdateWithOut getUpdateWithOut() {
        return updateWithOut;
    }


    public boolean isShowProgressDialog() {
        return showProgressDialog;
    }

    public boolean isDownloadCancel() {
        return downloadCancel;
    }

    public void setDownloadCancel(boolean downloadCdownloadCancelancle) {
        this.downloadCancel = downloadCancel;
    }

    public UpdateListener getUpdateListener() {
        return updateListener;
    }

    public void check(Activity activity) {
        UpdateAgent.getInstance().checkUpdate(activity);
    }

}
