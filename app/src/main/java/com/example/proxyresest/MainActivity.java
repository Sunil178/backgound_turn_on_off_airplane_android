package com.example.proxyresest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private boolean flag = false;
    private String permission = Manifest.permission.READ_PHONE_STATE;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        flag = (getApplicationContext().checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        if (flag)
            startSocket();
        else
            askPermission();
    }
    public void askPermission () {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 10);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (!Arrays.asList(grantResults).contains(PackageManager.PERMISSION_DENIED)) {
                startSocket();
            }
        }
        else
            askPermission();
    }

    public void startSocket () {
        final PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(MyWorker.class, 0, TimeUnit.SECONDS)
                .setConstraints(Constraints.NONE)
                .addTag("FLIGHT_MODE_TOGGLE")
                .build();
        Global.count = 0;
        WorkManager.getInstance().enqueueUniquePeriodicWork("FLIGHT_MODE_TOGGLE", ExistingPeriodicWorkPolicy.REPLACE, work);
    }

    public void stopToggle(View view) {
        WorkManager.getInstance().cancelAllWorkByTag("FLIGHT_MODE_TOGGLE");
        Toast.makeText(getApplicationContext(),"Toggle Airplane Mode Stopped",Toast.LENGTH_SHORT).show();
    }
}
