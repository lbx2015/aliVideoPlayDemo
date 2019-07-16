package com.aliyun.vodplayerview.utils.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.aliyun.vodplayerview.utils.download.AliyunDownloadMediaInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库管理类
 *
 * @author hanyu
 */
public class DatabaseManager {

    /**
     * 数据库名称
     */
    public static final String DB_NAME = "Player_Download";

    /**
     * 表名
     */
    private static final String TABLE_NAME = "player_download_info";

    /**
     * 准备状态
     */
    private static final int PREPARED_STATE = 1;

    /**
     * 下载中状态
     */
    private static final int DOWNLOADING_STATE = 3;

    /**
     * 停止状态
     */
    private static final int STOP_STATE = 4;

    /**
     * 完成状态
     */
    private static final int COMPLETED_STATE = 5;

    /**
     * 建表语句
     */
    public static final String CREATE_TABLE_SQL = "create table if not exists " + DatabaseManager.TABLE_NAME +
            " (" + DatabaseManager.ID + " integer primary key autoincrement," +
            DatabaseManager.VID + " text," + DatabaseManager.QUALITY + " text," +
            DatabaseManager.TITLE + " text," + DatabaseManager.COVERURL + " text," +
            DatabaseManager.DURATION + " text," + DatabaseManager.SIZE + " text," +
            DatabaseManager.PROGRESS + " integer," + DatabaseManager.STATUS + " integer," +
            DatabaseManager.PATH + " text," + DatabaseManager.TRACKINDEX + " integer," +
            DatabaseManager.FORMAT + " text)";

    /**
     * 查询所有语句
     */
    private static final String SELECT_ALL_SQL = "select * from " + DatabaseManager.TABLE_NAME;

    /**
     * 根据状态查询数据
     */
    private static final String SELECT_WITH_STATUS_SQL = "select * from " + DatabaseManager.TABLE_NAME + " where status=?";

    private static final String ID = "id";
    private static final String VID = "vid";
    private static final String QUALITY = "quality";
    private static final String TITLE = "title";
    private static final String COVERURL = "coverurl";
    private static final String DURATION = "duration";
    private static final String SIZE = "size";
    private static final String PROGRESS = "progress";
    private static final String STATUS = "status";
    private static final String PATH = "path";
    private static final String FORMAT = "format";
    private static final String TRACKINDEX = "trackindex";

