package com.aliyun.vodplayerview.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorCode;
import com.aliyun.player.nativeclass.MediaInfo;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidSts;
import com.aliyun.private_service.PrivateService;
import com.aliyun.utils.VcPlayerLog;
import com.aliyun.vodplayer.R;
import com.aliyun.vodplayerview.constants.PlayParameter;
import com.aliyun.vodplayerview.listener.OnChangeQualityListener;
import com.aliyun.vodplayerview.listener.OnStoppedListener;
import com.aliyun.vodplayerview.listener.RefreshStsCallback;
import com.aliyun.vodplayerview.playlist.AlivcPlayListAdapter;
import com.aliyun.vodplayerview.playlist.AlivcPlayListManager;
import com.aliyun.vodplayerview.playlist.AlivcVideoInfo;
import com.aliyun.vodplayerview.utils.Common;
import com.aliyun.vodplayerview.utils.FixedToastUtils;
import com.aliyun.vodplayerview.utils.ScreenUtils;
import com.aliyun.vodplayerview.utils.VidStsUtil;
import com.aliyun.vodplayerview.utils.database.DatabaseManager;
import com.aliyun.vodplayerview.utils.database.LoadDbDatasListener;
import com.aliyun.vodplayerview.utils.download.AliyunDownloadInfoListener;
import com.aliyun.vodplayerview.utils.download.AliyunDownloadManager;
import com.aliyun.vodplayerview.utils.download.AliyunDownloadMediaInfo;
import com.aliyun.vodplayerview.view.choice.AlivcShowMoreDialog;
import com.aliyun.vodplayerview.view.control.ControlView;
import com.aliyun.vodplayerview.view.download.AddDownloadView;
import com.aliyun.vodplayerview.view.download.AlivcDialog;
import com.aliyun.vodplayerview.view.download.AlivcDialog.onCancelOnclickListener;
import com.aliyun.vodplayerview.view.download.AlivcDialog.onConfirmClickListener;
import com.aliyun.vodplayerview.view.download.AlivcDownloadMediaInfo;
import com.aliyun.vodplayerview.view.download.DownloadChoiceDialog;
import com.aliyun.vodplayerview.view.download.DownloadDataProvider;
import com.aliyun.vodplayerview.view.download.DownloadView;
import com.aliyun.vodplayerview.view.download.DownloadView.OnDownloadViewListener;
import com.aliyun.vodplayerview.view.gesturedialog.BrightnessDialog;
import com.aliyun.vodplayerview.view.more.AliyunShowMoreValue;
import com.aliyun.vodplayerview.view.more.ShowMoreView;
import com.aliyun.vodplayerview.view.more.SpeedValue;
import com.aliyun.vodplayerview.view.tipsview.ErrorInfo;
import com.aliyun.vodplayerview.widget.AliyunScreenMode;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView.OnPlayerViewClickListener;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView.PlayViewType;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 播放器和播放列表界面 Created by Mulberry on 2018/4/9.
 */
public class AliyunPlayerSkinActivity extends BaseActivity {

    private PlayerHandler playerHandler;
    private DownloadView dialogDownloadView;
    private AlivcShowMoreDialog showMoreDialog;

    private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SS");
    private List<String> logStrs = new ArrayList<>();

    private AliyunScreenMode currentScreenMode = AliyunScreenMode.Small;
    private TextView tvLogs;
    private TextView tvTabLogs;
    private TextView tvTabDownloadVideo;
    private ImageView ivLogs;
    private ImageView ivDownloadVideo;
    private LinearLayout llClearLogs;
    private RelativeLayout rlLogsContent;
    private RelativeLayout rlDownloadManagerContent;
    private TextView tvVideoList;
    private ImageView ivVideoList;
    private RecyclerView recyclerView;
    private LinearLayout llVideoList;
    private TextView tvStartSetting;

    private DownloadView downloadView;
    private AliyunVodPlayerView mAliyunVodPlayerView = null;

    private DownloadDataProvider downloadDataProvider;
    private AliyunDownloadManager downloadManager;
    private AlivcPlayListAdapter alivcPlayListAdapter;

    private ArrayList<AlivcVideoInfo.Video> alivcVideoInfos;
    private ErrorInfo currentError = ErrorInfo.Normal;
    //判断是否在后台
    private boolean mIsInBackground = false;
    /**
     * 开启设置界面的请求码
     */
    private static final int CODE_REQUEST_SETTING = 1000;
    /**
     * 设置界面返回的结果码, 100为vid类型, 200为url类型
     */
    private static final int CODE_RESULT_TYPE_VID = 100;
    private static final int CODE_RESULT_TYPE_URL = 200;
    private static final String DEFAULT_URL = "http://player.alicdn.com/video/aliyunmedia.mp4";
    private static final String DEFAULT_VID = "6e783360c811449d8692b2117acc9212";
    /**
     * get StsToken stats
     */
    private boolean inRequest;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    /**
     * 当前tab
     */
    private int currentTab = TAB_VIDEO_LIST;
    private static final int TAB_VIDEO_LIST = 1;
    private static final int TAB_LOG = 2;
    private static final int TAB_DOWNLOAD_LIST = 3;
    private Common commenUtils;
    private long oldTime;
    private long downloadOldTime;
    private static String preparedVid;

    private AliyunScreenMode mCurrentDownloadScreenMode;

    /**
     * 是否需要展示下载界面,如果是恢复数据,则不用展示下载界面
     */
    private boolean showAddDownloadView;

    /**
     * 是否鉴权过期
     */
    private boolean mIsTimeExpired = false;
    /**
     * 判断是否在下载中
     */
    private boolean mDownloadInPrepare = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (isStrangePhone()) {
            //            setTheme(R.style.ActTheme);
        } else {
            setTheme(R.style.NoActionTheme);
        }
        copyAssets();
        DatabaseManager.getInstance().createDataBase(this);

        super.onCreate(savedInstanceState);
        showAddDownloadView = false;
        setContentView(R.layout.alivc_player_layout_skin);

