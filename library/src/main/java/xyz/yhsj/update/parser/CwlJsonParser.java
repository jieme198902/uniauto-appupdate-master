package xyz.yhsj.update.parser;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.yhsj.update.ParseData;
import xyz.yhsj.update.bean.UpdateEntity;

/**
 * 默认的json解析类
 * Created by LOVE on 2016/9/5 005.
 */

public class CwlJsonParser implements ParseData {

    private Context context;
    private Integer app_name;

    public CwlJsonParser(Context context, Integer app_name) {
        this.context = context;
        this.app_name = app_name;
    }

    @Override
    public UpdateEntity parse(String httpResponse) {
        UpdateEntity updateEntity = new UpdateEntity();
        try {
            if (TextUtils.isEmpty(httpResponse)) {
                return null;
            }
            JSONObject jsonObject = new JSONObject(httpResponse);
            if ("C1000".endsWith(jsonObject.getString("code"))) {
                JSONObject dataobj = jsonObject.getJSONObject("data");
                updateEntity.setName(context.getResources().getString(app_name));
                if (!dataobj.isNull("versionCode")) {
                    updateEntity.setVersionCode(dataobj.getInt("versionCode") + "");
                } else {
                    updateEntity.setVersionCode("");
                }
                if (!dataobj.isNull("updateDesc")) {
                    updateEntity.setContent(dataobj.getString("updateDesc"));
                } else {
                    updateEntity.setContent("修复了若干bug");
                }
                if (!dataobj.isNull("downloadUrl")) {
                    updateEntity.setUpdateUrl(dataobj.getString("downloadUrl"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return updateEntity;
    }
}
