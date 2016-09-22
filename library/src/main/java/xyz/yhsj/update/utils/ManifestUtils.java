package xyz.yhsj.update.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ManifestUtils {

	/**
	 * 获取AndroidManifest META_DATA 内容
	 * @param context
	 * @param key
	 * @return
	 */
	public static String getMetaData(Context context,String key) {
		ApplicationInfo appInfo;
		try {
			appInfo = context.getPackageManager()
					.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			return appInfo.metaData.getString(key);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
		
	}

}


