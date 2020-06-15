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
import android.widget.Toast;


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
    public static final String ACTION_RESET = "com.timerwidget.widget.extra.RESET";
    public static final String SERVICE_STARTED = "com.timerwidget.widget.SERVICE_STARTED";
    public static final String EXTRA_TIME_VALUE = "com.timerwidget.widget.extra.TIME_VALUE";

    private ArrayMap<Integer, Boolean> widgetActiveArrayMap = new ArrayMap<>();

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
                    views.setTextColor(R.id.stopButton, context.getResources().getColor(android.R.color.white, null));
                    widgetActiveArrayMap.replace(appwidgetID, true);
                    context.sendBroadcast(new Intent(TimerService.ACTION_PLAY).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appwidgetID));
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_PAUSE:{
                    views.setOnClickPendingIntent(R.id.playPauseButton, getPendingBroadcast(context, ACTION_PLAY, appwidgetID));
                    views.setTextViewText(R.id.playPauseButton, "PLAY");
                    widgetActiveArrayMap.replace(appwidgetID, false);
                    context.sendBroadcast(new Intent(TimerService.ACTION_PAUSE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appwidgetID));
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_STOP:{
                    widgetActiveArrayMap.replace(appwidgetID, false);
                    context.sendBroadcast(new Intent(TimerService.ACTION_STOP).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appwidgetID));
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, resetWidget(context, appwidgetID));
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
                    context.startForegroundService(new Intent(context, TimerService.class).setAction(TimerService.ACTION_WAKE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appwidgetID));
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, views);
                    break;
                }
                case ACTION_SLEEP:{
                    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                    for(int widgetID: widgetManager.getAppWidgetIds(new ComponentName(context, TimerWidgetProvider.class))){
                        RemoteViews resetView = resetWidget(context, widgetID);
                        resetView.setInt(R.id.wakeButton, "setVisibility", 0);
                        widgetManager.updateAppWidget(widgetID, resetView);
                    }
                    break;
                }
                case ACTION_RESET:{
                    AppWidgetManager.getInstance(context).updateAppWidget(appwidgetID, resetWidget(context, appwidgetID));
                    break;
                }
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:{
                    String ids = "";
                    for(int id: AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, TimerWidgetProvider.class))){
                        ids = ids.concat(" " + id + " ");
                    }
                    String extraids = "";
                    int[] extraArr = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    if(extraArr != null){
                        for(int id: extraArr){
                            extraids = extraids.concat(" " + id + " ");
                        }
                    }

                    Log.d("TimerWidgetProvider", "IDS = " + ids + " extraIDs = " + extraids);
                }
                default: {

                }
            }
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.stopService(new Intent(context, TimerService.class));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for(int appWidgetId: appWidgetIds){
            Toast.makeText(context, "onUpdate " + appWidgetId, Toast.LENGTH_LONG).show();
            try{
                boolean timerRunning = widgetActiveArrayMap.get(appWidgetId);
                if(!timerRunning){
                    Log.d("TimerWidgetProvider", "updateAppWidget called");
                    appWidgetManager.updateAppWidget(appWidgetId, resetWidget(context, appWidgetId));
                }
            } catch(Exception ex){
                Log.d("TimerWidgetProvider", "updateAppWidget called in exception");
                widgetActiveArrayMap.put(appWidgetId, false);
                appWidgetManager.updateAppWidget(appWidgetId, resetWidget(context, appWidgetId));
            }
        }
    }


    private PendingIntent getPendingBroadcast(Context context, String action, int widgetId){
        Log.d("TimerWidgetProvider", "PendingBroadcast Action = " + action + " Created id = " + widgetId);
        Intent intent = new Intent(context, TimerWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, widgetId, intent, 0);
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
        views.setBoolean(R.id.stopButton, "setEnabled", false);
        views.setTextColor(R.id.stopButton, context.getResources().getColor(R.color.half_transparent_white, null));
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
            output = String.format("%1$02d:%2$tS", minutes, millis);
        } else {
            output = String.format("%1$tS.%1$tL", millis).substring(0, 4);
        }
        return output;
    }
}
