/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.model;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.tool.ResourceParser.NoteBgResources;


/**
 * 此类是用于与数据库进行数据交互的类
 */
public class WorkingNote {
    // Note for the working note
    private Note mNote;
    // Note Id
    // 便签ID
    private long mNoteId;
    // Note content
    // 便签内容
    private String mContent;
    // Note mode
    // 便签模式
    private int mMode;
    //便签提醒日期
    private long mAlertDate;
    //便签修改日期
    private long mModifiedDate;
    //便签颜色ID
    private int mBgColorId;
    //便签控件ID
    private int mWidgetId;
    //便签控件类型
    private int mWidgetType;
    //便签文件夹ID
    private long mFolderId;
    //便签环境
    private Context mContext;
    //便签标签名
    private static final String TAG = "WorkingNote";
    //便签被删除
    private boolean mIsDeleted;
    //设置更改监听器以及状态监听器

    //图片路径Uri
    private String mImagePath;
    //文本样式选择
    private int mFontSelect;

    private NoteSettingChangedListener mNoteSettingStatusListener;
    //定义一系列数据投影，用于从数据库中获取该数组中包含的列值
    public static final String[] DATA_PROJECTION = new String[] {
            DataColumns.ID,                         //数据列ID
            DataColumns.CONTENT,                   //内容
            DataColumns.MIME_TYPE,                 //类型
            DataColumns.DATA1,                      //数据1，2，3，4
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
            DataColumns.IMAGE_PATH,
            DataColumns.FONT_SELECT
    };
    // 定义一系列标签投影
    public static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.PARENT_ID,                      //起始ID
            NoteColumns.ALERTED_DATE,                  //闹钟日期
            NoteColumns.BG_COLOR_ID,                   //背景颜色ID
            NoteColumns.WIDGET_ID,                      //桌面小部件ID
            NoteColumns.WIDGET_TYPE,                     //桌面小部件类型
            NoteColumns.MODIFIED_DATE                   //最新的修改日期
    };
    //用于获取上方数据投影中指定列的值
    private static final int DATA_ID_COLUMN = 0;

    private static final int DATA_CONTENT_COLUMN = 1;

    private static final int DATA_MIME_TYPE_COLUMN = 2;

    private static final int DATA_MODE_COLUMN = 3;

    private static final int DATA_IMAGE_PATH_COLUMN = 7;

    private static final int DATA_FONT_SELECT_COLUMN = 8;

    private static final int NOTE_PARENT_ID_COLUMN = 0;

    private static final int NOTE_ALERTED_DATE_COLUMN = 1;

    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;

    private static final int NOTE_WIDGET_ID_COLUMN = 3;

    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;

    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    // New note construct
    // 新建便签的构造方法
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        mAlertDate = 0;
        mModifiedDate = System.currentTimeMillis();   //获得当前系统时间
        mFolderId = folderId;
        mNote = new Note();
        mNoteId = 0;
        mIsDeleted = false;
        mMode = 0;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
    }

    // Existing note construct
    // 现有便签的构造方法
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        mIsDeleted = false;
        mNote = new Note();
        loadNote();
    }
    //Context.getContentResolver().query获取后面的一些信息：文件名ID，颜色ID，小控件ID，小控件类型，闹钟提醒日期，修改日期
    private void loadNote() {
        /**
         * public final Cursor query (Uri uri, String[] projection,String selection,String[] selectionArgs, String sortOrder)
         * uri:指的是唯一的标识来标识这个Provider（）内容提供者
         * projection:告诉查询要返回的列（Column）
         * selection:查询where字句
         * selectionArgs: 查询条件属性值
         * sortOrder:结果排序规则(升序、降序等)
         */
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
            }
            cursor.close();
        } else {
            Log.e(TAG, "No note with id:" + mNoteId);//没有ID的便签为：
            throw new IllegalArgumentException("Unable to find note with id " + mNoteId);//找不到ID为**的便签
        }
        loadNoteData();
    }
      //类似于上面的Context.getContentResolver().query方法，用于获取该id便签的数据内容信息
    private void loadNoteData() {
        Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[] {
                        String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                    if (DataConstants.NOTE.equals(type)) {
                        mContent = cursor.getString(DATA_CONTENT_COLUMN);
                        mMode = cursor.getInt(DATA_MODE_COLUMN);
                        mImagePath = cursor.getString(DATA_IMAGE_PATH_COLUMN);
                        mFontSelect = cursor.getInt(DATA_FONT_SELECT_COLUMN);
                        Log.e("font_select", String.valueOf(mFontSelect));
                        mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else if (DataConstants.CALL_NOTE.equals(type)) {
                        mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else {
                        Log.d(TAG, "Wrong note type with type:" + type);//类型为的标签类型错误：
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            Log.e(TAG, "No data with id:" + mNoteId);//没有ID为的数据：
            throw new IllegalArgumentException("Unable to find note's data with id " + mNoteId);//找不到ID为的便笺数据
        }
    }
   //新建一个空便签
    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
                                              int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        note.setBgColorId(defaultBgColorId);
        note.setWidgetId(widgetId);
        note.setWidgetType(widgetType);
        return note;
    }
    //处理便签数据
    public static WorkingNote load(Context context, long id) {
        return new WorkingNote(context, id, 0);
    }

    public synchronized boolean saveNote() {
        if (isWorthSaving()) {
            if (!existInDatabase()) {
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    Log.e(TAG, "Create new note fail with id:" + mNoteId);//使用新ID创建便签失败
                    return false;
                }
            }

            mNote.syncNote(mContext, mNoteId);

            /**
             * Update widget content if there exist any widget of this note
             * 如果此标签中存在任何桌面小部件，则更新桌面小部件内容
             */
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();//给便签设置监听器
            }
            return true;
        } else {
            return false;
        }
    }
    //存在于数据库中
    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    //满足下面条件即可存储
    private boolean isWorthSaving() {
        if (mIsDeleted || (!existInDatabase() && TextUtils.isEmpty(mContent))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }
      //设置状态改变监听器
    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }
      //设置闹钟提醒日期
    public void setAlertDate(long date, boolean set) {
        if (date != mAlertDate) {
            mAlertDate = date;
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
    }
    //删除标记
    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onWidgetChanged();
        }
    }
     //设置背景颜色ID
    public void setBgColorId(int id) {
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }
    //设置检查列表模式
    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }
     //设置桌面小部件类型
    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }
     //设置桌面小部件ID
    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }
    //设置清单模式的文本
    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    public void setWorkingImage(String imagepath){
        mImagePath = imagepath;
        mNote.setTextData(DataColumns.IMAGE_PATH,mImagePath);
    }

    public void setFontSelect(int id){
        mFontSelect = id;
        mNote.setFontData(DataColumns.FONT_SELECT, mFontSelect);
    }
    //转换调用便签
    public void convertToCallNote(String phoneNumber, long callDate) {
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
    }
   //返回一些数值
    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    public String getContent() {
        return mContent;
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getBgColorResId() {
        return NoteBgResources.getNoteBgResource(mBgColorId);
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public int getTitleBgResId() {
        return NoteBgResources.getNoteTitleBgResource(mBgColorId);
    }

    public int getCheckListMode() {
        return mMode;
    }

    public long getNoteId() {
        return mNoteId;
    }

    public long getFolderId() {
        return mFolderId;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public int getWidgetType() {
        return mWidgetType;
    }
    public String getImagePath(){
        return mImagePath;
    }

    public int getFontSelect(){
        return mFontSelect;
    }

    public interface NoteSettingChangedListener {
        /**
         * Called when the background color of current note has just changed
         * 当当前便笺的背景色刚刚更改时调用
         */
        void onBackgroundColorChanged();

        /**
         * Called when user set clock
         * 当用户设置时钟时调用
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * Call when user create note from widget
         * 当用户从小部件创建便签时调用
         */
        void onWidgetChanged();

        /**
         * Call when switch between check list mode and normal mode
         * 在清单模式和正常模式之间切换时调用
         * @param oldMode is previous mode before change旧模式是更改前的模式
         * @param newMode is new mode新模式是新模式
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}
