package com.example.timerwidget;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Timer;

public class TimerService extends Service {
    public static final String ACTION_PLAY = "com.timerwidget.timerservice.action.PLAY";
    public static final String ACTION_PAUSE = "com.timerwidget.timerservice.action.PAUSE";
    public static final String ACTION_STOP = "com.timerwidget.timerservice.action.STOP";
    public static final String ACTION_SELF_DESTRUCT = "com.timerwidget.timerservice.action.SELF_DESTRUCT";
    public static final String ACTION_WAKE = "com.timerwidget.timerservice.action.WAKE";
    public static final String ACTION_UPDATE_TIMER = "com.timerwidget.timerservice.UPDATE_TIMER";
    public static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "TimerServiceNotificationChannel";
    private PausableCDTimer selfDestructTimer;
    ArrayMap<Integer, PausableCDTimer> timerArrayMap = new ArrayMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_UPDATE_TIMER);
        filter.addAction(ACTION_SELF_DESTRUCT);
        registerReceiver(receiver, filter);
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Service Running")
                .setContentText("Click to stop")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(PendingIntent.getBroadcast(this, 0 , new Intent(ACTION_SELF_DESTRUCT), 0))
                .build();
        startForeground(NOTIFICATION_ID, notification);
        Log.d("TimerService", "onCreate receiver registered");

        selfDestructTimer = new PausableCDTimer(-1, 300000, true) {
            @Override
            public void tick(long millisLeft) { }
            @Override
            public void finish() {
                selfDestruct();
            }
        };
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "TimerServiceNotificationChannel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d("TimerService", "onReceive called action = " + action);
            if(action != null){
                switch(action){
                    case ACTION_PLAY:{
                        if(selfDestructTimer.isTimerRunning()) selfDestructTimer.stop();
                        start(widgetID);
                        break;
                    }
                    case ACTION_PAUSE:{
                        pause(widgetID);
                        break;
                    }
                    case ACTION_STOP:{
                        if(!anyTimersRunning()) selfDestructTimer.start();
                        stop(widgetID);
                        break;
                    }
                    case ACTION_UPDATE_TIMER:{
                        deleteTimer(widgetID);
                        addTimer(widgetID, false);
                        break;
                    }
                    case ACTION_SELF_DESTRUCT:{
                        selfDestruct();
                        break;
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String action = intent.getAction();
        if(action != null && action.equals(ACTION_WAKE)){
            addTimer(widgetID, false);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void addTimer(int widgetID, boolean runAtStart){
        Context context = getApplicationContext();
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.widget_time_pref_key), Context.MODE_PRIVATE);
        long startingTime = preferences.getLong(context.getString(R.string.pref_time_key) + widgetID, context.getResources().getInteger(R.integer.default_timer_length));
        PausableCDTimer timer = new PausableCDTimer(widgetID, startingTime, runAtStart) {
            @SuppressLint("DefaultLocale")
            @Override
            public void tick(long millisLeft) {
                Log.d("PausableCDTimer", "tick timeLeft = " + millisLeft);
                long hours = millisLeft / 3600000;
                long minutes = (millisLeft % 3600000) / 60000;
                String output;
                if(hours != 0){
                    output = String.format("%1$02d:%2$tM:%2$tS", hours, millisLeft);
                } else if (minutes != 0){
                    output = String.format("%1$02d:%2$tS", minutes, millisLeft);
                } else {
                    output = String.format("%1$tS.%1$tL", millisLeft).substring(0, 4);
                }
                sendBroadcast(new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_TICK).putExtra(TimerWidgetProvider.EXTRA_TIME_STRING, output).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
            }
            @Override
            public void finish() {
                //TODO: add Ringtone
                if(!anyTimersRunning()) selfDestructTimer.start();
                sendBroadcast(new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_FINISH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
            }
        };
        timerArrayMap.put(widgetID, timer);
    }
    public void deleteTimer(int widgetID){
        timerArrayMap.remove(widgetID);
    }
    public void pause(int widgetID){
        PausableCDTimer timer = timerArrayMap.get(widgetID);
        if(timer != null) timer.pause();
    }
    public void resume(int widgetID){
        PausableCDTimer timer = timerArrayMap.get(widgetID);
        if(timer != null) timer.resume();
    }
    public void start(int widgetID){
        PausableCDTimer timer = timerArrayMap.get(widgetID);
        if(timer != null) {
            timer.start();
        }
    }
    public void stop(int widgetID){
        PausableCDTimer timer = timerArrayMap.get(widgetID);
        if(timer != null) {
            timer.stop();
        }
    }
    public boolean timerExists(int widgetID){
        return timerArrayMap.containsKey(widgetID);
    }
    public boolean timerRunning(int widgetID){
        if(timerExists(widgetID)){
            PausableCDTimer timer = timerArrayMap.get(widgetID);
            if(timer != null){
                return timer.isTimerRunning();
            }
        }
        return false;
    }

    private boolean anyTimersRunning(){
        for(int i = 0; i < timerArrayMap.size(); i++){
            if(timerArrayMap.valueAt(i).isTimerRunning()) return true;
        }
        return false;
    }

    private void selfDestruct(){
        stopSelf();
        sendBroadcast(new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_SLEEP));
    }
}