package com.mcl.propertysticker;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cn.pedant.SweetAlert.SweetAlertDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity
{
    private IntentIntegrator scanIntegrator;
    private String TokenCheckURL="http://140.115.26.39:8880/api/token_check";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private void startScan(){
        scanIntegrator = new IntentIntegrator(MainActivity.this);
        scanIntegrator.setCaptureActivity(ContinuousCaptureActivity.class);
        scanIntegrator.setDesiredBarcodeFormats(IntentIntegrator.CODE_39);
        scanIntegrator.setPrompt("請將財產貼條碼完整置入掃描框");
        scanIntegrator.setTimeout(300000);
        scanIntegrator.setOrientationLocked(false);
        scanIntegrator.initiateScan();
        MainActivity.this.finish();
    }

    public void login_onclick(View v){
        final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("登入中...");
        pDialog.setCancelable(false);
        pDialog.show();


        EditText et_token = (EditText) findViewById(R.id.et_token);
        final String input_token = et_token.getText().toString();

        OkHttpClient mOkHttpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("token",input_token)
                .build();
        Request request = new Request.Builder()
                .url(TokenCheckURL)
                .post(formBody)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Log.i("error", "fail");
                pDialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "連線失敗", Toast.LENGTH_SHORT).show();
                        FailDialog("連線失敗","請確認網路連線及伺服器狀態");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                pDialog.dismiss();
                if(response.code()!=200){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(getApplicationContext(), "回應格式錯誤,請確認網路連線及伺服器狀態", Toast.LENGTH_SHORT).show();
                            FailDialog("有點不對勁呢~!","回應格式錯誤,請確認網路連線及伺服器狀態");
                        }
                    });
                }
                else{
                    try{
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final String status = jsonResponse.getString("status");
                        //Log.i("response",responseData);
                        if(status.equals("success")){
                            GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
                            globalVariable.Token = input_token;
                            final String username = jsonResponse.getString("user");
                            globalVariable.UserName = username;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Toast.makeText(getApplicationContext(), "你好，"+username, Toast.LENGTH_SHORT).show();
                                    startScan();
                                }
                            });
                        }
                        else if(status.equals("failed")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Toast.makeText(getApplicationContext(), "登入失敗，無此使用者", Toast.LENGTH_SHORT).show();
                                    FailDialog("你484壞人!","登入失敗，無此使用者");
                                }
                            });
                        }
                    } catch (JSONException e){}
                }
            }
        });
    }

    private void FailDialog(String title,String msg){
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(title)
                .setContentText(msg)
                .setConfirmText("我再試試QQ")
                .show();

    }




}