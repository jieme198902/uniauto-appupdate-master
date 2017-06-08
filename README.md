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

## Multiple build variants

If your library uses multiple flavours then see this example:


## Adding the maven plugin

To enable installing into local maven repository and JitPack you need to add the [android-maven](https://github.com/dcendents/android-maven-gradle-plugin) plugin:

1. Add `classpath 'com.github.dcendents:android-maven-gradle-plugin:1.3'` to root build.gradle under `buildscript { dependencies {`
2. Add `com.github.dcendents.android-maven` to the library/build.gradle

After these changes you should be able to run:

    ./gradlew install
    
from the root of your project. If install works and you have added a GitHub release it should work on jitpack.io

## Adding a sample app 

If you add a sample app to the same repo then your app needs to have a dependency on the library. To do this in your app/build.gradle add:

```gradle
    dependencies {
        compile project(':library')
    }
```

## add a call method simple
#初始化
UpdateHelper.init(this);
//具体使用的时候
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
        UpdateHelper.getInstance().appKey("5fb9c5849535c13917c2cf9baaece6ef9693ef27")
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
                                                new AlertDialog.Builder(MainActivity.this)
                                                        .setTitle("提示")
                                                        .setMessage("app需要开启写存储的权限才能使用此功能")
                                                        .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                                                                MainActivity.this.startActivity(intent);
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

