package com.example.slapplication01;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import android.media.MediaRecorder;

public class MainActivity extends AppCompatActivity {

    private static final long START_TIME = 180000;

    private  TextView mTextViewCountDown;
    private  Button mButtonStartPause;
    private  Button getmButtonReset;
    private  Button getmButtonFinish;
    private  CountDownTimer mCountDownTimer;
    private  boolean mTimerRunning;
    private long mTimeLeftInMillis = START_TIME;
    private static int PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;

    File audiofile = null;

    final static String FILENAME = "test.mp3";
    MediaRecorder recorder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        getmButtonReset = findViewById(R.id.buttonreset);
        getmButtonFinish = findViewById(R.id.finish);

        getmButtonReset.setVisibility(View.INVISIBLE);
        getmButtonFinish.setVisibility(View.INVISIBLE);

        Context context = getApplicationContext();
        audiofile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILENAME);

        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSIONS_REQUEST_READ_PHONE_STATE);

        recorder = new MediaRecorder();


        mButtonStartPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.out.println("mTimerRunningの値は？ " +mTimerRunning);
                if (mTimerRunning) {
                    recorder.stop();
                    recorder.release();
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        getmButtonFinish.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.out.println("ストップボタン" +mTimerRunning);
                if (mTimerRunning) {
                    recorder.stop();
                    recorder.release();
                    mCountDownTimer.cancel();
                    mTimerRunning = false;
                    mTimeLeftInMillis = START_TIME;
                    updateCountDownText();
                    mButtonStartPause.setText("START");
                    mButtonStartPause.setVisibility(View.VISIBLE);
                    getmButtonReset.setVisibility(View.INVISIBLE);
                    getmButtonFinish.setVisibility(View.INVISIBLE);
                } else {

                }
            }
        });

        getmButtonReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                resetTimer();
            }
        });
    }


    private void startTimer(){
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();

                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile(audiofile.getAbsolutePath());
                System.out.println("パス " +audiofile.getAbsolutePath());
                try {
                    recorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recorder.start();

                getmButtonReset.setVisibility(View.VISIBLE);
                getmButtonFinish.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mButtonStartPause.setText("START");
                getmButtonReset.setVisibility(View.INVISIBLE);
            }
        }.start();

        mTimerRunning = true;
        mButtonStartPause.setText("一時停止");
        getmButtonReset.setVisibility(View.INVISIBLE);
    }

    private void pauseTimer(){
        mCountDownTimer.cancel();
        mTimerRunning = false;
        mButtonStartPause.setText("START");
        getmButtonReset.setVisibility(View.VISIBLE);
    }

    private void resetTimer(){
        mCountDownTimer.cancel();
        mTimeLeftInMillis = START_TIME;
        updateCountDownText();
        mButtonStartPause.setText("START");
        mButtonStartPause.setVisibility(View.VISIBLE);
        getmButtonReset.setVisibility(View.INVISIBLE);
        getmButtonFinish.setVisibility(View.INVISIBLE);
    }

    private void updateCountDownText(){
        int minutes = (int)(mTimeLeftInMillis/1000)/60;
        int seconds = (int)(mTimeLeftInMillis/1000)%60;
        String timerLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        mTextViewCountDown.setText(timerLeftFormatted);
    }
}