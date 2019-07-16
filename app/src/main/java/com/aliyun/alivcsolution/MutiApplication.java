package com.aliyun.alivcsolution;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;


//import com.aliyun.alivcsolution.utils.LeakcanaryUtil;



/**
 * Created by Mulberry on 2018/2/24.
 */
public class MutiApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();



        //初始化播放器
//        AliVcMediaPlayer.init(getApplicationContext());

//        LeakcanaryUtil.initLeakCanary(this);

        ////设置保存密码。此密码如果更换，则之前保存的视频无法播放
        //AliyunDownloadConfig config = new AliyunDownloadConfig();
        //config.setSecretImagePath(Environment.getExternalStorageDirectory().getAbsolutePath()+"/aliyun/encryptedApp.dat");
        ////        config.setDownloadPassword("123456789");
        ////设置保存路径。请确保有SD卡访问权限。
        //config.setDownloadDir(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test_save/");
        ////设置同时下载个数
        //config.setMaxNums(2);

        //AliyunDownloadManager.getInstance(this).setDownloadConfig(config);

    }

}