        requestVidSts();
        initAliyunPlayerView();
        initLogView();
        initDownloadView();
        initVideoListView();
    }

    /**
     * 设置屏幕亮度
     */
    private void setWindowBrightness(int brightness) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255.0f;
        window.setAttributes(lp);
    }

    private void copyAssets() {
        commenUtils = Common.getInstance(getApplicationContext()).copyAssetsToSD("encrypt", "aliyun");
        commenUtils.setFileOperateCallback(

                new Common.FileOperateCallback() {
                    @Override
                    public void onSuccess() {
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save/");
                        if (!file.exists()) {
                            file.mkdir();
                        }

                        // 获取AliyunDownloadManager对象
                        downloadManager = AliyunDownloadManager.getInstance(getApplicationContext());
                        downloadManager.setEncryptFilePath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/aliyun/encryptedApp.dat");
                        PrivateService.initService(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/aliyun/encryptedApp.dat");
                        downloadManager.setDownloadDir(file.getAbsolutePath());
                        //设置同时下载个数
                        downloadManager.setMaxNum(3);

                        downloadDataProvider = DownloadDataProvider.getSingleton(getApplicationContext());
                        // 更新sts回调
                        downloadManager.setRefreshStsCallback(new MyRefreshStsCallback());

                        // 视频下载的回调
                        downloadManager.setDownloadInfoListener(new MyDownloadInfoListener(AliyunPlayerSkinActivity.this));
                        downloadViewSetting(downloadView);
                    }

                    @Override
                    public void onFailed(String error) {
                    }
                });
    }

    private void initAliyunPlayerView() {
        mAliyunVodPlayerView = (AliyunVodPlayerView) findViewById(R.id.video_view);
        //保持屏幕敞亮
        mAliyunVodPlayerView.setKeepScreenOn(true);
        PlayParameter.PLAY_PARAM_URL = DEFAULT_URL;
        String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save_cache";
        mAliyunVodPlayerView.setPlayingCache(false, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/);
        mAliyunVodPlayerView.setTheme(AliyunVodPlayerView.Theme.Blue);
        //mAliyunVodPlayerView.setCirclePlay(true);
        mAliyunVodPlayerView.setAutoPlay(true);

        mAliyunVodPlayerView.setOnPreparedListener(new MyPrepareListener(this));
        mAliyunVodPlayerView.setNetConnectedListener(new MyNetConnectedListener(this));
        mAliyunVodPlayerView.setOnCompletionListener(new MyCompletionListener(this));
        mAliyunVodPlayerView.setOnFirstFrameStartListener(new MyFrameInfoListener(this));
        mAliyunVodPlayerView.setOnChangeQualityListener(new MyChangeQualityListener(this));
        //TODO
        mAliyunVodPlayerView.setOnStoppedListener(new MyStoppedListener(this));
        mAliyunVodPlayerView.setmOnPlayerViewClickListener(new MyPlayViewClickListener(this));
        mAliyunVodPlayerView.setOrientationChangeListener(new MyOrientationChangeListener(this));
//        mAliyunVodPlayerView.setOnUrlTimeExpiredListener(new MyOnUrlTimeExpiredListener(this));
        mAliyunVodPlayerView.setOnTimeExpiredErrorListener(new MyOnTimeExpiredErrorListener(this));
        mAliyunVodPlayerView.setOnShowMoreClickListener(new MyShowMoreClickLisener(this));
        mAliyunVodPlayerView.setOnPlayStateBtnClickListener(new MyPlayStateBtnClickListener(this));
        mAliyunVodPlayerView.setOnSeekCompleteListener(new MySeekCompleteListener(this));
        mAliyunVodPlayerView.setOnSeekStartListener(new MySeekStartListener(this));
        mAliyunVodPlayerView.setOnScreenBrightness(new MyOnScreenBrightnessListener(this));
        mAliyunVodPlayerView.setOnErrorListener(new MyOnErrorListener(this));
        mAliyunVodPlayerView.setScreenBrightness(BrightnessDialog.getActivityBrightness(AliyunPlayerSkinActivity.this));
        mAliyunVodPlayerView.enableNativeLog();
    }

    /**
     * 请求sts
     */
    private void requestVidSts() {
        if (inRequest) {
            return;
        }
        inRequest = true;
        if (TextUtils.isEmpty(PlayParameter.PLAY_PARAM_VID)) {
            PlayParameter.PLAY_PARAM_VID = DEFAULT_VID;
        }
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new MyStsListener(this));
    }

    /**
     * 获取播放列表数据
     */
    private void loadPlayList() {

        AlivcPlayListManager.getInstance().fetchPlayList(PlayParameter.PLAY_PARAM_AK_ID,
                PlayParameter.PLAY_PARAM_AK_SECRE,
                PlayParameter.PLAY_PARAM_SCU_TOKEN, new AlivcPlayListManager.PlayListListener() {
                    @Override
                    public void onPlayList(int code, final ArrayList<AlivcVideoInfo.Video> videos) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
                                    alivcVideoInfos.clear();
                                    alivcVideoInfos.addAll(videos);
                                    alivcPlayListAdapter.notifyDataSetChanged();

                                    // 请求sts成功后, 加载播放资源,和视频列表
                                    AlivcVideoInfo.Video video = alivcVideoInfos.get(0);
                                    PlayParameter.PLAY_PARAM_VID = video.getVideoId();
                                    //url/vid设置界面播放后,
                                    PlayParameter.PLAY_PARAM_TYPE = "vidsts";
                                    setPlaySource();
                                }

                            }
                        });
                    }

                });
    }

    /**
     * init视频列表tab
     */
    private void initVideoListView() {
        tvVideoList = findViewById(R.id.tv_tab_video_list);
        ivVideoList = findViewById(R.id.iv_video_list);
        recyclerView = findViewById(R.id.video_list);
        llVideoList = findViewById(R.id.ll_video_list);
        tvStartSetting = findViewById(R.id.tv_start_player);
        alivcVideoInfos = new ArrayList<AlivcVideoInfo.Video>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        alivcPlayListAdapter = new AlivcPlayListAdapter(this, alivcVideoInfos);

        ivVideoList.setActivated(true);
        llVideoList.setVisibility(View.VISIBLE);
        rlDownloadManagerContent.setVisibility(View.GONE);
        rlLogsContent.setVisibility(View.GONE);

        tvVideoList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTab = TAB_VIDEO_LIST;
                ivVideoList.setActivated(true);
                ivLogs.setActivated(false);
                ivDownloadVideo.setActivated(false);
//                downloadView.changeDownloadEditState(false);
                llVideoList.setVisibility(View.VISIBLE);
                rlDownloadManagerContent.setVisibility(View.GONE);
                rlLogsContent.setVisibility(View.GONE);
            }
        });

        recyclerView.setAdapter(alivcPlayListAdapter);

        alivcPlayListAdapter.setOnVideoListItemClick(new AlivcPlayListAdapter.OnVideoListItemClick() {
            @Override
            public void onItemClick(int position) {
                long currentClickTime = System.currentTimeMillis();
                // 防止快速点击
                if (currentClickTime - oldTime <= 2000) {
                    return;
                }
                PlayParameter.PLAY_PARAM_TYPE = "vidsts";
                // 点击视频列表, 切换播放的视频
                changePlaySource(position);
                oldTime = currentClickTime;
            }
        });

        // 开启vid和url设置界面
        tvStartSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AliyunPlayerSkinActivity.this, AliyunPlayerSettingActivity.class);
                // 开启时, 默认为vid
                startActivityForResult(intent, CODE_REQUEST_SETTING);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadPlayList();
        //vid/url设置界面并且是取消
        if (requestCode == CODE_REQUEST_SETTING && resultCode == Activity.RESULT_CANCELED) {
            return;
        } else if (requestCode == CODE_REQUEST_SETTING && resultCode == Activity.RESULT_OK) {
            setPlaySource();
        }
    }

    private int currentVideoPosition;

    /**
     * 切换播放资源
     *
     * @param position 需要播放的数据在集合中的下标
     */
    private void changePlaySource(int position) {

        currentVideoPosition = position;

        AlivcVideoInfo.Video video = alivcVideoInfos.get(position);

        changePlayVidSource(video);
    }

    /**
     * 播放本地资源
     */
    private void changePlayLocalSource(String url, String title) {
        UrlSource urlSource = new UrlSource();
        urlSource.setUri(url);
        urlSource.setTitle(title);
        mAliyunVodPlayerView.setLocalSource(urlSource);
    }

    /**
     * 切换播放vid资源
     *
     * @param video 要切换的资源
     */
    private void changePlayVidSource(AlivcVideoInfo.Video video) {
        VidSts vidSts = new VidSts();
        PlayParameter.PLAY_PARAM_VID = video.getVideoId();
        mAliyunVodPlayerView.setAutoPlay(!mIsInBackground);
        //切换资源重置下载flag
        mDownloadInPrepare = false;
        /**
         * 如果是鉴权过期
         */
        if (mIsTimeExpired) {
            onTimExpiredError();
        } else {
            vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
            vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
            vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
            vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
            vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
            vidSts.setTitle(video.getTitle());
            mAliyunVodPlayerView.setVidSts(vidSts);
        }

    }

    /**
     * 下载重新调用onPrepared方法,否则会出现断点续传的现象
     * 而且断点续传出错
     */
    private void callDownloadPrepare(String vid, String title) {
        VidSts vidSts = new VidSts();
        vidSts.setVid(vid);
        vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
        vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
        vidSts.setTitle(title);
        downloadManager.prepareDownload(vidSts);
    }

    /**
     * init 日志tab
     */
    private void initLogView() {
        tvLogs = (TextView) findViewById(R.id.tv_logs);
        tvTabLogs = (TextView) findViewById(R.id.tv_tab_logs);
        ivLogs = (ImageView) findViewById(R.id.iv_logs);
        llClearLogs = (LinearLayout) findViewById(R.id.ll_clear_logs);
        rlLogsContent = (RelativeLayout) findViewById(R.id.rl_logs_content);

        //日志Tab默认不选择
        ivLogs.setActivated(false);

        //日志清除
        llClearLogs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                logStrs.clear();
                tvLogs.setText("");
            }
        });

        tvTabLogs.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                currentTab = TAB_LOG;
                // TODO: 2018/4/10 show logs contents view
                ivLogs.setActivated(true);
                ivDownloadVideo.setActivated(false);
                ivVideoList.setActivated(false);
                rlLogsContent.setVisibility(View.VISIBLE);
