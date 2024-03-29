package com.example.slapplication01;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {
    // 画面オブジェクト
    private EditText mTextViewUserID;
    private EditText mTextViewPassword;
    private Button mButtonLogin;
    // メッセージ表示用
    private MainActivity objMainActivity = new MainActivity();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Context context = getApplicationContext();

        // 画面オブジェクトの関連付け
        mTextViewUserID = findViewById(R.id.editTextTextUserID);
        mTextViewPassword = findViewById(R.id.editTextTextPassword);
        mButtonLogin = findViewById(R.id.Login_button);

        // クリックイベント：ログイン
        mButtonLogin.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                String strUserID = mTextViewUserID.getText().toString();    // ユーザーID
                String strPassword = mTextViewPassword.getText().toString();// パスワード

                // 入力必須チェック
                if(strUserID.isEmpty() || strPassword.isEmpty()){
                    // メッセージ表示
                    objMainActivity.showDialog(StartActivity.this,
                            "ユーザーID・パスワードを\n入力してください",
                            300, 4000);
                    return;
                }

                // ユーザー認証処理
                boolean blRet = userAuthentication(strUserID, strPassword);
                // ログイン成功
                if(blRet){
                    // メッセージ表示
                    Toast.makeText(StartActivity.this,
                            "ログイン成功",
                            Toast.LENGTH_LONG).show();
                    //インテントオブジェクトを生成
                    Intent Intent = new Intent(context, MainActivity.class);
                    // ユーザーIDを退避(MainActivityに連携)
                    Intent.putExtra("UserID", strUserID);
                    //追加画面の起動
                    startActivity(Intent);
                }
                // ログイン失敗
                else{
                    // メッセージ表示
                    objMainActivity.showDialog(StartActivity.this,
                            "ログイン失敗\nユーザーIDまたはパスワードが誤っています",
                            300, 4000);
                }
            }
        });
    }
    //--------------------
    // ログイン制御
    //--------------------
    // ユーザー認証処理
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean userAuthentication(String p_strUserID, String p_strPassword){
        boolean blRet = true;   // ユーザー認証結果

        // API Gatewayコール

        return blRet;
    }
}