package com.example.slapplication01;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import android.media.MediaRecorder;

//AWS_S3アップロード関連で必要なライブラリ
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class MainActivity extends AppCompatActivity {
    // 画面オブジェクト
    private TextView mTextViewCountDown;
    private ImageButton mButtonStart;
    private ImageButton mButtonPause;
    private ImageButton getmButtonReset;
    private ImageButton getmButtonFinish;
    private CountDownTimer mCountDownTimer;
    private ImageView GifWave;
    // タイマー関連
    private static final long START_TIME = 10800000;
    //private static final long START_TIME = 10000; // テスト10秒
    private long mTimeLeftInMillis = START_TIME;
    private long mStopTime = 0;
    // 音声ファイル関連
    File audiofile = null;
    final static String FILENAME = "Recorded_Voice.mp3";
    private MediaRecorder recorder = null;
    // 権限関連
    private static int PERMISSIONS_REQUEST_READ_PHONE_STATE = 1;
    // 実行状態
    private enum runState{
        IDLE,     // 起動前
        START,    // 開始
        PAUSE,    // 一時停止
        RESUME,   // 再開
        COMPLETE, // 完了
        RESET     // 中止
    }
    // 状態管理フラグ（起動前で初期化）
    private runState nowStatus = runState.IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();

        // 画面オブジェクトの関連付け
        GifWave = findViewById(R.id.image_view);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStart = findViewById(R.id.imagebutton_restart);
        mButtonPause = findViewById(R.id.imagebutton_pause);
        getmButtonReset = findViewById(R.id.imagebutton_reset);
        getmButtonFinish = findViewById(R.id.imagebutton_finish);

        // 初期表示設定
        updateCountDownText();
        changeButton(nowStatus);

        // イコライザー生成
        ImageView matchImage = findViewById(R.id.image_view);
        GlideDrawableImageViewTarget target = new GlideDrawableImageViewTarget(matchImage);

        // 録音ファイル生成
        audiofile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILENAME);

        // 録音許可申請表示
        ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                PERMISSIONS_REQUEST_READ_PHONE_STATE);

        // クリックイベント：開始 / 再開
        mButtonStart.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                System.out.println("クリックイベント：開始 / 再開");
                System.out.println("nowStatus["+nowStatus+"]");
                if(recorder == null){
                    recorder = new MediaRecorder();
                    // イコライザー画像設定（初期処理で設定すると一瞬表示されるため）
                    Glide.with(context).load(R.drawable.wave_minion).into(target);
                }
                startPause();
            }
        });

        // クリックイベント：一時停止
        mButtonPause.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                System.out.println("クリックイベント：一時停止");
                System.out.println("nowStatus["+nowStatus+"]");
                startPause();
            }
        });

        // クリックイベント：完了
        getmButtonFinish.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.out.println("クリックイベント：完了");
                System.out.println("nowStatus["+nowStatus+"]");
                // タイマー停止
                mCountDownTimer.cancel();
                // 録音停止
                recorder.stop();
                recorder.release();
                recorder = null;
                nowStatus = runState.COMPLETE;
                // 転送前処理
                preUpload();
                // S3に録音ファイルを転送
                uploadToS3(FILENAME, audiofile.getAbsolutePath());
                // 転送後処理
                // S3転送のコールバック内で実行
            }
        });

        // クリックイベント：中止
        getmButtonReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                System.out.println("クリックイベント：中止");
                System.out.println("nowStatus["+nowStatus+"]");
                // タイマー停止
                mCountDownTimer.cancel();
                resetTimer();
                // 録音停止
                recorder.stop();
                recorder.release();
                recorder = null;
                nowStatus = runState.RESET;
                // ボタン表示変更
                changeButton(nowStatus);
                nowStatus = runState.IDLE;
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startPause(){
        // 実行状態
        switch (nowStatus){
            // 起動前、完了、中止の場合
            case IDLE:
            case COMPLETE:
            case RESET:
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile(audiofile.getAbsolutePath());
                System.out.println("パス["+audiofile.getAbsolutePath()+"]");
                // 録音準備
                try {
                    recorder.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 録音開始
                recorder.start();
                nowStatus = runState.START;
                // タイマー開始
                startTimer();
                // ボタン表示変更
                changeButton(nowStatus);
                break;
            // 開始、再開の場合
            case START:
            case RESUME:
                // 録音一時停止
                recorder.pause();
                nowStatus = runState.PAUSE;
                // タイマー一時停止
                pauseTimer();
                // ボタン表示変更
                changeButton(nowStatus);
                break;
            // 一時停止の場合
            case PAUSE:
                // 録音再開
                recorder.resume();
                nowStatus = runState.RESUME;
                // タイマー再開
                resumeTimer();
                // ボタン表示変更
                changeButton(nowStatus);
                break;
            default:
                break;
        }
    }
    //--------------------
    // タイマー制御
    //--------------------
    // タイマー開始処理
    private void startTimer(){
        // タイマー生成
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                // 録音停止
                recorder.stop();
                recorder.release();
                recorder = null;
                nowStatus = runState.IDLE;
                // 転送前処理
                preUpload();
                // S3に録音ファイルを転送
                uploadToS3(FILENAME, audiofile.getAbsolutePath());
                // 転送後処理
                // S3転送のコールバック内で実行
            }
        }.start();
    }
    // タイマー一時停止処理
    private void pauseTimer(){
        mStopTime = mTimeLeftInMillis;
        mCountDownTimer.cancel();
    }
    // タイマー再開処理
    private void resumeTimer(){
        mTimeLeftInMillis = mStopTime;
        updateCountDownText();
        startTimer();
    }
    // タイマーリセット処理
    private void resetTimer(){
        mTimeLeftInMillis = START_TIME;
        mStopTime = 0;
        updateCountDownText();
    }
    // タイマー表示更新処理
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
    //--------------------
    // ボタン表示制御
    //--------------------
    // ボタン表示変更処理
    private void changeButton(runState p_nowStatus){
        // 実行状態
        switch (p_nowStatus){
            //　起動前、完了、中止の場合
            case IDLE:
            case COMPLETE:
            case RESET:
                GifWave.setVisibility(View.INVISIBLE);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonPause.setVisibility(View.INVISIBLE);
                getmButtonReset.setVisibility(View.INVISIBLE);
                getmButtonFinish.setVisibility(View.INVISIBLE);
                break;
            // 開始、再開の場合
            case START:
            case RESUME:
                GifWave.setVisibility(View.VISIBLE);
                mButtonStart.setVisibility(View.INVISIBLE);
                mButtonPause.setVisibility(View.VISIBLE);
                getmButtonReset.setVisibility(View.VISIBLE);
                getmButtonFinish.setVisibility(View.VISIBLE);
                break;
            // 一時停止の場合
            case PAUSE:
                GifWave.setVisibility(View.INVISIBLE);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonPause.setVisibility(View.INVISIBLE);
                getmButtonReset.setVisibility(View.VISIBLE);
                getmButtonFinish.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
    //--------------------
    // ダイアログ表示
    //--------------------
    public void showDialog( Context p_context, String p_strMessage, int p_intY, int p_intTime) {
        // ダイアログ作成
        AlertDialog.Builder builder = new AlertDialog.Builder(p_context);
//            builder.setPositiveButton("OK", null);    // （参考）コールバックなし
//            builder.setNegativeButton("NG", null);    // （参考）同上
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setMessage(p_strMessage);
        // 表示カスタマイズ
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.CENTER;
        wmlp.x = 0;      // x位置
        wmlp.y = p_intY; // y位置
        // 自動消去設定
        Handler handler = new Handler();
        Runnable dialogDismiss = new Runnable() {
            public void run() {
                // ダイアログ削除
                dialog.dismiss();
            }
        };
        // ダイアログ表示
        dialog.show();
        handler.postDelayed(dialogDismiss, p_intTime);
    }
    //--------------------
    // ファイル転送制御
    //--------------------
    // 転送前処理
    private void preUpload(){
        // ボタン非活性
        getmButtonReset.setEnabled(false);
        getmButtonFinish.setEnabled(false);
        // メッセージ表示
        showDialog(MainActivity.this,
                "録音終了\n文字起こしを開始しました\n音声ファイル転送中・・・",
                300, 4000);
    }
    // 転送後処理
    private void postUpload(){
        // タイマー表示初期化
        resetTimer();
        // ボタン活性
        getmButtonReset.setEnabled(true);
        getmButtonFinish.setEnabled(true);
        // ボタン表示変更
        changeButton(nowStatus);
        nowStatus = runState.IDLE;
    }
    // 録音ファイル転送処理
    private void uploadToS3(String p_fileName, String p_path){
        // S3関連の設定
        // バケット名作成
        String bucket = "slaplication01-in";
        // キー作成（ログイン画面のユーザーID＋録音音声ファイル名）
        String mUserID = getIntent().getStringExtra("UserID");
        String key = mUserID + "/" + p_fileName;

        // Amazon Cognito 認証情報プロバイダーを初期化
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-northeast-1:2e5a8f36-24bb-400c-a102-75765164a6d9", // ID プールの ID
                Regions.AP_NORTHEAST_1 // リージョン
            );

        // 作成した認証情報でクライアント接続用オブジェクトを作成
        AmazonS3Client s3Client = new AmazonS3Client(credentialsProvider);
        TransferUtility transferUtility = new TransferUtility(s3Client, getApplicationContext());

        // ファイルを指定してアップロードを行う
        TransferObserver observer = transferUtility.upload(
                bucket,
                key,
                new java.io.File(p_path));

        // コールバック登録
        observer.setTransferListener(new TransferListener() {
            // 完了時
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d("uploadToS3", "status: " + state);
                if(state.toString().equals("COMPLETED")){
                    // メッセージ表示
                    showDialog(MainActivity.this,
                            "音声ファイル転送完了\n文字起こし完了メールをお待ちください",
                            600, 5000);
                    // 転送後処理
                    postUpload();
                }
            }
            // 転送中
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d("uploadToS3",
                        "progress: "+id+" bytesCurrent:"+bytesCurrent+" bytesTotal:"+bytesTotal);
            }
            // 異常終了
            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
                // メッセージ表示
                showDialog(MainActivity.this,
                        "音声ファイル転送失敗",
                        600, 5000);
                // 転送後処理
                postUpload();
            }
        });
    }
    @Override
    public void onBackPressed() {
        if(nowStatus != runState.IDLE){
            // メッセージ表示
            showDialog(MainActivity.this,
                    "録音操作中は戻れません\n完了もしくはリセットしてください",
                    300, 3000);
        }
        else {
            // ログイン画面に戻る
            finish();
        }
    }
}