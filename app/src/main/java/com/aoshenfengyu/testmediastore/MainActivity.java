package com.aoshenfengyu.testmediastore;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.databinding.DataBindingUtil;

import com.aoshenfengyu.testmediastore.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * 本項目在演示如何在 Android Q 上存取檔案
 * <p>
 * Environment.getExternalStoragePublicDirectory(type) 已經被淘汰
 * <p>
 * 取而代之，使用 context.getExternalFilesDir(type)
 **/
public class MainActivity extends CopyImageActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private Bitmap scopedStorageDirImageBitmap;
    private Bitmap publicStorageDirImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.tvShowImgInScopedStorageDownloadDir.setText("顯示 Scoped Storage 的圖檔，路徑 = " + getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + IMAGE_FILE_NAME);
        binding.btnImgInScopedStorageDownloadDir.setOnClickListener(this);

        binding.tvShowImgInPublicStorageDownloadDir.setText("顯示 Public Storage 的圖檔，路徑 = " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + IMAGE_FILE_NAME);
        binding.btnImgInPublicStorageDownloadDir.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_img_in_scoped_storage_download_dir:
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        recycleBitmap(scopedStorageDirImageBitmap);

                        File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), IMAGE_FILE_NAME);

                        scopedStorageDirImageBitmap = readImageByOldMethod(imageFile);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.imImgInScopedStorageDownloadDir.setImageBitmap(scopedStorageDirImageBitmap);
                            }
                        });
                    }
                });
                break;

            case R.id.btn_img_in_public_storage_download_dir:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 以下為新方法
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            recycleBitmap(publicStorageDirImageBitmap);

                            Uri dirUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                            long id = queryId(dirUri, IMAGE_FILE_NAME);
                            Uri fileUri = Uri.withAppendedPath(dirUri, String.valueOf(id));

                            publicStorageDirImageBitmap = readImage(fileUri);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.imImgInPublicStorageDownloadDir.setImageBitmap(publicStorageDirImageBitmap);
                                }
                            });
                        }
                    });
                } else {
                    // 以下為舊方法
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            recycleBitmap(publicStorageDirImageBitmap);

                            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), IMAGE_FILE_NAME);

                            publicStorageDirImageBitmap = readImageByOldMethod(imageFile);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    binding.imImgInPublicStorageDownloadDir.setImageBitmap(publicStorageDirImageBitmap);
                                }
                            });
                        }
                    });
                }
                break;

        }
    }

    @WorkerThread
    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @WorkerThread
    private Bitmap readImageByOldMethod(File imageFile) {
        if (!imageFile.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }

    @WorkerThread
    private Bitmap readImage(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        Bitmap bitmap = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            }
        } catch (final IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            });
        } finally {
            closeCloseable(parcelFileDescriptor);
        }
        return bitmap;
    }
}
