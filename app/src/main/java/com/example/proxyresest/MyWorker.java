package com.example.proxyresest;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
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
import android.content.Intent;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class MyWorker extends Worker{
    private Context context;
    private NotificationManager notificationManager;
    private String bounds, x, y;
    NodeList nodes;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
        this.context = context;
    }

    private Socket mSocket;
    {
        try {
            IO.Options options = new IO.Options();
            options.port = 2000;
            options.reconnection = true;
            options.forceNew = true;
            mSocket = IO.socket("http://15.207.247.154:2000/", options);
//            mSocket = IO.socket("http://127.0.0.1:2000/", options);
        } catch (URISyntaxException e) {
            Log.e("abc", "index=" + e);
        }
    }

    public void setClipboard(String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(text);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                    clipboard.setPrimaryClip(clip);
                }
            }
        });
    }

    private final Emitter.Listener onStartProxidize = new Emitter.Listener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void call(final Object... args) {
            try {
                Runtime.getRuntime().exec("su -c am start -n com.proxidize/com.activity.LoginActivity");
                setClipboard("");
                Thread.sleep(10000);
                Runtime.getRuntime().exec("uiautomator dump");
                XPathFactory factory2 = XPathFactory.newInstance();
                XPath xPath = factory2.newXPath();
                NodeList nodes = (NodeList) xPath.evaluate("//*[contains(@text, 'COPY PROXY TO CLIPBOARD')]", new InputSource(new FileReader("/sdcard/window_dump.xml")), XPathConstants.NODESET);
                Element connect = (Element) nodes.item(0);
                String connectBounds = connect.getAttribute("bounds");
                System.out.println(connectBounds);
                x = Integer.toString((Integer.parseInt(connectBounds.split("]\\[")[0].split(",")[0].replace("[", ""))) + (((Integer.parseInt(connectBounds.split("]\\[")[1].split(",")[0])) - (Integer.parseInt(connectBounds.split("]\\[")[0].split(",")[0].replace("[", "")))) / 2));
                y = Integer.toString((Integer.parseInt(connectBounds.split("]\\[")[0].split(",")[1])) + (((Integer.parseInt(connectBounds.split("]\\[")[1].split(",")[1].replace("]", ""))) - (Integer.parseInt(connectBounds.split("]\\[")[0].split(",")[1]))) / 2));
                Runtime.getRuntime().exec("su -c input tap " + x + " " + y + "\n");
                Thread.sleep(2000);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                        TelephonyManager TelephonyMgr = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
                        String imei = TelephonyMgr.getDeviceId();
                        ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
                        String copyedText = (String) clipboard.getText();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("device_id", android_id);
                            obj.put("imei_number", imei);
                            obj.put("proxy_text", copyedText);
                            mSocket.emit("proxyStringResponse", obj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException | InterruptedException | XPathExpressionException e) {
                e.printStackTrace();
            }
        }
    };



    private final Emitter.Listener onForceStartProxidize = new Emitter.Listener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void call(final Object... args) {
            try {
                Runtime.getRuntime().exec("su -c am force-stop com.proxidize");
                Thread.sleep(5000);
                Runtime.getRuntime().exec("su -c am start -n com.proxidize/com.activity.LoginActivity");
                setClipboard("");
                Thread.sleep(10000);
                Runtime.getRuntime().exec("uiautomator dump");
                XPathFactory factory2 = XPathFactory.newInstance();
                XPath xPath = factory2.newXPath();
                nodes = (NodeList) xPath.evaluate("//*[contains(@text, 'CONNECT')]", new InputSource(new FileReader("/sdcard/window_dump.xml")), XPathConstants.NODESET);
                bounds = ((Element) nodes.item(0)).getAttribute("bounds");
                x = Integer.toString((Integer.parseInt(bounds.split("]\\[")[0].split(",")[0].replace("[", ""))) + (((Integer.parseInt(bounds.split("]\\[")[1].split(",")[0])) - (Integer.parseInt(bounds.split("]\\[")[0].split(",")[0].replace("[", "")))) / 2));
                y = Integer.toString((Integer.parseInt(bounds.split("]\\[")[0].split(",")[1])) + (((Integer.parseInt(bounds.split("]\\[")[1].split(",")[1].replace("]", ""))) - (Integer.parseInt(bounds.split("]\\[")[0].split(",")[1]))) / 2));
                Runtime.getRuntime().exec("su -c input tap " + x + " " + y + "\n");
                Thread.sleep(10000);
                nodes = (NodeList) xPath.evaluate("//*[contains(@text, 'COPY PROXY TO CLIPBOARD')]", new InputSource(new FileReader("/sdcard/window_dump.xml")), XPathConstants.NODESET);
                bounds = ((Element) nodes.item(0)).getAttribute("bounds");
                x = Integer.toString((Integer.parseInt(bounds.split("]\\[")[0].split(",")[0].replace("[", ""))) + (((Integer.parseInt(bounds.split("]\\[")[1].split(",")[0])) - (Integer.parseInt(bounds.split("]\\[")[0].split(",")[0].replace("[", "")))) / 2));
                y = Integer.toString((Integer.parseInt(bounds.split("]\\[")[0].split(",")[1])) + (((Integer.parseInt(bounds.split("]\\[")[1].split(",")[1].replace("]", ""))) - (Integer.parseInt(bounds.split("]\\[")[0].split(",")[1]))) / 2));
                Runtime.getRuntime().exec("su -c input tap " + x + " " + y + "\n");
                Thread.sleep(2000);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                        TelephonyManager TelephonyMgr = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
                        String imei = TelephonyMgr.getDeviceId();
                        ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(CLIPBOARD_SERVICE);
                        String copyedText = (String) clipboard.getText();
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("device_id", android_id);
                            obj.put("imei_number", imei);
                            obj.put("proxy_text", copyedText);
                            mSocket.emit("proxyStringResponse", obj);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException | InterruptedException | XPathExpressionException e) {
                e.printStackTrace();
            }
        }
    };



    private final Emitter.Listener onNewMessage = new Emitter.Listener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void call(final Object... args) {
            try {
                Runtime.getRuntime().exec("su -c settings put global airplane_mode_on 1\n");
                Runtime.getRuntime().exec("su -c am broadcast -a android.intent.action.AIRPLANE_MODE\n");
                Thread.sleep(2000);
                Runtime.getRuntime().exec("su -c settings put global airplane_mode_on 0\n");
                Runtime.getRuntime().exec("su -c am broadcast -a android.intent.action.AIRPLANE_MODE\n");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {

        Context context = getApplicationContext();
        String id = context.getString(R.string.notification_channel_id);
        String title = context.getString(R.string.notification_title) + " from " + Integer.toString(Global.count) + " times";
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
                .setContentIntent(
                        PendingIntent.getActivity(context, 10,
                        new Intent(context, MainActivity.class)
                                .addFlags(FLAG_ACTIVITY_CLEAR_TOP), 0))
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
        Global.count++;
        System.out.println(Global.count);
        String progress = "Starting Download";
        setForegroundAsync(createForegroundInfo(progress));
        mSocket.on("resetRequest", onNewMessage);
        mSocket.on("requestToOpenProxidize", onStartProxidize);
        mSocket.on("requestToOpenForceProxidize", onForceStartProxidize);
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                @SuppressLint("HardwareIds") String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                TelephonyManager TelephonyMgr = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
                String imei = TelephonyMgr.getDeviceId();
                String mobile_name = Build.BRAND + " " + Build.MODEL;
                BatteryManager bm = (BatteryManager) getApplicationContext().getSystemService(BATTERY_SERVICE);
                int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                JSONObject obj = new JSONObject();
                try {
                    obj.put("mobile_name", mobile_name);
                    obj.put("device_id", android_id);
                    obj.put("imei_number", imei);
                    obj.put("battery", batLevel);
                    obj.put("socket_id", mSocket.id());
                    mSocket.emit("setDevice", obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mSocket.connect();
        return Result.success();
    }
}