//                downloadView.changeDownloadEditState(false);
                rlDownloadManagerContent.setVisibility(View.GONE);
                llVideoList.setVisibility(View.GONE);
            }
        });
    }

    /**
     * init下载(离线视频)tab
     */
    private void initDownloadView() {
        tvTabDownloadVideo = (TextView) findViewById(R.id.tv_tab_download_video);
        ivDownloadVideo = (ImageView) findViewById(R.id.iv_download_video);
        rlDownloadManagerContent = (RelativeLayout) findViewById(R.id.rl_download_manager_content);
        downloadView = (DownloadView) findViewById(R.id.download_view);
        //离线下载Tab默认不选择
        ivDownloadVideo.setActivated(false);
        tvTabDownloadVideo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                currentTab = TAB_DOWNLOAD_LIST;
                // TODO: 2018/4/10 show download content
                ivDownloadVideo.setActivated(true);
                ivLogs.setActivated(false);
                ivVideoList.setActivated(false);
                rlLogsContent.setVisibility(View.GONE);
                llVideoList.setVisibility(View.GONE);
                rlDownloadManagerContent.setVisibility(View.VISIBLE);
                //Drawable drawable = getResources().getDrawable(R.drawable.alivc_new_download);
                //drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

                updateDownloadTaskTip();
            }
        });
    }

    /**
     * downloadView的配置 里面配置了需要下载的视频的信息, 事件监听等 抽取该方法的主要目的是, 横屏下download dialog的离线视频列表中也用到了downloadView, 而两者显示内容和数据是同步的,
     * 所以在此进行抽取 AliyunPlayerSkinActivity.class#showAddDownloadView(DownloadVie view)中使用
     */
    private void downloadViewSetting(final DownloadView downloadView) {
        downloadDataProvider.restoreMediaInfo(new LoadDbDatasListener() {
            @Override
            public void onLoadSuccess(List<AliyunDownloadMediaInfo> dataList) {
                if (downloadView != null) {
                    downloadView.addAllDownloadMediaInfo(dataList);
                }
            }
        });

        downloadView.setOnDownloadViewListener(new OnDownloadViewListener() {
            @Override
            public void onStop(AliyunDownloadMediaInfo downloadMediaInfo) {
                downloadManager.stopDownload(downloadMediaInfo);
            }

            @Override
            public void onStart(AliyunDownloadMediaInfo downloadMediaInfo) {
                downloadManager.startDownload(downloadMediaInfo);
            }

            @Override
            public void onDeleteDownloadInfo(final ArrayList<AlivcDownloadMediaInfo> alivcDownloadMediaInfos) {
                // 视频删除的dialog
                final AlivcDialog alivcDialog = new AlivcDialog(AliyunPlayerSkinActivity.this);
                alivcDialog.setDialogIcon(R.drawable.icon_delete_tips);
                alivcDialog.setMessage(getResources().getString(R.string.alivc_delete_confirm));
                alivcDialog.setOnConfirmclickListener(getResources().getString(R.string.alivc_dialog_sure),
                        new onConfirmClickListener() {
                            @Override
                            public void onConfirm() {
                                alivcDialog.dismiss();
                                if (alivcDownloadMediaInfos != null && alivcDownloadMediaInfos.size() > 0) {
                                    downloadView.deleteDownloadInfo();
                                    if (dialogDownloadView != null) {
                                        dialogDownloadView.deleteDownloadInfo();
                                    }
                                    if (downloadManager != null) {
                                        for (AlivcDownloadMediaInfo alivcDownloadMediaInfo : alivcDownloadMediaInfos) {
                                            downloadManager.deleteFile(alivcDownloadMediaInfo.getAliyunDownloadMediaInfo());
                                        }

                                    }
                                    downloadDataProvider.deleteAllDownloadInfo(alivcDownloadMediaInfos);
                                } else {
                                    FixedToastUtils.show(AliyunPlayerSkinActivity.this, "没有删除的视频选项...");
                                }
                            }
                        });
                alivcDialog.setOnCancelOnclickListener(getResources().getString(R.string.alivc_dialog_cancle),
                        new onCancelOnclickListener() {
                            @Override
                            public void onCancel() {
                                alivcDialog.dismiss();
                            }
                        });
                alivcDialog.show();
            }
        });

        downloadView.setOnDownloadedItemClickListener(new DownloadView.OnDownloadItemClickListener() {
            @Override
            public void onDownloadedItemClick(final int positin) {
                ArrayList<AlivcDownloadMediaInfo> allDownloadMediaInfo = downloadView.getAllDownloadMediaInfo();
                if (positin < 0) {
                    FixedToastUtils.show(AliyunPlayerSkinActivity.this, "视频资源不存在");
                    return;
                }
                // 如果点击列表中的视频, 需要将类型改为vid
                AliyunDownloadMediaInfo aliyunDownloadMediaInfo = allDownloadMediaInfo.get(positin).getAliyunDownloadMediaInfo();
                PlayParameter.PLAY_PARAM_TYPE = "localSource";
                if (aliyunDownloadMediaInfo != null) {
                    PlayParameter.PLAY_PARAM_URL = aliyunDownloadMediaInfo.getSavePath();
                    mAliyunVodPlayerView.updateScreenShow();
                    changePlayLocalSource(PlayParameter.PLAY_PARAM_URL, aliyunDownloadMediaInfo.getTitle());
                }
            }

            @Override
            public void onDownloadingItemClick(ArrayList<AlivcDownloadMediaInfo> infos, int position) {
                AlivcDownloadMediaInfo alivcInfo = infos.get(position);
                AliyunDownloadMediaInfo aliyunDownloadInfo = alivcInfo.getAliyunDownloadMediaInfo();
                AliyunDownloadMediaInfo.Status status = aliyunDownloadInfo.getStatus();
                if (status == AliyunDownloadMediaInfo.Status.Error || status == AliyunDownloadMediaInfo.Status.Wait) {
                    //downloadManager.removeDownloadMedia(aliyunDownloadInfo);
                    downloadManager.startDownload(aliyunDownloadInfo);
                }
            }
        });
    }

    private static class MyPrepareListener implements IPlayer.OnPreparedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyPrepareListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<>(skinActivity);
        }

        @Override
        public void onPrepared() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onPrepared();
            }
        }
    }

    private void onPrepared() {
        logStrs.add(format.format(new Date()) + getString(R.string.log_prepare_success));

        for (String log : logStrs) {
            tvLogs.append(log + "\n");
        }
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_prepare_success);
    }

    private static class MyCompletionListener implements IPlayer.OnCompletionListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyCompletionListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onCompletion() {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onCompletion();
            }
        }
    }

    private void onCompletion() {
        logStrs.add(format.format(new Date()) + getString(R.string.log_play_completion));
        for (String log : logStrs) {
            tvLogs.append(log + "\n");
        }
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.toast_play_compleion);

        // 当前视频播放结束, 播放下一个视频
        if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            onNext();
        }
    }

    /**
     * 播放下一个视频
     */
    private void onNext() {
        if (currentError == ErrorInfo.UnConnectInternet) {
            // 此处需要判断网络和播放类型
            // 网络资源, 播放完自动波下一个, 无网状态提示ErrorTipsView
            // 本地资源, 播放完需要重播, 显示Replay, 此处不需要处理
            if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
                mAliyunVodPlayerView.showErrorTipView(4014, "-1", "当前网络不可用");
            }
            return;
        }

        currentVideoPosition++;
        if (currentVideoPosition > alivcVideoInfos.size() - 1) {
            //列表循环播放，如发现播放完成了从列表的第一个开始重新播放
            currentVideoPosition = 0;
        }

        if (alivcVideoInfos.size() > 0) {
            AlivcVideoInfo.Video video = alivcVideoInfos.get(currentVideoPosition);
            if (video != null) {
                changePlayVidSource(video);
            }
        }
    }

    private static class MyFrameInfoListener implements IPlayer.OnRenderingStartListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyFrameInfoListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onRenderingStart() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onFirstFrameStart();
            }
        }
    }

    private void onFirstFrameStart() {
        if (mAliyunVodPlayerView != null) {
            Map<String, String> debugInfo = mAliyunVodPlayerView.getAllDebugInfo();
            if (debugInfo == null) {
                return;
            }
            long createPts = 0;
            if (debugInfo.get("create_player") != null) {
                String time = debugInfo.get("create_player");
                createPts = (long) Double.parseDouble(time);
                logStrs.add(format.format(new Date(createPts)) + getString(R.string.log_player_create_success));
            }
            if (debugInfo.get("open-url") != null) {
                String time = debugInfo.get("open-url");
                long openPts = (long) Double.parseDouble(time) + createPts;
                logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_open_url_success));
            }
            if (debugInfo.get("find-stream") != null) {
                String time = debugInfo.get("find-stream");
                long findPts = (long) Double.parseDouble(time) + createPts;
                logStrs.add(format.format(new Date(findPts)) + getString(R.string.log_request_stream_success));
            }
            if (debugInfo.get("open-stream") != null) {
                String time = debugInfo.get("open-stream");
                long openPts = (long) Double.parseDouble(time) + createPts;
                logStrs.add(format.format(new Date(openPts)) + getString(R.string.log_start_open_stream));
            }
            logStrs.add(format.format(new Date()) + getString(R.string.log_first_frame_played));
            for (String log : logStrs) {
                tvLogs.append(log + "\n");
            }
        }
    }

    private class MyPlayViewClickListener implements OnPlayerViewClickListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyPlayViewClickListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onClick(AliyunScreenMode screenMode, PlayViewType viewType) {
            long currentClickTime = System.currentTimeMillis();
            // 防止快速点击
            if (currentClickTime - oldTime <= 1000) {
                return;
            }
            oldTime = currentClickTime;
            // 如果当前的Type是Download, 就显示Download对话框
            if (viewType == PlayViewType.Download) {
                mCurrentDownloadScreenMode = screenMode;
                AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
                if (aliyunPlayerSkinActivity != null) {
                    aliyunPlayerSkinActivity.showAddDownloadView = true;
                }

                if (mAliyunVodPlayerView != null) {
                    MediaInfo currentMediaInfo = mAliyunVodPlayerView.getCurrentMediaInfo();
                    if (currentMediaInfo != null && currentMediaInfo.getVideoId().equals(PlayParameter.PLAY_PARAM_VID)) {
                        VidSts vidSts = new VidSts();
                        vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
                        vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
                        vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
                        vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
                        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
                        if (!mDownloadInPrepare) {
                            mDownloadInPrepare = true;
                            downloadManager.prepareDownload(vidSts);
                        }
                    }
                }
            }
        }
    }

    /**
     * 显示下载选择项 download 对话框
     *
     * @param screenMode
     */
    private void showAddDownloadView(AliyunScreenMode screenMode) {
        //这个时候视频的状态已经是delete了
        if (currentPreparedMediaInfo != null && currentPreparedMediaInfo.get(0).getVid().equals(preparedVid)) {
            downloadDialog = new DownloadChoiceDialog(this, screenMode);
            final AddDownloadView contentView = new AddDownloadView(this, screenMode);
            contentView.onPrepared(currentPreparedMediaInfo);
            contentView.setOnViewClickListener(viewClickListener);
            final View inflate = LayoutInflater.from(getApplicationContext()).inflate(
                    R.layout.alivc_dialog_download_video, null, false);
            dialogDownloadView = inflate.findViewById(R.id.download_view);
            downloadDialog.setContentView(contentView);
            downloadDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (dialogDownloadView != null) {
                        dialogDownloadView.setOnDownloadViewListener(null);
                        dialogDownloadView.setOnDownloadedItemClickListener(null);
                    }
                }
            });
            if (!downloadDialog.isShowing()) {
                downloadDialog.show();
            }
            downloadDialog.setCanceledOnTouchOutside(true);

            if (screenMode == AliyunScreenMode.Full) {
                contentView.setOnShowVideoListLisener(new AddDownloadView.OnShowNativeVideoBtnClickListener() {
                    @Override
                    public void onShowVideo() {
                        if (downloadDataProvider != null) {
                            downloadDataProvider.restoreMediaInfo(new LoadDbDatasListener() {
                                @Override
                                public void onLoadSuccess(List<AliyunDownloadMediaInfo> dataList) {
                                    if (dialogDownloadView != null) {
                                        dialogDownloadView.addAllDownloadMediaInfo(dataList);
                                    }
                                }
                            });
                        }
                        downloadDialog.setContentView(inflate);
                    }
                });

                dialogDownloadView.setOnDownloadViewListener(new OnDownloadViewListener() {
                    @Override
                    public void onStop(AliyunDownloadMediaInfo downloadMediaInfo) {
                        downloadManager.stopDownload(downloadMediaInfo);
                    }

                    @Override
                    public void onStart(AliyunDownloadMediaInfo downloadMediaInfo) {
                        downloadManager.startDownload(downloadMediaInfo);
                    }

                    @Override
                    public void onDeleteDownloadInfo(final ArrayList<AlivcDownloadMediaInfo> alivcDownloadMediaInfos) {
                        // 视频删除的dialog
                        final AlivcDialog alivcDialog = new AlivcDialog(AliyunPlayerSkinActivity.this);
                        alivcDialog.setDialogIcon(R.drawable.icon_delete_tips);
                        alivcDialog.setMessage(getResources().getString(R.string.alivc_delete_confirm));
                        alivcDialog.setOnConfirmclickListener(getResources().getString(R.string.alivc_dialog_sure),
                                new onConfirmClickListener() {
                                    @Override
                                    public void onConfirm() {
                                        alivcDialog.dismiss();
                                        if (alivcDownloadMediaInfos != null && alivcDownloadMediaInfos.size() > 0) {
                                            dialogDownloadView.deleteDownloadInfo();
                                            if (downloadView != null) {
                                                for (AlivcDownloadMediaInfo alivcDownloadMediaInfo : alivcDownloadMediaInfos) {
                                                    if (alivcDownloadMediaInfo.isCheckedState()) {
                                                        downloadView.deleteDownloadInfo(alivcDownloadMediaInfo.getAliyunDownloadMediaInfo());
                                                    }
                                                }

                                            }
                                            if (downloadManager != null) {
                                                for (AlivcDownloadMediaInfo alivcDownloadMediaInfo : alivcDownloadMediaInfos) {
                                                    downloadManager.deleteFile(alivcDownloadMediaInfo.getAliyunDownloadMediaInfo());
                                                }

                                            }
                                            downloadDataProvider.deleteAllDownloadInfo(alivcDownloadMediaInfos);
                                        } else {
                                            FixedToastUtils.show(AliyunPlayerSkinActivity.this, "没有删除的视频选项...");
                                        }
                                    }
                                });
                        alivcDialog.setOnCancelOnclickListener(getResources().getString(R.string.alivc_dialog_cancle),
                                new onCancelOnclickListener() {
                                    @Override
                                    public void onCancel() {
                                        alivcDialog.dismiss();
                                    }
                                });
                        alivcDialog.show();
                    }
                });

                dialogDownloadView.setOnDownloadedItemClickListener(new DownloadView.OnDownloadItemClickListener() {
                    @Override
                    public void onDownloadedItemClick(final int positin) {
                        ArrayList<AlivcDownloadMediaInfo> allDownloadMediaInfo = dialogDownloadView.getAllDownloadMediaInfo();
                        List<AliyunDownloadMediaInfo> dataList = new ArrayList<>();
                        for (AlivcDownloadMediaInfo alivcDownloadMediaInfo : allDownloadMediaInfo) {
                            dataList.add(alivcDownloadMediaInfo.getAliyunDownloadMediaInfo());
                        }
//                List<AliyunDownloadMediaInfo> dataList = downloadDataProvider.getAllDownloadMediaInfo();
                        // 存入顺序和显示顺序相反,  所以进行倒序
                        ArrayList<AliyunDownloadMediaInfo> tempList = new ArrayList<>();
                        int size = dataList.size();
                        for (AliyunDownloadMediaInfo aliyunDownloadMediaInfo : dataList) {
                            if (aliyunDownloadMediaInfo.getProgress() == 100) {
                                tempList.add(aliyunDownloadMediaInfo);
                            }
                        }

                        Collections.reverse(tempList);
                        if ((dataList.size() - 1) < 0 || (dataList.size() - 1) > tempList.size()) {
                            return;
                        }
                        tempList.add(dataList.get(dataList.size() - 1));
                        for (int i = 0; i < size; i++) {
                            AliyunDownloadMediaInfo aliyunDownloadMediaInfo = dataList.get(i);
                            if (!tempList.contains(aliyunDownloadMediaInfo)) {
                                tempList.add(aliyunDownloadMediaInfo);
                            }
                        }

                        if (positin < 0) {
                            FixedToastUtils.show(AliyunPlayerSkinActivity.this, "视频资源不存在");
                            return;
                        }

                        // 如果点击列表中的视频, 需要将类型改为vid
                        AliyunDownloadMediaInfo aliyunDownloadMediaInfo = tempList.get(positin);
                        PlayParameter.PLAY_PARAM_TYPE = "localSource";
                        if (aliyunDownloadMediaInfo != null) {
                            PlayParameter.PLAY_PARAM_URL = aliyunDownloadMediaInfo.getSavePath();
                            mAliyunVodPlayerView.updateScreenShow();
                            changePlayLocalSource(PlayParameter.PLAY_PARAM_URL, aliyunDownloadMediaInfo.getTitle());
                        }
                    }

                    @Override
                    public void onDownloadingItemClick(ArrayList<AlivcDownloadMediaInfo> infos, int position) {
                        AlivcDownloadMediaInfo alivcInfo = infos.get(position);
                        AliyunDownloadMediaInfo aliyunDownloadInfo = alivcInfo.getAliyunDownloadMediaInfo();
                        AliyunDownloadMediaInfo.Status status = aliyunDownloadInfo.getStatus();
                        if (status == AliyunDownloadMediaInfo.Status.Error || status == AliyunDownloadMediaInfo.Status.Wait) {
                            //downloadManager.removeDownloadMedia(aliyunDownloadInfo);
                            downloadManager.startDownload(aliyunDownloadInfo);

                        }
                    }

                });
            }
        }
    }

    private Dialog downloadDialog = null;

    private AliyunDownloadMediaInfo aliyunDownloadMediaInfo;
    /**
     * 开始下载的事件监听
     */
    private AddDownloadView.OnViewClickListener viewClickListener = new AddDownloadView.OnViewClickListener() {
        @Override
        public void onCancel() {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
            }
        }

        @Override
        public void onDownload(AliyunDownloadMediaInfo info) {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
            }
            aliyunDownloadMediaInfo = info;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                int permission = ContextCompat.checkSelfPermission(AliyunPlayerSkinActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permission != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(AliyunPlayerSkinActivity.this, PERMISSIONS_STORAGE,
                            REQUEST_EXTERNAL_STORAGE);

                } else {
                    addNewInfo(info);
                }
            } else {
                addNewInfo(info);
            }

        }
    };

    private void addNewInfo(AliyunDownloadMediaInfo info) {
        if (downloadManager != null && info != null) {
            //todo
//            downloadManager.addDownloadMedia(info);
//            callDownloadPrepare(info.getVid(), info.getTitle());
            if (downloadView != null) {
                boolean hasAdd = downloadView.hasAdded(info);
                if (!hasAdd) {
                    if (downloadView != null && info != null) {
                        downloadView.addDownloadMediaInfo(info);
                    }
                    if (dialogDownloadView != null && info != null) {
                        dialogDownloadView.addDownloadMediaInfo(info);
                    }
                    downloadManager.startDownload(info);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addNewInfo(aliyunDownloadMediaInfo);
            } else {
                // Permission Denied
                FixedToastUtils.show(AliyunPlayerSkinActivity.this, "没有sd卡读写权限, 无法下载");
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 下载监听
     */
    private static class MyDownloadInfoListener implements AliyunDownloadInfoListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyDownloadInfoListener(AliyunPlayerSkinActivity aliyunPlayerSkinActivity) {
            weakReference = new WeakReference<>(aliyunPlayerSkinActivity);
        }

        @Override
        public void onPrepared(List<AliyunDownloadMediaInfo> infos) {
            preparedVid = infos.get(0).getVid();
            Collections.sort(infos, new Comparator<AliyunDownloadMediaInfo>() {
                @Override
                public int compare(AliyunDownloadMediaInfo mediaInfo1, AliyunDownloadMediaInfo mediaInfo2) {
                    if (mediaInfo1.getSize() > mediaInfo2.getSize()) {
                        return 1;
                    }
                    if (mediaInfo1.getSize() < mediaInfo2.getSize()) {
                        return -1;
                    }

                    if (mediaInfo1.getSize() == mediaInfo2.getSize()) {
                        return 0;
                    }
                    return 0;
                }
            });
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.mDownloadInPrepare = false;
                aliyunPlayerSkinActivity.onDownloadPrepared(infos, aliyunPlayerSkinActivity.showAddDownloadView);
            }
        }

        @Override
        public void onAdd(AliyunDownloadMediaInfo info) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                if (aliyunPlayerSkinActivity.downloadDataProvider != null) {
                    aliyunPlayerSkinActivity.downloadDataProvider.addDownloadMediaInfo(info);
                }
            }
        }

        @Override
        public void onStart(AliyunDownloadMediaInfo info) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                if (aliyunPlayerSkinActivity.dialogDownloadView != null) {
                    aliyunPlayerSkinActivity.dialogDownloadView.updateInfo(info);
                }
                if (aliyunPlayerSkinActivity.downloadView != null) {
                    aliyunPlayerSkinActivity.downloadView.updateInfo(info);
                }

            }
        }

        @Override
        public void onProgress(AliyunDownloadMediaInfo info, int percent) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                if (aliyunPlayerSkinActivity.dialogDownloadView != null) {
                    aliyunPlayerSkinActivity.dialogDownloadView.updateInfo(info);
                }
                if (aliyunPlayerSkinActivity.downloadView != null) {
                    aliyunPlayerSkinActivity.downloadView.updateInfo(info);
                }
            }
        }

        @Override
        public void onStop(AliyunDownloadMediaInfo info) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                if (aliyunPlayerSkinActivity.dialogDownloadView != null) {
                    aliyunPlayerSkinActivity.dialogDownloadView.updateInfo(info);
                }
                if (aliyunPlayerSkinActivity.downloadView != null) {
                    aliyunPlayerSkinActivity.downloadView.updateInfo(info);
                }
            }
        }

        @Override
        public void onCompletion(AliyunDownloadMediaInfo info) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                synchronized (aliyunPlayerSkinActivity) {
                    if (aliyunPlayerSkinActivity.downloadView != null) {
                        aliyunPlayerSkinActivity.downloadView.updateInfoByComplete(info);
                    }

                    if (aliyunPlayerSkinActivity.dialogDownloadView != null) {
                        aliyunPlayerSkinActivity.dialogDownloadView.updateInfoByComplete(info);
                    }

                    if (aliyunPlayerSkinActivity.downloadDataProvider != null) {
                        aliyunPlayerSkinActivity.downloadDataProvider.addDownloadMediaInfo(info);
                    }
                }
            }
        }

        @Override
        public void onError(AliyunDownloadMediaInfo info, ErrorCode code, String msg, String requestId) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.mDownloadInPrepare = false;
                if (aliyunPlayerSkinActivity.downloadView != null) {
                    aliyunPlayerSkinActivity.downloadView.updateInfoByError(info);
                }

                if (aliyunPlayerSkinActivity.dialogDownloadView != null) {
                    aliyunPlayerSkinActivity.dialogDownloadView.updateInfoByError(info);
                }

                //鉴权过期
                if (code.getValue() == ErrorCode.ERROR_SERVER_POP_UNKNOWN.getValue()) {
                    aliyunPlayerSkinActivity.refreshDownloadVidSts(info);
                }
                Message message = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putString(DOWNLOAD_ERROR_KEY, msg);
                message.setData(bundle);
                message.what = DOWNLOAD_ERROR;
                aliyunPlayerSkinActivity.playerHandler = new PlayerHandler(aliyunPlayerSkinActivity);
                aliyunPlayerSkinActivity.playerHandler.sendMessage(message);
            }
        }

        @Override
        public void onWait(AliyunDownloadMediaInfo info) {
//            mPlayerDownloadAdapter.updateData(info);
        }

        @Override
        public void onDelete(AliyunDownloadMediaInfo info) {
//            mPlayerDownloadAdapter.deleteData(info);
        }

        @Override
        public void onDeleteAll() {
//            mPlayerDownloadAdapter.clearAll();
        }

        @Override
        public void onFileProgress(AliyunDownloadMediaInfo info) {

        }
    }

    List<AliyunDownloadMediaInfo> aliyunDownloadMediaInfoList = new ArrayList<>();
    private List<AliyunDownloadMediaInfo> currentPreparedMediaInfo = null;

    private void onDownloadPrepared(List<AliyunDownloadMediaInfo> infos, boolean showAddDownloadView) {
        currentPreparedMediaInfo = new ArrayList<>();
        currentPreparedMediaInfo.addAll(infos);
        if (showAddDownloadView) {
            showAddDownloadView(mCurrentDownloadScreenMode);
        }

    }

    private static class MyChangeQualityListener implements OnChangeQualityListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyChangeQualityListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onChangeQualitySuccess(String finalQuality) {

            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualitySuccess(finalQuality);
            }
        }

        @Override
        public void onChangeQualityFail(int code, String msg) {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onChangeQualityFail(code, msg);
            }
        }
    }

    private void onChangeQualitySuccess(String finalQuality) {
        logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_success));
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(),
                getString(R.string.log_change_quality_success));
    }

    void onChangeQualityFail(int code, String msg) {
        logStrs.add(format.format(new Date()) + getString(R.string.log_change_quality_fail) + " : " + msg);
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(),
                getString(R.string.log_change_quality_fail));
    }

    private static class MyStoppedListener implements OnStoppedListener {

        private WeakReference<AliyunPlayerSkinActivity> activityWeakReference;

        public MyStoppedListener(AliyunPlayerSkinActivity skinActivity) {
            activityWeakReference = new WeakReference<AliyunPlayerSkinActivity>(skinActivity);
        }

        @Override
        public void onStop() {
            AliyunPlayerSkinActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onStopped();
            }
        }
    }

    private static class MyRefreshStsCallback implements RefreshStsCallback {

        @Override
        public VidSts refreshSts(String vid, String quality, String format, String title, boolean encript) {
            VcPlayerLog.d("refreshSts ", "refreshSts , vid = " + vid);
            //NOTE: 注意：这个不能启动线程去请求。因为这个方法已经在线程中调用了。
            VidSts vidSts = VidStsUtil.getVidSts(vid);
            if (vidSts == null) {
                return null;
            } else {
                vidSts.setVid(vid);
                vidSts.setQuality(quality, true);
                vidSts.setTitle(title);
                return vidSts;
            }
        }
    }

    private void onStopped() {
        FixedToastUtils.show(AliyunPlayerSkinActivity.this.getApplicationContext(), R.string.log_play_stopped);
    }

    private void setPlaySource() {
        if ("localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            UrlSource urlSource = new UrlSource();
            urlSource.setUri(PlayParameter.PLAY_PARAM_URL);
            if (mAliyunVodPlayerView != null) {
                mAliyunVodPlayerView.setLocalSource(urlSource);
            }

        } else if ("vidsts".equals(PlayParameter.PLAY_PARAM_TYPE)) {
            if (!inRequest) {
                VidSts vidSts = new VidSts();
                vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
                vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
                vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
                vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
                vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView.setVidSts(vidSts);
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mIsInBackground = false;
        updatePlayerViewMode();
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(true);
            mAliyunVodPlayerView.onResume();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsInBackground = true;
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(false);
            mAliyunVodPlayerView.onStop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePlayerViewMode();
    }

    private void updateDownloadTaskTip() {
        if (currentTab != TAB_DOWNLOAD_LIST) {

            Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.alivc_download_new_task);
            drawable.setBounds(0, 0, 20, 20);
            tvTabDownloadVideo.setCompoundDrawablePadding(-20);
            tvTabDownloadVideo.setCompoundDrawables(null, null, drawable, null);
        } else {
            tvTabDownloadVideo.setCompoundDrawables(null, null, null, null);
        }
    }

    private void updatePlayerViewMode() {
        if (mAliyunVodPlayerView != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //转为竖屏了。
                //显示状态栏
                //                if (!isStrangePhone()) {
                //                    getSupportActionBar().show();
                //                }

                this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                //设置view的布局，宽高之类
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams) mAliyunVodPlayerView
                        .getLayoutParams();
                aliVcVideoViewLayoutParams.height = (int) (ScreenUtils.getWidth(this) * 9.0f / 16);
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //转到横屏了。
                //隐藏状态栏
                if (!isStrangePhone()) {
                    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    mAliyunVodPlayerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
                //设置view的布局，宽高
                LinearLayout.LayoutParams aliVcVideoViewLayoutParams = (LinearLayout.LayoutParams) mAliyunVodPlayerView
                        .getLayoutParams();
                aliVcVideoViewLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                aliVcVideoViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.onDestroy();
            mAliyunVodPlayerView = null;
        }

        if (playerHandler != null) {
            playerHandler.removeMessages(DOWNLOAD_ERROR);
            playerHandler = null;
        }

        if (commenUtils != null) {
            commenUtils.onDestroy();
            commenUtils = null;
        }
        super.onDestroy();

        if (downloadManager != null && downloadDataProvider != null) {
            ConcurrentLinkedQueue<AliyunDownloadMediaInfo> downloadMediaInfos = new ConcurrentLinkedQueue<>();
            downloadMediaInfos.addAll(downloadDataProvider.getAllDownloadMediaInfo());
            downloadManager.stopDownloads(downloadMediaInfos);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAliyunVodPlayerView != null) {
            boolean handler = mAliyunVodPlayerView.onKeyDown(keyCode, event);
            if (!handler) {
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //解决某些手机上锁屏之后会出现标题栏的问题。
        updatePlayerViewMode();
    }

    private static final int DOWNLOAD_ERROR = 1;
    private static final String DOWNLOAD_ERROR_KEY = "error_key";

    private static class PlayerHandler extends Handler {
        //持有弱引用AliyunPlayerSkinActivity,GC回收时会被回收掉.
        private final WeakReference<AliyunPlayerSkinActivity> mActivty;

        public PlayerHandler(AliyunPlayerSkinActivity activity) {
            mActivty = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AliyunPlayerSkinActivity activity = mActivty.get();
            super.handleMessage(msg);
            if (activity != null) {
                switch (msg.what) {
                    case DOWNLOAD_ERROR:
                        Log.d("donwload", msg.getData().getString(DOWNLOAD_ERROR_KEY));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static class MyStsListener implements VidStsUtil.OnStsResultListener {

        private WeakReference<AliyunPlayerSkinActivity> weakActivity;

        MyStsListener(AliyunPlayerSkinActivity act) {
            weakActivity = new WeakReference<>(act);
        }

        @Override
        public void onSuccess(String vid, final String akid, final String akSecret, final String token) {
            AliyunPlayerSkinActivity activity = weakActivity.get();
            if (activity != null) {
                activity.onStsSuccess(vid, akid, akSecret, token);
            }
        }

        @Override
        public void onFail() {
            AliyunPlayerSkinActivity activity = weakActivity.get();
            if (activity != null) {
                activity.onStsFail();
            }
        }
    }

    private void onStsFail() {

        FixedToastUtils.show(getApplicationContext(), R.string.request_vidsts_fail);
        inRequest = false;
        //finish();
    }

    private void onStsSuccess(String mVid, String akid, String akSecret, String token) {
        PlayParameter.PLAY_PARAM_VID = mVid;
        PlayParameter.PLAY_PARAM_AK_ID = akid;
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token;

        mIsTimeExpired = false;

        inRequest = false;

        // 视频列表数据为0时, 加载列表
        if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
            alivcVideoInfos.clear();
            loadPlayList();
        }
    }

    private static class MyOrientationChangeListener implements AliyunVodPlayerView.OnOrientationChangeListener {

        private final WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOrientationChangeListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void orientationChange(boolean from, AliyunScreenMode currentMode) {
            AliyunPlayerSkinActivity activity = weakReference.get();

            if (activity != null) {
                activity.hideDownloadDialog(from, currentMode);
                activity.hideShowMoreDialog(from, currentMode);

            }
        }
    }

    private void hideShowMoreDialog(boolean from, AliyunScreenMode currentMode) {
        if (showMoreDialog != null) {
            if (currentMode == AliyunScreenMode.Small) {
                showMoreDialog.dismiss();
                currentScreenMode = currentMode;
            }
        }
    }

    private void hideDownloadDialog(boolean from, AliyunScreenMode currentMode) {

        if (downloadDialog != null) {
            if (currentScreenMode != currentMode) {
                downloadDialog.dismiss();
                currentScreenMode = currentMode;
            }
        }
    }

    /**
     * 判断是否有网络的监听
     */
    private class MyNetConnectedListener implements AliyunVodPlayerView.NetConnectedListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyNetConnectedListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onReNetConnected(boolean isReconnect) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onReNetConnected(isReconnect);
            }
        }

        @Override
        public void onNetUnConnected() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onNetUnConnected();
            }
        }
    }

    private void onNetUnConnected() {
        currentError = ErrorInfo.UnConnectInternet;
        if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList.size() > 0) {
            ConcurrentLinkedQueue<AliyunDownloadMediaInfo> allDownloadMediaInfo = new ConcurrentLinkedQueue<>();
            List<AliyunDownloadMediaInfo> mediaInfos = downloadDataProvider.getAllDownloadMediaInfo();
            allDownloadMediaInfo.addAll(mediaInfos);
            downloadManager.stopDownloads(allDownloadMediaInfo);
        }
    }

    private void onReNetConnected(boolean isReconnect) {
        currentError = ErrorInfo.Normal;
        if (isReconnect) {
            if (aliyunDownloadMediaInfoList != null && aliyunDownloadMediaInfoList.size() > 0) {
                int unCompleteDownload = 0;
                for (AliyunDownloadMediaInfo info : aliyunDownloadMediaInfoList) {
                    if (info.getStatus() == AliyunDownloadMediaInfo.Status.Stop) {
                        unCompleteDownload++;
                    }
                }

                if (unCompleteDownload > 0) {
                    FixedToastUtils.show(this, "网络恢复, 请手动开启下载任务...");
                }
            }
            // 如果当前播放列表为空, 网络重连后需要重新请求sts和播放列表, 其他情况不需要
            if (alivcVideoInfos != null && alivcVideoInfos.size() == 0) {
                VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new MyStsListener(this));
            }
        }
    }

    /**
     * 因为鉴权过期,而去重新鉴权
     */
    private static class RetryExpiredSts implements VidStsUtil.OnStsResultListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public RetryExpiredSts(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(String vid, String akid, String akSecret, String token) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onStsRetrySuccess(vid, akid, akSecret, token);
            }
        }

        @Override
        public void onFail() {

        }
    }

    private void onStsRetrySuccess(String mVid, String akid, String akSecret, String token) {
        PlayParameter.PLAY_PARAM_VID = mVid;
        PlayParameter.PLAY_PARAM_AK_ID = akid;
        PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
        PlayParameter.PLAY_PARAM_SCU_TOKEN = token;

        inRequest = false;
        mIsTimeExpired = false;

        VidSts vidSts = new VidSts();
        vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
        vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
        vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
        vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);

        mAliyunVodPlayerView.setVidSts(vidSts);
    }

    //    private static class MyOnUrlTimeExpiredListener implements IAliyunVodPlayer.OnUrlTimeExpiredListener {