    private static DatabaseManager mInstance = null;

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase mSqliteDatabase;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        if (mInstance == null) {
            synchronized (DatabaseManager.class) {
                if (mInstance == null) {
                    mInstance = new DatabaseManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 创建数据库
     */
    public void createDataBase(Context context) {
        databaseHelper = DatabaseHelper.getInstance(context);
        if (mSqliteDatabase == null) {
            synchronized (DatabaseManager.class) {
                if (mSqliteDatabase == null) {
                    mSqliteDatabase = databaseHelper.getWritableDatabase();
                }
            }
        }
    }

    /**
     * 查询某条数据是否已经插入到数据库中
     */
    public int selectItemExist(AliyunDownloadMediaInfo mediaInfo) {
        Cursor cursor = mSqliteDatabase.query(TABLE_NAME, new String[]{"id"}, "vid=? and quality=?",
                new String[]{mediaInfo.getVid(), mediaInfo.getQuality()}, null, null, null);

        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public long insert(AliyunDownloadMediaInfo mediaInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(VID, mediaInfo.getVid());
        contentValues.put(QUALITY, mediaInfo.getQuality());
        contentValues.put(TITLE, mediaInfo.getTitle());
        contentValues.put(FORMAT,mediaInfo.getFormat());
        contentValues.put(COVERURL, mediaInfo.getCoverUrl());
        contentValues.put(DURATION, mediaInfo.getDuration());
        contentValues.put(SIZE, mediaInfo.getSize());
        contentValues.put(PROGRESS, mediaInfo.getProgress());
        contentValues.put(STATUS, mediaInfo.getStatus().ordinal());
        contentValues.put(PATH,mediaInfo.getSavePath());
        contentValues.put(TRACKINDEX,mediaInfo.getQualityIndex());
        return mSqliteDatabase.insert(TABLE_NAME, null, contentValues);
    }

    public int delete(AliyunDownloadMediaInfo mediaInfo) {
        if(mSqliteDatabase == null || !mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        return mSqliteDatabase.delete(TABLE_NAME, "vid=? and quality=?",
                new String[]{mediaInfo.getVid(), mediaInfo.getQuality()});
    }

    public int update(AliyunDownloadMediaInfo mediaInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PROGRESS, mediaInfo.getProgress());
        contentValues.put(STATUS,mediaInfo.getStatus().ordinal());
        contentValues.put(PATH,mediaInfo.getSavePath());
        contentValues.put(TRACKINDEX,mediaInfo.getQualityIndex());
        return mSqliteDatabase.update(TABLE_NAME, contentValues, " vid=? and quality=?",
                new String[]{mediaInfo.getVid(), mediaInfo.getQuality()});
    }

    /**
     * 删除所有数据
     */
    public void deleteAll(){
        if(!mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        mSqliteDatabase.delete(TABLE_NAME,"",new String[]{});
    }

    /**
     * 删除指定的数据
     */
    public void deleteItem(AliyunDownloadMediaInfo mediaInfo){
        if(!mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        mSqliteDatabase.delete(TABLE_NAME,"vid=?,quality=?",new String[]{mediaInfo.getVid(),mediaInfo.getQuality()});
    }

    /**
     * 查询所有下载中状态的数据
     */
    public List<AliyunDownloadMediaInfo> selectDownloadingList(){
        if(!mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        Cursor cursor = mSqliteDatabase.rawQuery(SELECT_WITH_STATUS_SQL,new String[]{DOWNLOADING_STATE+""});
        List<AliyunDownloadMediaInfo> queryLists = new ArrayList<>();
        if(cursor == null || cursor.getCount() <= 0){
            if(cursor != null){
                cursor.close();
            }
            return queryLists;
        }
        while(cursor.moveToNext()){
            AliyunDownloadMediaInfo mediaInfo = new AliyunDownloadMediaInfo();
            mediaInfo.setVid(cursor.getString(cursor.getColumnIndex(VID)));
            mediaInfo.setQuality(cursor.getString(cursor.getColumnIndex(QUALITY)));
            mediaInfo.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            mediaInfo.setCoverUrl(cursor.getString(cursor.getColumnIndex(COVERURL)));
            String duration = cursor.getString(cursor.getColumnIndex(DURATION));
            mediaInfo.setDuration(Long.valueOf(duration));
            String size = cursor.getString(cursor.getColumnIndex(SIZE));
            mediaInfo.setSize(Long.valueOf(size));
            mediaInfo.setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
            mediaInfo.setSavePath(cursor.getString(cursor.getColumnIndex(PATH)));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            mediaInfo.setFormat(cursor.getString(cursor.getColumnIndex(FORMAT)));
            mediaInfo.setQualityIndex(cursor.getInt(cursor.getColumnIndex(TRACKINDEX)));
            switch (status) {
                case 0:
                    //Idle
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
                case 1:
                    //prepare
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Prepare);
                    break;
                case 2:
                    //wait
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Wait);
                    break;
                case 3:
                    //start
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Start);
                    break;
                case 4:
                    //stop
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Stop);
                    break;
                case 5:
                    //complete
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Complete);
                    break;
                case 6:
                    //error
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Error);
                    break;
                default:
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
            }
            queryLists.add(mediaInfo);

        }
        cursor.close();
        return queryLists;
    }

    /**
     * 查询处于等待状态的数据
     */
    public List<AliyunDownloadMediaInfo> selectStopedList(){
        if(!mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        Cursor cursor = mSqliteDatabase.rawQuery(SELECT_WITH_STATUS_SQL,new String[]{STOP_STATE+""});
        List<AliyunDownloadMediaInfo> queryLists = new ArrayList<>();
        if(cursor == null || cursor.getCount() <= 0){
            if(cursor != null){
                cursor.close();
            }

            return queryLists;
        }
        while(cursor.moveToNext()){
            AliyunDownloadMediaInfo mediaInfo = new AliyunDownloadMediaInfo();
            mediaInfo.setVid(cursor.getString(cursor.getColumnIndex(VID)));
            mediaInfo.setQuality(cursor.getString(cursor.getColumnIndex(QUALITY)));
            mediaInfo.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            mediaInfo.setCoverUrl(cursor.getString(cursor.getColumnIndex(COVERURL)));
            String duration = cursor.getString(cursor.getColumnIndex(DURATION));
            mediaInfo.setDuration(Long.valueOf(duration));
            String size = cursor.getString(cursor.getColumnIndex(SIZE));
            mediaInfo.setSize(Long.valueOf(size));
            mediaInfo.setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
            mediaInfo.setSavePath(cursor.getString(cursor.getColumnIndex(PATH)));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            mediaInfo.setFormat(cursor.getString(cursor.getColumnIndex(FORMAT)));
            mediaInfo.setQualityIndex(cursor.getInt(cursor.getColumnIndex(TRACKINDEX)));
            switch (status) {
                case 0:
                    //Idle
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
                case 1:
                    //prepare
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Prepare);
                    break;
                case 2:
                    //wait
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Wait);
                    break;
                case 3:
                    //start
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Start);
                    break;
                case 4:
                    //stop
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Stop);
                    break;
                case 5:
                    //complete
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Complete);
                    break;
                case 6:
                    //error
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Error);
                    break;
                default:
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
            }
            queryLists.add(mediaInfo);

        }
        cursor.close();
        return queryLists;
    }

    /**
     * 查询所有完成状态的数据
     */
    public List<AliyunDownloadMediaInfo> selectCompletedList(){
        Cursor cursor = mSqliteDatabase.rawQuery(SELECT_WITH_STATUS_SQL,new String[]{COMPLETED_STATE+""});
        List<AliyunDownloadMediaInfo> queryLists = new ArrayList<>();
        if(cursor == null || cursor.getCount() <= 0){
            if(cursor != null){
                cursor.close();
            }
            return queryLists;
        }
        while(cursor.moveToNext()){
            AliyunDownloadMediaInfo mediaInfo = new AliyunDownloadMediaInfo();
            mediaInfo.setVid(cursor.getString(cursor.getColumnIndex(VID)));
            mediaInfo.setQuality(cursor.getString(cursor.getColumnIndex(QUALITY)));
            mediaInfo.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            mediaInfo.setCoverUrl(cursor.getString(cursor.getColumnIndex(COVERURL)));
            String duration = cursor.getString(cursor.getColumnIndex(DURATION));
            mediaInfo.setDuration(Long.valueOf(duration));
            String size = cursor.getString(cursor.getColumnIndex(SIZE));
            mediaInfo.setSize(Long.valueOf(size));
            mediaInfo.setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
            mediaInfo.setSavePath(cursor.getString(cursor.getColumnIndex(PATH)));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            mediaInfo.setFormat(cursor.getString(cursor.getColumnIndex(FORMAT)));
            mediaInfo.setQualityIndex(cursor.getInt(cursor.getColumnIndex(TRACKINDEX)));
            switch (status) {
                case 0:
                    //Idle
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
                case 1:
                    //prepare
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Prepare);
                    break;
                case 2:
                    //wait
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Wait);
                    break;
                case 3:
                    //start
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Start);
                    break;
                case 4:
                    //stop
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Stop);
                    break;
                case 5:
                    //complete
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Complete);
                    break;
                case 6:
                    //error
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Error);
                    break;
                default:
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
            }
            queryLists.add(mediaInfo);

        }
        cursor.close();
        return queryLists;
    }

    /**
     * 查询所有准备状态的数据
     */
    public List<AliyunDownloadMediaInfo> selectPreparedList(){
        if(!mSqliteDatabase.isOpen()){
            mSqliteDatabase = databaseHelper.getWritableDatabase();
        }
        Cursor cursor = mSqliteDatabase.rawQuery(SELECT_WITH_STATUS_SQL,new String[]{PREPARED_STATE+""});
        List<AliyunDownloadMediaInfo> queryLists = new ArrayList<>();
        if(cursor == null || cursor.getCount() <= 0){
            if(cursor != null){
                cursor.close();
            }
            return queryLists;
        }
        while(cursor.moveToNext()){
            AliyunDownloadMediaInfo mediaInfo = new AliyunDownloadMediaInfo();
            mediaInfo.setVid(cursor.getString(cursor.getColumnIndex(VID)));
            mediaInfo.setQuality(cursor.getString(cursor.getColumnIndex(QUALITY)));
            mediaInfo.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            mediaInfo.setCoverUrl(cursor.getString(cursor.getColumnIndex(COVERURL)));
            String duration = cursor.getString(cursor.getColumnIndex(DURATION));
            mediaInfo.setDuration(Long.valueOf(duration));
            String size = cursor.getString(cursor.getColumnIndex(SIZE));
            mediaInfo.setSize(Long.valueOf(size));
            mediaInfo.setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
            mediaInfo.setSavePath(cursor.getString(cursor.getColumnIndex(PATH)));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            mediaInfo.setFormat(cursor.getString(cursor.getColumnIndex(FORMAT)));
            switch (status) {
                case 0:
                    //Idle
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
                case 1:
                    //prepare
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Prepare);
                    break;
                case 2:
                    //wait
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Wait);
                    break;
                case 3:
                    //start
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Start);
                    break;
                case 4:
                    //stop
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Stop);
                    break;
                case 5:
                    //complete
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Complete);
                    break;
                case 6:
                    //error
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Error);
                    break;
                default:
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
            }
            queryLists.add(mediaInfo);

        }
        cursor.close();
        return queryLists;
    }

    /**
     * 查询所有
     */
    public List<AliyunDownloadMediaInfo> selectAll() {
        Cursor cursor = mSqliteDatabase.rawQuery(SELECT_ALL_SQL, new String[]{});
        List<AliyunDownloadMediaInfo> queryLists = new ArrayList<>();
        if (cursor == null || cursor.getCount() <= 0) {
            if(cursor != null){
                cursor.close();
            }
            return queryLists;
        }
        while (cursor.moveToNext()) {
            AliyunDownloadMediaInfo mediaInfo = new AliyunDownloadMediaInfo();
            mediaInfo.setVid(cursor.getString(cursor.getColumnIndex(VID)));
            mediaInfo.setQuality(cursor.getString(cursor.getColumnIndex(QUALITY)));
            mediaInfo.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            mediaInfo.setCoverUrl(cursor.getString(cursor.getColumnIndex(COVERURL)));
            String duration = cursor.getString(cursor.getColumnIndex(DURATION));
            mediaInfo.setDuration(Long.valueOf(duration));
            String size = cursor.getString(cursor.getColumnIndex(SIZE));
            mediaInfo.setSize(Long.valueOf(size));
            mediaInfo.setProgress(cursor.getInt(cursor.getColumnIndex(PROGRESS)));
            mediaInfo.setSavePath(cursor.getString(cursor.getColumnIndex(PATH)));
            int status = cursor.getInt(cursor.getColumnIndex(STATUS));
            mediaInfo.setFormat(cursor.getString(cursor.getColumnIndex(FORMAT)));
            mediaInfo.setQualityIndex(cursor.getInt(cursor.getColumnIndex(TRACKINDEX)));
            switch (status) {
                case 0:
                    //Idle
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
                case 1:
                    //prepare
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Prepare);
                    break;
                case 2:
                    //wait
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Wait);
                    break;
                case 3:
                    //start
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Start);
                    break;
                case 4:
                    //stop
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Stop);
                    break;
                case 5:
                    //complete
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Complete);
                    break;
                case 6:
                    //error
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Error);
                    break;
                default:
                    mediaInfo.setStatus(AliyunDownloadMediaInfo.Status.Idle);
                    break;
            }
            queryLists.add(mediaInfo);
        }
        cursor.close();
        return queryLists;
    }

    public void close() {
        if (mSqliteDatabase != null) {
            mSqliteDatabase.close();
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}
