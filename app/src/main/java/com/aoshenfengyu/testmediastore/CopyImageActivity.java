package com.aoshenfengyu.testmediastore;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class CopyImageActivity extends RequestPermissionActivity {

    private static final String TAG = "CopyImage";

    protected static final String IMAGE_FILE_NAME = "TestMediaStore_Image.png";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Scoped Download Dir，不需要權限即可存取
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "-------拷貝檔案到Scoped Download Dir------");
                File imageInAppDownloadDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), IMAGE_FILE_NAME);
                copyAssetFileByOldMethod("img_i_am_bad.png", imageInAppDownloadDir);
                Log.i(TAG, "--------------------");
            }
        });

    }

    @Override
    protected void onPermissionsGranted() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "-------拷貝檔案到Public Download Dir------");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 新方法
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "You are bad");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "This is an Android Q test image in the public storage.");
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, IMAGE_FILE_NAME);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

                    copyAssetFile("img_you_are_bad.png", MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                } else {
                    // 舊方法
                    File imageInPublicDownloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), IMAGE_FILE_NAME);
                    copyAssetFileByOldMethod("img_you_are_bad.png", imageInPublicDownloadDir);
                }
                Log.i(TAG, "--------------------");
            }
        });
    }

    @WorkerThread
    protected void copyAssetFileByOldMethod(String assetFileName, File savedFilePath) {
        Log.i(TAG, "使用舊方法，拷貝到" + savedFilePath);
        if (savedFilePath.exists()) {
            Log.i(TAG, "不需要拷貝");
            return;
        }
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            is = getAssets().open(assetFileName);
            fos = new FileOutputStream(savedFilePath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
        } catch (final IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CopyImageActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            });
            Log.i(TAG, "拷貝失敗");
        } finally {
            closeCloseable(fos);
            closeCloseable(is);
        }
    }

    protected void copyAssetFile(String assetFileName, Uri externalDirUri, ContentValues contentValues) {
        Log.i(TAG, "使用新方法，拷貝到" + externalDirUri);
        String displayName = contentValues.getAsString(MediaStore.Images.Media.DISPLAY_NAME);

        // 如果檔案已存在，則刪除它
        boolean exists = exists(externalDirUri, displayName);
        if (exists) {
            delete(externalDirUri, displayName);
        }

        ContentResolver resolver = getContentResolver();

        // 透過insert，系統會生成一個id，
        // 並返回一個由dir uri和id組成的新uri，
        // 作為要插入的檔案的uri。
        Uri insertUri = resolver.insert(externalDirUri, contentValues);
        Log.i(TAG, "insertUri: " + insertUri);

        if (insertUri == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CopyImageActivity.this, "insertUri is null", Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        // 透過openOutputStream取得OutputStream，來寫入檔案
        OutputStream os = null;
        InputStream is = null;
        try {
            os = resolver.openOutputStream(insertUri);
            if (os != null) {
                is = getAssets().open(assetFileName);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CopyImageActivity.this, "openOutputStream fail", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (final IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CopyImageActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            });
            Log.e(TAG, "拷貝失敗");
        } finally {
            closeCloseable(os);
            closeCloseable(is);
        }
    }

    /**
     * 刪除，透過檔案的uri來實現刪除
     **/
    protected void delete(Uri dirUri, String displayName) {
        if (!exists(dirUri, displayName)) {
            return;
        }

        long id = queryId(dirUri, displayName);
        Uri fileUri = Uri.withAppendedPath(dirUri, String.valueOf(id));
        ContentResolver resolver = getContentResolver();
        resolver.delete(fileUri, null, null);
    }

    /**
     * 判斷檔案是否存在
     **/
    protected boolean exists(Uri dirUri, String displayName) {
        return queryId(dirUri, displayName) != -1;
    }

    /**
     * 查詢檔案的id
     **/
    protected long queryId(Uri dirUri, String displayName) {
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(
                dirUri,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DISPLAY_NAME + "=?",
                new String[]{displayName},
                null
        );

        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
            cursor.close();
        }

        return id;
    }

    protected void closeCloseable(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }
}
