package com.example.timerwidget;

import android.os.CountDownTimer;

abstract class PausableCDTimer {
    private CountDownTimer timer = null;
    private long timeLeft = 0;
    private boolean timerRunning = false;
    public int widgetID;

    abstract public void tick(long millisLeft);
    abstract public void finish();

    public PausableCDTimer(int id, long lengthMillis, boolean runAtStart){
        widgetID = id;
        create(lengthMillis, runAtStart);
    }

    private void create(long length, boolean run){
        timeLeft = length;
        timer = new CountDownTimer(length, 1000) {
            @Override
            public void onTick(long l) {
                timeLeft = l;
                tick(timeLeft);
            }

            @Override
            public void onFinish() {
                finish();
            }
        };
        if(run) start();
    }
    public void start(){
        if(!timerRunning){
            timer.start();
            timerRunning = true;
        }
    }
    public void stop(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
        timerRunning = false;
        timeLeft = 0;
    }
    public void pause(){
        if(timerRunning){
            timer.cancel();
            timerRunning = false;
        }
    }
    public void resume(){
        if(!timerRunning){
            create(timeLeft, true);
        }
    }

    public boolean isTimerRunning(){
        return timerRunning;
    }

}
