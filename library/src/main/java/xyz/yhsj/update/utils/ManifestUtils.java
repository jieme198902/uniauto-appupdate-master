package xyz.yhsj.update.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * 一个小帮助类
 */
public class ManifestUtils {

    /**
     * 获取AndroidManifest META_DATA 内容
     *
     * @param context 上下文
     * @param key     key
     * @return
     */
    public static String getMetaData(Context context, String key) {
        ApplicationInfo appInfo;
        try {
            appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return appInfo.metaData.getString(key);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "web";
    }

    /**
     * packageName
     *
     * @param context 上下文
     * @return
     */
    public static String getPackName(Context context) {
        return context.getPackageName();
    }

    /**
     * 获取本地软件版本号
     */
    public static String getVersionCode(Context ctx) {
        int versionCode = 0;
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode + "";
    }

}


