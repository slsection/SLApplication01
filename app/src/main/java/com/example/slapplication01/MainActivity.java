package com.example.slapplication01;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
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

//AWS_S3アップロード関連で必要なライブラリ
import android.util.Log;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;

public class MainActivity extends AppCompatActivity {

    private static final long START_TIME = 10800000;

    private  TextView mTextViewCountDown;
    private  Button mButtonStartPause;
    private  Button getmButtonReset;
    private  Button getmButtonFinish;
    private  Button getmButtonTran;
    private  CountDownTimer mCountDownTimer;
    private  boolean mTimerRunning;
    private  boolean mIsPause;
    private long mTimeLeftInMillis = START_TIME;
    private long mStopTime = 0;
    private static int PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;

    File audiofile = null;

    final static String FILENAME = "test.mp3";
    private MediaRecorder recorder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        getmButtonReset = findViewById(R.id.buttonreset);
        getmButtonFinish = findViewById(R.id.finish);
        getmButtonTran = findViewById(R.id.button_tran);

        getmButtonReset.setVisibility(View.INVISIBLE);
        getmButtonFinish.setVisibility(View.INVISIBLE);
        getmButtonTran.setVisibility(View.INVISIBLE);

        Context context = getApplicationContext();
        audiofile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILENAME);

        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSIONS_REQUEST_READ_PHONE_STATE);

        recorder = new MediaRecorder();

        // クリックイベント：開始 or 一時停止
        mButtonStartPause.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                System.out.println("mTimerRunningの値は？ " +mTimerRunning);
                if (mTimerRunning) {
                    // タイマーが動いてたら
                    // 録音一時停止
                    recorder.pause();
                    pauseTimer();
                    mIsPause = true;
                } else {
                    // タイマーが動いてなかったら
                    if (mIsPause){
                        // 一時停止中の場合
                        // 録音再開
                        recorder.resume();
                        resumeTimer();
                        mIsPause = false;
                    }else{
                        // 初回の場合
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        recorder.setOutputFile(audiofile.getAbsolutePath());
                        System.out.println("パス " +audiofile.getAbsolutePath());
                        // 録音準備
                        try {
                            recorder.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 録音開始
                        recorder.start();

                        // startTimer()の中で録音開始
                        startTimer();
                    }
                }
            }
        });

        // クリックイベント：停止
        getmButtonFinish.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.out.println("ストップボタン" +mTimerRunning);
                if (mTimerRunning) {
                    // タイマーが動いてたら
                    mCountDownTimer.cancel();
                    mTimerRunning = false;
                    // 録音停止
                    recorder.stop();
                    recorder.release();
                    recorder = null;
//                    // S3に録音ファイルを転送
//                    uploadToS3(FILENAME, audiofile.getAbsolutePath());
//                    //
//                    mTimeLeftInMillis = START_TIME;
//                    updateCountDownText();
//                    mButtonStartPause.setText("START");
//                    mButtonStartPause.setVisibility(View.VISIBLE);
//                    getmButtonReset.setVisibility(View.INVISIBLE);
//                    getmButtonFinish.setVisibility(View.INVISIBLE);

                    getmButtonTran.setVisibility(View.VISIBLE);
                } else {
                    // タイマーが動いてなかったら
                    // ・・・
                }
            }
        });

        // クリックイベント：文字起こし
        getmButtonTran.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // S3に録音ファイルを転送
                uploadToS3(FILENAME, audiofile.getAbsolutePath());
                // 画面表示更新
                mTimeLeftInMillis = START_TIME;
                updateCountDownText();
                mButtonStartPause.setText("START");
                mButtonStartPause.setVisibility(View.VISIBLE);
                getmButtonReset.setVisibility(View.INVISIBLE);
                getmButtonFinish.setVisibility(View.INVISIBLE);
                getmButtonTran.setVisibility(View.INVISIBLE);
            }
        });

        // クリックイベント：リセット
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
        mStopTime = mTimeLeftInMillis;
        mCountDownTimer.cancel();
        mTimerRunning = false;
        mButtonStartPause.setText("START");
        getmButtonReset.setVisibility(View.VISIBLE);
    }
    private void resumeTimer(){
        mTimeLeftInMillis = mStopTime;
        updateCountDownText();
        mCountDownTimer.start();
        mTimerRunning = true;
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
        getmButtonTran.setVisibility(View.INVISIBLE);
    }

    private void updateCountDownText(){
        int hour    = (int)(mTimeLeftInMillis/1000)/60/60;
        int minutes = (int)(mTimeLeftInMillis/1000)/60%60;
        int seconds = (int)(mTimeLeftInMillis/1000)%60;
        String timerLeftFormatted = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                hour,
                minutes,
                seconds);
        mTextViewCountDown.setText(timerLeftFormatted);
    }

    // 録音ファイル転送処理
    private void uploadToS3(String p_fileName, String p_path){
        // S3関連の設定
        String accessKey = "******";                // アクセスキー
        String secKey = "******";                   // シークレットキー
        String bucket = "slaplication01-in";        // バケット名

        // 認証情報の作成
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secKey);

        // 作成した認証情報でクライアント接続用オブジェクトを作成
        AmazonS3Client s3Client = new AmazonS3Client(basicAWSCredentials);
        TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());

        // ファイルを指定してアップロードを行う
        TransferObserver observer = transferUtility.upload(
                bucket,
                p_fileName,
                new java.io.File(p_path));

        // コールバック登録
        observer.setTransferListener(new TransferListener() {
            // 完了時
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d("uploadToS3", "status: " + state);
            }
            // 転送中
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("uploadToS3", "progress: "+id+" bytesCurrent:"+bytesCurrent+" bytesTotal:"+bytesTotal);
            }
            // 異常終了
            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}