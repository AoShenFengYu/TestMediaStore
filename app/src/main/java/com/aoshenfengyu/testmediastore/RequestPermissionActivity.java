package com.aoshenfengyu.testmediastore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class RequestPermissionActivity extends AppCompatActivity {

    private static final String TAG = "RequestPermission";

    private static final int REQ_CODE = 666;

    private static final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean allAreGranted = true;

        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, " permission：" + permission);
                allAreGranted = false;
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    onPermissionsNotGranted();
                } else {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_CODE);
                }
                break;
            }
        }

        if (allAreGranted) {
            onPermissionsGranted();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE: {
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            onPermissionsNotGranted();
                            return;
                        }
                    }

                    onPermissionsGranted();
                } else {
                    onPermissionsNotGranted();
                }
            }
            break;
        }
    }

    protected abstract void onPermissionsGranted();

    private void onPermissionsNotGranted() {
        Toast.makeText(this, "請給我權限，用來拷貝範例圖檔到Public Download Dir", Toast.LENGTH_LONG).show();
    }
}
