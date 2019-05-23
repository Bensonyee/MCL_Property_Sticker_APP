package com.mcl.propertysticker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This sample performs continuous scanning, displaying the barcode and source image whenever
 * a barcode is scanned.
 */
public class ContinuousCaptureActivity extends Activity {
    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String lastText;
    private boolean flag = true;

    private String GetPropertyInfoURL = "http://140.115.26.39:8880/api/get_property";
    private String PropertyCheckURL = "http://140.115.26.39:8880/api/stick";

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            //barcodeView.setStatusText(result.getText());
            requestPropertyInfo(result.getText());

            beepManager.playBeepSoundAndVibrate();

            //Added preview of scanned barcode
            ImageView imageView = (ImageView) findViewById(R.id.barcodePreview);
            imageView.setImageBitmap(result.getBitmapWithResultPoints(Color.YELLOW));
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_continuous_capture);

        getPermissionCamera();//check permission first




        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);

        beepManager = new BeepManager(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WelcomeDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    /*
    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }
    */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }


    public void SwitchLight(View v) {
        //requestProperty("314010185");
        //requestPropertyInfo("3140101-03-25177");
        //requestPropertyCheck("5010106-03-1078","it should be error");
        if (flag) {
            flag = false;
            // open flashlight
            barcodeView.setTorchOn();
        }else {
            flag = true;
            // close flashlight
            barcodeView.setTorchOff();
        }
    }

    public void getPermissionCamera(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
    }


    private void requestPropertyInfo(final String property_id) {
        final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("載入中...");
        pDialog.setCancelable(false);
        pDialog.show();


        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        OkHttpClient mOkHttpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("property_id",property_id)
                .add("token",globalVariable.Token)
                .build();
        Request request = new Request.Builder()
                .url(GetPropertyInfoURL)
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
                            FailDialog("回應格式錯誤","請確認網路連線及伺服器狀態");
                        }
                    });
                }
                else{
                    try{
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final String status = jsonResponse.getString("status");

                        if(status.equals("success")){
                            int confirmed = jsonResponse.getInt("confirmed");
                            final String property_name = jsonResponse.getString("name");

                            if(confirmed==0){ //can be check
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        PropertyInfoDialog(property_name,property_id);
                                        //Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else{ //has been checked
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        WarningDialog("真可惜","此財產:\n"+property_id+"\n"+property_name+"\n已被確認過");
                                        //Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                        else if(status.equals("failed")){
                            int error_type = jsonResponse.getInt("error type");
                            if(error_type == 1) { //token error
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FailDialog("錯誤","使用者代碼錯誤");
                                    }
                                });
                            }
                            else if(error_type == 2) { //property not found
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FailDialog("錯誤","查無此財產:\n"+property_id);
                                    }
                                });
                            }
                        }

                    } catch (JSONException e){}
                }
            }
        });
    }


    private void requestPropertyCheck(String property_id,String note) {
        final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("載入中...");
        pDialog.setCancelable(false);
        pDialog.show();


        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        OkHttpClient mOkHttpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("property_id",property_id)
                .add("note",note)
                .add("token",globalVariable.Token)
                .build();
        Request request = new Request.Builder()
                .url(PropertyCheckURL)
                .post(formBody)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                pDialog.dismiss();
//                Log.i("error", "fail");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FailDialog("連線失敗","請確認網路連線及伺服器狀態");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                pDialog.dismiss();
                if(response.code()!=200){
                    String responseData = response.body().string();
                    Log.i("error",responseData);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FailDialog("回應格式錯誤","請確認網路連線及伺服器狀態");
                        }
                    });
                }
                else{
                    try{
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        final String status = jsonResponse.getString("status");

                        if(status.equals("success")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SuccessDialog();
                                    //Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else if(status.equals("failed")){
                            Log.i("error",responseData);
                            int error_type = jsonResponse.getInt("error type");
                            if(error_type==1){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FailDialog("錯誤","使用者代碼錯誤");
                                    }
                                });
                            }
                            else if(error_type==2){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FailDialog("錯誤","查無此財產");
                                    }
                                });
                            }
                            else if(error_type==3){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        FailDialog("錯誤","此財產已被標記");
                                    }
                                });
                            }

                        }
                    } catch (JSONException e){}
                }
            }
        });
    }


    private void PropertyInfoDialog(String property_name, final String property_id) {
        final EditText editText = new EditText(this);
        final TextView textView = new TextView(this);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(30, 20, 30, 30);

        editText.setHint("留點註記吧(可留空)");
        textView.setText("財產編號: "+property_id+"\n財產名稱: "+property_name);
        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(textView,layoutParams);
        linearLayout.addView(editText);


        SweetAlertDialog dialog = new SweetAlertDialog(this);
        dialog  .setTitleText("財產資訊")
                .setCustomView(linearLayout)
                .setConfirmText("確認")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        String note = editText.getText().toString();
                        requestPropertyCheck(property_id,note);
                        sDialog.dismissWithAnimation();
                    }
                })
                .setCancelButton("取消", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                });
        dialog.show();

    }



    public void HandInputIdDialog(View view){
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("ex:3140101-03-19943");

        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(input);


        SweetAlertDialog dialog = new SweetAlertDialog(this);
        dialog  .setTitleText("輸入財產編號")
                .setCustomView(linearLayout)
                .setConfirmText("查詢")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        String property_id = input.getText().toString();
                        requestPropertyInfo(property_id);
                        sDialog.dismissWithAnimation();
                    }
                })
                .setCancelButton("取消", new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.dismissWithAnimation();
                    }
                });
        dialog.show();

    }

    public void SuccessDialog(){
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("標記成功")
                .setContentText("恭喜你!")
                .setConfirmText("耶嘿")
                .show();
    }
    private void FailDialog(String title,String msg){
        new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(title)
                .setContentText(msg)
                .setConfirmText("好")
                .show();

    }

    private void WarningDialog(String title,String msg){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(title)
                .setContentText(msg)
                .setConfirmText("好吧QAQ")
                .show();
    }

    private void WelcomeDialog(){
        GlobalVariable globalVariable = (GlobalVariable)getApplicationContext();
        new SweetAlertDialog(this)
                .setTitleText("上工囉")
                .setContentText(globalVariable.UserName+"，歡迎回來!\n一起朝著財產貼大師之路前進吧!")
                .setConfirmText("好耶")
                .show();
    }


}