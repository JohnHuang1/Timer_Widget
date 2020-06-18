package com.example.timerwidget;

import android.os.CountDownTimer;

abstract class PausableCDTimer {
    private CountDownTimer timer = null;
    private long defaultLength;
    private long timeLeft = 0;
    private boolean timerRunning = false;
    public int widgetID;

    abstract public void tick(long millisLeft);
    abstract public void finish();

    public PausableCDTimer(int id, long lengthMillis, boolean runAtStart){
        widgetID = id;
        defaultLength = lengthMillis;
        create(lengthMillis, runAtStart);
    }

    private void create(long length, boolean runAtStart){
        timeLeft = length;
        timer = new CountDownTimer(length, 100) {
            @Override
            public void onTick(long l) {
                timeLeft = l;
                tick(timeLeft);
            }

            @Override
            public void onFinish() {
                finish();
                timerRunning = false;
            }
        };
        if(runAtStart) start();
    }
    public void start(){
        if(!timerRunning){
            if(timer != null){
                timer.start();
            } else {
                create(timeLeft, true);
            }
            timerRunning = true;
        }
    }
    public void stop(){
        if(timer != null){
            timer.cancel();
            create(defaultLength, false);
        }
        timerRunning = false;
        timeLeft = defaultLength;
    }
    public void pause(){
        if(timerRunning){
            timer.cancel();
            create(timeLeft, false);
            timerRunning = false;
        }
    }
    public boolean isTimerRunning(){
        return timerRunning;
    }

}
