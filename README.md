# uniauto-appupdate

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.github.jieme198902:uniauto-appupdate-master:{latest version}'
}
```

## 在Application中初始化
```java
    @Override
    public void onCreate() {
        super.onCreate();
        UpdateHelper.init(this);
    }
```
UpdateHelper.init(this);
### 具体使用代码的时候
UpdateHelper.init(this);
```java
    /**
     * 检测新版本。
     */
    private void checkVersionInfo() {
        //这里即请求版本信息,也更新
        HashMap<String, String> param = new HashMap<>();
        param.put("oldVersion", ManifestUtils.getVersionCode(this));
        String channelName = "web";
        try {
            Bundle bundle = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (null != bundle && !TextUtils.isEmpty(bundle.getString("UMENG_CHANNEL"))) {
                channelName = bundle.getString("UMENG_CHANNEL");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        param.put("channelName", channelName);
        param.put("packname", ManifestUtils.getPackName(this));
        UpdateHelper.getInstance().appKey("5279c5849535c13917c227927272762796932727")
                .post(Constants.checkVersion, param)
                .setJsonParser(new CwlJsonParser(MainActivity.this))
                //             这个true是否往下走,进行版本更新
                .setUpdateListener(false, new UpdateListener() {
                    //                  update 这里放的是否有新版本
                    @Override
                    public void Update(boolean update, UpdateEntity updateEntity) {
                        if (update) {
                            show_version_text.setText("发现新版本");
                            RxPermissions.getInstance(MainActivity.this)
                                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .subscribe(new Action1<Boolean>() {
                                        @Override
                                        public void call(Boolean aBoolean) {
                                            if (aBoolean) {
                                            } else {
                                                new AlertDialog.Builder(context)
                                                        .setTitle("提示")
                                                        .setMessage("app需要开启写存储的权限才能使用此功能")
                                                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                               Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                               intent.setData(Uri.parse("package:" + getPackageName()));
                                                               startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton("取消", null)
                                                        .create()
                                                        .show();
                                            }
                                        }
                                    });
                        } else {
                            show_version_text.setText("已是最新版本");
                        }
                    }
                })
                .check(this);
    }
```
