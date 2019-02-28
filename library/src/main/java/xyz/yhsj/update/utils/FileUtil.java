package xyz.yhsj.update.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * 类描述：FileUtil
 * <p>
 * Created by LOVE on 2016/8/31 031.
 */
public class FileUtil {

    public static File updateDir = null;
    public static File updateFile = null;
    /***********
     * 保存升级APK的目录
     ***********/
    public static final String KonkaApplication = "UpdateApp";

    /**
     * 方法描述：createFile方法
     *
     * @param app_name
     * @return
     * @see FileUtil
     */
    public static boolean createFile(String app_name) {
        boolean isCreateFileSuccess;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            isCreateFileSuccess = true;

            updateDir = new File(Environment.getExternalStorageDirectory() + "/" + KonkaApplication + "/");
            updateFile = new File(updateDir + "/" + app_name + ".apk");

            if (!updateDir.exists()) {
                updateDir.mkdirs();
            }
            if (!updateFile.exists()) {
                try {
                    updateFile.createNewFile();
                } catch (IOException e) {
                    isCreateFileSuccess = false;
                    e.printStackTrace();
                }
            }

        } else {
            isCreateFileSuccess = false;
        }
        return isCreateFileSuccess;
    }
}