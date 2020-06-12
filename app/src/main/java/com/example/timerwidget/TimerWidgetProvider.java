package com.example.timerwidget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.RemoteViews;


public class TimerWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "TimerWidgetProvider";
    public static final String ACTION_PLAY = "com.timerwidget.widget.action.PLAY";
    public static final String ACTION_PAUSE = "com.timerwidget.widget.action.PAUSE";
    public static final String ACTION_STOP = "com.timerwidget.widget.action.STOP";
    public static final String ACTION_TICK = "com.timerwidget.widget.action.TICK";
    public static final String ACTION_WAKE = "com.timerwidget.widget.action.WAKE";
    public static final String ACTION_SLEEP = "com.timerwidget.widget.action.SLEEP";
    public static final String ACTION_FINISH = "com.timerwidget.widget.action.FINISH";
    public static final String EXTRA_TIME_STRING = "com.timerwidget.widget.extra.TIME_STRING";
    public static final String SERVICE_STARTED = "com.timerwidget.widget.SERVICE_STARTED";
    public static final String EXTRA_TIME_VALUE = "com.timerwidget.widget.extra.TIME_VALUE";

    private ArrayMap<Integer, Boolean> widgetActiveArrayMap = new ArrayMap<>();
    private TimerService boundService;
    private boolean isBound = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timer);
        int appwidgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        Log.d(TAG, "action = " + action + " widgetID = " + appwidgetID);
        if(action != null){
            switch(action){
                case ACTION_PLAY:{
                    views.setOnClickPendingIntent(R.id.playPauseButton, getPendingBroadcast(context, ACTION_PAUSE, appwidgetID));
                    views.setTextViewText(R.id.playPauseButton, "PAUSE");
                    views.setBoolean(R.id.stopButton, "setEnabled", true);
                    if(isBound){
                        widgetActiveArrayMap.replace(appwidgetID, true);
                        if(boundService.timerExists(appwidgetID)){
                            boundService.resume(appwidgetID);
                        } else {
                            boundService.addTimer(appwidgetID, true);
                        }
                    }
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_PAUSE:{
                    views.setOnClickPendingIntent(R.id.playPauseButton, getPendingBroadcast(context, ACTION_PLAY, appwidgetID));
                    views.setTextViewText(R.id.playPauseButton, "PLAY");
                    if(isBound){
                        if(boundService.timerExists(appwidgetID)){
                            boundService.pause(appwidgetID);
                        } else {
                            boundService.addTimer(appwidgetID, false);
                        }
                    }
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_STOP:{
                    views.setOnClickPendingIntent(R.id.playPauseButton, getPendingBroadcast(context, ACTION_PLAY, appwidgetID));
                    views.setTextViewText(R.id.playPauseButton, "PLAY");
                    views.setBoolean(R.id.stopButton, "setEnabled", false);
                    if(isBound){
                        widgetActiveArrayMap.replace(appwidgetID, false);
                        if(boundService.timerExists(appwidgetID)){
                            boundService.stop(appwidgetID);
                        }
                        context.unbindService(connection);
                        isBound = false;
                    }
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_TICK:{
                    views.setTextViewText(R.id.timerTextView, intent.getStringExtra(EXTRA_TIME_STRING));
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_FINISH:{
                    widgetActiveArrayMap.replace(appwidgetID, false);
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, resetWidget(context, appwidgetID));
                    break;
                }
                case ACTION_WAKE:{
                    views.setInt(R.id.wakeButton, "setVisibility", 8);
                    if(!isBound){
                        Intent serviceIntent = new Intent(context, TimerService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appwidgetID);
                        context.startService(serviceIntent);
                    }
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_SLEEP:{
                    views.setInt(R.id.wakeButton, "setVisibility", 0);
                    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                    for(int widgetID: widgetManager.getAppWidgetIds(new ComponentName(context, TimerWidgetProvider.class))){
                        widgetManager.updateAppWidget(widgetID, views);
                    }
                    widgetManager.updateAppWidget(appwidgetID, views);
                    break;
                }
                case SERVICE_STARTED:{
                    context.bindService(intent, connection, Context.BIND_IMPORTANT);
                    break;
                }
                default: {

                }
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) iBinder;
            boundService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            boundService = null;
            isBound = false;
        }
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for(int appWidgetId: appWidgetIds){
            boolean timerRunning = false;
            if(isBound){
                timerRunning = boundService.timerRunning(appWidgetId);
                if(!timerRunning){
                    appWidgetManager.updateAppWidget(appWidgetId, resetWidget(context, appWidgetId));
                }
            } else {
                appWidgetManager.updateAppWidget(appWidgetId, resetWidget(context, appWidgetId));
            }
            Log.d("onUpdate", "isBound = " + isBound + " timerRunning = " + timerRunning);
        }
    }


    private PendingIntent getPendingBroadcast(Context context, String action, int widgetId){
        Intent intent = new Intent(context, TimerWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private RemoteViews resetWidget(Context context, int appWidgetId){
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.widget_time_pref_key), Context.MODE_PRIVATE);
        long startingTime = preferences.getLong(context.getString(R.string.pref_time_key) + appWidgetId, context.getResources().getInteger(R.integer.default_timer_length));
        Log.d("resetWidget()", "startingTime = " + startingTime + " widgetID = " + appWidgetId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timer);
        views.setOnClickPendingIntent(R.id.timerTextView, PendingIntent.getActivity(
                context,
                0,
                new Intent(context, SetTimeActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT));
        views.setTextViewText(R.id.timerTextView, millisToTime(startingTime));
        views.setOnClickPendingIntent(R.id.playPauseButton, getPendingBroadcast(context, ACTION_PLAY, appWidgetId));
        views.setTextViewText(R.id.playPauseButton, "PLAY");
        views.setOnClickPendingIntent(R.id.stopButton, getPendingBroadcast(context, ACTION_STOP, appWidgetId));
        views.setTextViewText(R.id.stopButton, "STOP");
        views.setOnClickPendingIntent(R.id.wakeButton, getPendingBroadcast(context, ACTION_WAKE, appWidgetId));
        return views;
    }

    @SuppressLint("DefaultLocale")
    private String millisToTime(long millis){
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        String output;
        if(hours != 0){
            output = String.format("%1$02d:%2$tM:%2$tS", hours, millis);
        } else if (minutes != 0){
            output = String.format("%1$02d:%2$tS.%2$tL", minutes, millis);
        } else {
            output = String.format("%1$tS.%1$tL", millis);
        }
        return output;
    }
}
