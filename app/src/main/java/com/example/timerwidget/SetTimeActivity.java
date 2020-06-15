package com.example.timerwidget;

import androidx.appcompat.app.AppCompatActivity;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class SetTimeActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    String timeTypeEntry;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_time);
        Intent intent = getIntent();
        widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.time_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    public void onConfirmButtonClick(View view){
        EditText editTextTime = findViewById(R.id.editTextTime);
        String userEntry = editTextTime.getText().toString();
        if(!userEntry.equals("") && !timeTypeEntry.equals("Select")){
            long entryTime = Long.parseLong(userEntry);
            long multiplier = 1;
            switch(timeTypeEntry){
                case "Seconds": {
                    multiplier = 1000;
                    break;
                }
                case "Minutes":{
                    multiplier = 60000;
                    break;
                }
                case "Hours": {
                    multiplier = 3600000;
                    break;
                }
            }
            SharedPreferences preferences = getSharedPreferences(getString(R.string.widget_time_pref_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            entryTime *= multiplier;
            editor.putLong(getString(R.string.pref_time_key) + widgetID, entryTime);
            editor.apply();
            Log.d("onConfirmButtonClick", "entryTime = " + entryTime + " widgetID = " + widgetID + " timeTypeEntry = " + timeTypeEntry);
            Intent updateIntent = new Intent(getApplicationContext(), TimerWidgetProvider.class).setAction(TimerWidgetProvider.ACTION_RESET).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            sendBroadcast(updateIntent);
            Intent updateTimerIntent = new Intent(TimerService.ACTION_UPDATE_TIMER).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            sendBroadcast(updateTimerIntent);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Please enter a valid number and select a time type.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        timeTypeEntry = adapterView.getItemAtPosition(pos).toString();
        Toast.makeText(getApplicationContext(), "Selected item: " + timeTypeEntry, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}