//        WeakReference<AliyunPlayerSkinActivity> weakReference;
//
//        public MyOnUrlTimeExpiredListener(AliyunPlayerSkinActivity activity) {
//            weakReference = new WeakReference<AliyunPlayerSkinActivity>(activity);
//        }
//
//        @Override
//        public void onUrlTimeExpired(String s, String s1) {
//            AliyunPlayerSkinActivity activity = weakReference.get();
//            if (activity != null) {
//                activity.onUrlTimeExpired(s, s1);
//            }
//        }
//    }
//
    public static class MyOnTimeExpiredErrorListener implements AliyunVodPlayerView.OnTimeExpiredErrorListener {

        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnTimeExpiredErrorListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onTimeExpiredError() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onTimExpiredError();
            }
        }
    }

    private void onUrlTimeExpired(String oldVid, String oldQuality) {
        //requestVidSts();
        VidSts vidSts = VidStsUtil.getVidSts(oldVid);
        PlayParameter.PLAY_PARAM_VID = vidSts.getVid();
        PlayParameter.PLAY_PARAM_AK_SECRE = vidSts.getAccessKeySecret();
        PlayParameter.PLAY_PARAM_AK_ID = vidSts.getAccessKeyId();
        PlayParameter.PLAY_PARAM_SCU_TOKEN = vidSts.getSecurityToken();

        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setVidSts(vidSts);
        }
    }

    /**
     * 鉴权过期
     */
    private void onTimExpiredError() {
        VidStsUtil.getVidSts(PlayParameter.PLAY_PARAM_VID, new RetryExpiredSts(this));
    }

    private static class MyShowMoreClickLisener implements ControlView.OnShowMoreClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyShowMoreClickLisener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void showMore() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                long currentClickTime = System.currentTimeMillis();
                // 防止快速点击
                if (currentClickTime - activity.oldTime <= 1000) {
                    return;
                }
                activity.oldTime = currentClickTime;
                activity.showMore(activity);
                activity.requestVidSts();
            }

        }
    }

    private void showMore(final AliyunPlayerSkinActivity activity) {
        showMoreDialog = new AlivcShowMoreDialog(activity);
        AliyunShowMoreValue moreValue = new AliyunShowMoreValue();
        moreValue.setSpeed(mAliyunVodPlayerView.getCurrentSpeed());
        moreValue.setVolume((int) mAliyunVodPlayerView.getCurrentVolume());

        ShowMoreView showMoreView = new ShowMoreView(activity, moreValue);
        showMoreDialog.setContentView(showMoreView);
        showMoreDialog.show();
        showMoreView.setOnDownloadButtonClickListener(new ShowMoreView.OnDownloadButtonClickListener() {
            @Override
            public void onDownloadClick() {
                long currentClickTime = System.currentTimeMillis();
                // 防止快速点击
                if (currentClickTime - downloadOldTime <= 1000) {
                    return;
                }
                downloadOldTime = currentClickTime;
                // 点击下载
                showMoreDialog.dismiss();
                if ("url".equals(PlayParameter.PLAY_PARAM_TYPE) || "localSource".equals(PlayParameter.PLAY_PARAM_TYPE)) {
                    FixedToastUtils.show(activity, getResources().getString(R.string.alivc_video_not_support_download));
                    return;
                }
                mCurrentDownloadScreenMode = AliyunScreenMode.Full;
                showAddDownloadView = true;
                if (mAliyunVodPlayerView != null) {
                    MediaInfo currentMediaInfo = mAliyunVodPlayerView.getCurrentMediaInfo();
                    if (currentMediaInfo != null && currentMediaInfo.getVideoId().equals(PlayParameter.PLAY_PARAM_VID)) {
                        VidSts vidSts = new VidSts();
                        vidSts.setVid(PlayParameter.PLAY_PARAM_VID);
                        vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
                        vidSts.setAccessKeyId(PlayParameter.PLAY_PARAM_AK_ID);
                        vidSts.setAccessKeySecret(PlayParameter.PLAY_PARAM_AK_SECRE);
                        vidSts.setSecurityToken(PlayParameter.PLAY_PARAM_SCU_TOKEN);
                        downloadManager.prepareDownload(vidSts);
                    }
                }
            }
        });

        showMoreView.setOnScreenCastButtonClickListener(new ShowMoreView.OnScreenCastButtonClickListener() {
            @Override
            public void onScreenCastClick() {
                Toast.makeText(activity, "功能正在开发中......", Toast.LENGTH_SHORT).show();
//                TODO 2019年04月18日16:43:29  先屏蔽投屏功能
//                showMoreDialog.dismiss();
//                showScreenCastView();
            }
        });

        showMoreView.setOnBarrageButtonClickListener(new ShowMoreView.OnBarrageButtonClickListener() {
            @Override
            public void onBarrageClick() {
                Toast.makeText(activity, "功能正在开发中......", Toast.LENGTH_SHORT).show();
//                if (showMoreDialog != null && showMoreDialog.isShowing()) {
//                    showMoreDialog.dismiss();
//                }
            }
        });

        showMoreView.setOnSpeedCheckedChangedListener(new ShowMoreView.OnSpeedCheckedChangedListener() {
            @Override
            public void onSpeedChanged(RadioGroup group, int checkedId) {
                // 点击速度切换
                if (checkedId == R.id.rb_speed_normal) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.One);
                } else if (checkedId == R.id.rb_speed_onequartern) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneQuartern);
                } else if (checkedId == R.id.rb_speed_onehalf) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.OneHalf);
                } else if (checkedId == R.id.rb_speed_twice) {
                    mAliyunVodPlayerView.changeSpeed(SpeedValue.Twice);
                }

            }
        });

        /**
         * 初始化亮度
         */
        if (mAliyunVodPlayerView != null) {
            showMoreView.setBrightness(mAliyunVodPlayerView.getScreenBrightness());
        }
        // 亮度seek
        showMoreView.setOnLightSeekChangeListener(new ShowMoreView.OnLightSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                setWindowBrightness(progress);
                if (mAliyunVodPlayerView != null) {
                    mAliyunVodPlayerView.setScreenBrightness(progress);
                }
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

        /**
         * 初始化音量
         */
        if (mAliyunVodPlayerView != null) {
            showMoreView.setVoiceVolume(mAliyunVodPlayerView.getCurrentVolume());
        }
        showMoreView.setOnVoiceSeekChangeListener(new ShowMoreView.OnVoiceSeekChangeListener() {
            @Override
            public void onStart(SeekBar seekBar) {

            }

            @Override
            public void onProgress(SeekBar seekBar, int progress, boolean fromUser) {
                mAliyunVodPlayerView.setCurrentVolume(progress / 100.00f);
            }

            @Override
            public void onStop(SeekBar seekBar) {

            }
        });

    }

    /**
     * 获取url的scheme
     *
     * @param url
     * @return
     */
    private String getUrlScheme(String url) {
        return Uri.parse(url).getScheme();
    }

    private static class MyPlayStateBtnClickListener implements AliyunVodPlayerView.OnPlayStateBtnClickListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MyPlayStateBtnClickListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onPlayBtnClick(int playerState) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onPlayStateSwitch(playerState);
            }
        }
    }

    /**
     * 播放状态切换
     */
    private void onPlayStateSwitch(int playerState) {
        if (playerState == IPlayer.started) {
            tvLogs.append(format.format(new Date()) + " 暂停 \n");
        } else if (playerState == IPlayer.paused) {
            tvLogs.append(format.format(new Date()) + " 开始 \n");
        }

    }

    private static class MySeekCompleteListener implements IPlayer.OnSeekCompleteListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekCompleteListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekComplete() {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekComplete();
            }
        }
    }

    private void onSeekComplete() {
        tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_completed) + "\n");
    }

    private static class MySeekStartListener implements AliyunVodPlayerView.OnSeekStartListener {
        WeakReference<AliyunPlayerSkinActivity> weakReference;

        MySeekStartListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onSeekStart(int position) {
            AliyunPlayerSkinActivity activity = weakReference.get();
            if (activity != null) {
                activity.onSeekStart(position);
            }
        }
    }

    private static class MyOnFinishListener implements AliyunVodPlayerView.OnFinishListener {

        WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnFinishListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onFinishClick() {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.finish();
            }
        }
    }

    private static class MyOnScreenBrightnessListener implements AliyunVodPlayerView.OnScreenBrightnessListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnScreenBrightnessListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onScreenBrightness(int brightness) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.setWindowBrightness(brightness);
                if (aliyunPlayerSkinActivity.mAliyunVodPlayerView != null) {
                    aliyunPlayerSkinActivity.mAliyunVodPlayerView.setScreenBrightness(brightness);
                }
            }
        }
    }

    /**
     * 播放器出错监听
     */
    private static class MyOnErrorListener implements IPlayer.OnErrorListener {

        private WeakReference<AliyunPlayerSkinActivity> weakReference;

        public MyOnErrorListener(AliyunPlayerSkinActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onError(com.aliyun.player.bean.ErrorInfo errorInfo) {
            AliyunPlayerSkinActivity aliyunPlayerSkinActivity = weakReference.get();
            if (aliyunPlayerSkinActivity != null) {
                aliyunPlayerSkinActivity.onError(errorInfo);
            }
        }
    }

    private void onError(com.aliyun.player.bean.ErrorInfo errorInfo) {
        //鉴权过期
        if (errorInfo.getCode().getValue() == ErrorCode.ERROR_SERVER_POP_UNKNOWN.getValue()) {
            mIsTimeExpired = true;
        }
    }

    private void onSeekStart(int position) {
        tvLogs.append(format.format(new Date()) + getString(R.string.log_seek_start) + "\n");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getKeyCode() == 67) {
            if (mAliyunVodPlayerView != null) {
                //删除按键监听,部分手机在EditText没有内容时,点击删除按钮会隐藏软键盘
                return false;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 刷新下载的VidSts
     */
    private void refreshDownloadVidSts(final AliyunDownloadMediaInfo downloadMediaInfo) {
        VidStsUtil.getVidSts(downloadMediaInfo.getVidSts().getVid(), new VidStsUtil.OnStsResultListener() {
            @Override
            public void onSuccess(String vid, String akid, String akSecret, String token) {
                if (downloadManager != null) {
                    VidSts vidSts = new VidSts();
                    vidSts.setVid(vid);
                    vidSts.setRegion(PlayParameter.PLAY_PARAM_REGION);
                    vidSts.setAccessKeyId(akid);
                    vidSts.setAccessKeySecret(akSecret);
                    vidSts.setSecurityToken(token);
                    downloadMediaInfo.setVidSts(vidSts);
                    PlayParameter.PLAY_PARAM_AK_ID = akid;
                    PlayParameter.PLAY_PARAM_AK_SECRE = akSecret;
                    PlayParameter.PLAY_PARAM_SCU_TOKEN = token;
                    downloadManager.prepareDownloadByQuality(downloadMediaInfo, AliyunDownloadManager.INTENT_STATE_START);
                }
            }

            @Override
            public void onFail() {

            }
        });

    }
}
