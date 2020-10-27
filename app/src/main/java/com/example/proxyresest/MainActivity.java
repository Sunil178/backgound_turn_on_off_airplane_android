package com.example.proxyresest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            final PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(MyWorker.class, 10, TimeUnit.SECONDS)
                    .setConstraints(Constraints.NONE)
                    .addTag("FLIGHT_MODE_TOGGLE")
                    .build();
//            final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).build();
                WorkManager.getInstance().enqueue(work);
        }
    }
    public void stopToggle(View view) {
        WorkManager.getInstance().cancelAllWorkByTag("FLIGHT_MODE_TOGGLE");
        Toast.makeText(getApplicationContext(),"Toggle Airplane Mode Stopped",Toast.LENGTH_SHORT).show();
    }
}
