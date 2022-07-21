package com.example.slapplication01;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends AppCompatActivity {
    // 画面オブジェクト
    private EditText mTextViewUserID;
    private EditText mTextViewPassword;
    private Button mButtonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
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
                // ユーザー認証処理
                boolean blRet = Func_Login(strUserID, strPassword);
                // ログイン成功
                if(blRet){
                    //インテントオブジェクトを生成
                    Intent newIntent = new Intent(context, MainActivity.class);
                    //追加画面の起動
                    startActivity(newIntent);
                }
                // ログイン失敗
                else{
                    // メッセージ表示
                    // ユーザーIDまたはパスワードが誤っています
                }
            }
        });
    }
    //--------------------
    // ログイン制御
    //--------------------
    // ユーザー認証処理
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean Func_Login(String p_strUserID, String p_strPassword){
        boolean blRet = true;   // ユーザー認証結果

        return blRet;
    }
}