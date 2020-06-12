package com.example.timerwidget;

import android.annotation.SuppressLint;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

public class TimerService extends Service {
    ArrayMap<Integer, PausableCDTimer> timerArrayMap = new ArrayMap<>();
    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        TimerService getService(){
            return TimerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        sendBroadcast(new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.SERVICE_STARTED).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
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
                long hours = millisLeft / 3600000;
                long minutes = (millisLeft % 3600000) / 60000;
                String output;
                if(hours != 0){
                    output = String.format("%1$02d:%2$tM:%2$tS", hours, millisLeft);
                } else if (minutes != 0){
                    output = String.format("%1$02d:%2$tS.%2$tL", minutes, millisLeft);
                } else {
                    output = String.format("%1$tS.%1$tL", millisLeft);
                }
                sendBroadcast((new Intent(TimerWidgetProvider.ACTION_TICK)).putExtra(TimerWidgetProvider.EXTRA_TIME_STRING, output).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
            }
            @Override
            public void finish() {
                //TODO: add Ringtone
                sendBroadcast(new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_FINISH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID));
                deleteTimer(widgetID);
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
        if(timer != null) timer.start();
    }
    public void stop(int widgetID){
        PausableCDTimer timer = timerArrayMap.get(widgetID);
        if(timer != null) {
            timer.stop();
            deleteTimer(widgetID);
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
}