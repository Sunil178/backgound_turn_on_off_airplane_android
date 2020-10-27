package com.example.proxyresest;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.provider.Settings.System.getString;
import static androidx.core.content.ContextCompat.getSystemService;

public class MyWorker extends Worker{
    private NotificationManager notificationManager;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }
    private Socket mSocket;
    {
        try {
            IO.Options options = new IO.Options();
            options.port = 6969;
            options.reconnection = true;
            options.forceNew = true;
            mSocket = IO.socket("http://127.0.0.1:6969", options);
        } catch (URISyntaxException e) {
            Log.e("abc", "index=" + e);
        }
    }

    private final Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            try {
                Process onCommand = Runtime.getRuntime().exec("su");
                DataOutputStream turnOn = new DataOutputStream(onCommand.getOutputStream());
                turnOn.writeBytes("settings put global airplane_mode_on 1\n");
                turnOn.flush();
                turnOn.writeBytes("am broadcast -a android.intent.action.AIRPLANE_MODE\n");
                turnOn.flush();
                turnOn.writeBytes("exit\n");
                turnOn.flush();
                onCommand.waitFor();
                Thread.sleep(2000);
                Process offCommand = Runtime.getRuntime().exec("su");
                DataOutputStream turnOff = new DataOutputStream(offCommand.getOutputStream());
                turnOff.writeBytes("settings put global airplane_mode_on 0\n");
                turnOff.flush();
                turnOff.writeBytes("am broadcast -a android.intent.action.AIRPLANE_MODE\n");
                turnOff.flush();
                turnOff.writeBytes("exit\n");
                turnOff.flush();
                offCommand.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        // Build a notification using bytesRead and contentLength

        Context context = getApplicationContext();
        String id = context.getString(R.string.notification_channel_id);
        String title = context.getString(R.string.notification_title);
        String cancel = context.getString(R.string.cancel_download);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, id)
                .setContentTitle(title)
                .setTicker(title)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(R.string.NOTIFICATION_ID, notification);
        return new ForegroundInfo(R.string.notification_channel_id, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "proxyreset_channel_id";
        CharSequence channelName = "ProxyReset";
        int importance = NotificationManager.IMPORTANCE_MAX;
        @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notificationManager.createNotificationChannel(notificationChannel);

        String groupId = "proxyreset_group_id";
        CharSequence groupName = "ProxyReset";
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(groupId, groupName));
    }

    @Override
    public Result doWork() {
        String progress = "Starting Download";
        setForegroundAsync(createForegroundInfo(progress));
        mSocket.on("Material", onNewMessage);
        mSocket.connect();
        mSocket.emit("requestToResetNetwork", "android");
        return Result.success();
    }
}
