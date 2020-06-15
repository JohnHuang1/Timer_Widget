package com.example.timerwidget;

import androidx.appcompat.app.AppCompatActivity;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class SetTimeActivity extends AppCompatActivity {
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_time);
        Intent intent = getIntent();
        widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        EditText timeEditText = findViewById(R.id.editTextTime);
        timeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence chars, int start, int before, int count) {
                CharSequence[] arr = {"00", "00", "00"};
                int length = chars.length();
                for(int i = 0; i < Math.round(((float)length)/2); i++){
                    int n = i * 2;
                    try{
                        arr[i] = chars.subSequence(length - (n + 2),length - n);
                    } catch(Exception ex){
                        arr[i] = "0" + chars.subSequence(length - (n + 1),length - n);
                        break;
                    }
                }
                ((TextView) findViewById(R.id.hourTextView)).setText(arr[2]);
                ((TextView) findViewById(R.id.minuteTextView)).setText(arr[1]);
                ((TextView) findViewById(R.id.secondTextView)).setText(arr[0]);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public void onConfirmButtonClick(View view){
        EditText editTextTime = findViewById(R.id.editTextTime);
        String userEntry = editTextTime.getText().toString();
        try{
            Integer.parseInt(userEntry);
        } catch(Exception ex){
            Toast.makeText(getApplicationContext(), "Please enter a valid number", Toast.LENGTH_LONG).show();
            return;
        }
        if(!userEntry.equals("")){
            String[] arr = {"0", "0", "0"};
            int length = userEntry.length();
            for(int i = 0; i < Math.round(((float)length)/2); i++){
                int n = i * 2;
                try{
                    arr[i] = userEntry.substring(length - (n + 2),length - n);
                } catch(Exception ex){
                    arr[i] = userEntry.substring(length - (n + 1),length - n);
                    break;
                }
            }
            long entryTime = Integer.parseInt(arr[0]) * 1000 + Integer.parseInt(arr[1]) * 60000 + Integer.parseInt(arr[2]) * 3600000;
            SharedPreferences preferences = getSharedPreferences(getString(R.string.widget_time_pref_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(getString(R.string.pref_time_key) + widgetID, entryTime);
            editor.apply();
            Log.d("onConfirmButtonClick", "entryTime = " + entryTime + " widgetID = " + widgetID);
            Intent updateIntent = new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_RESET).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            sendBroadcast(updateIntent);
            Intent updateTimerIntent = new Intent(TimerService.ACTION_UPDATE_TIMER).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            sendBroadcast(updateTimerIntent);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Please enter a valid number", Toast.LENGTH_LONG).show();
        }
    }

}