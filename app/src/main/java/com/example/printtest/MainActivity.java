package com.example.printtest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.printtest.btprint.PrinterUtils;
import com.example.printtest.btprint.Utils;
import com.example.printtest.printer.UsbPrinterActivity;
import com.telpo.tps550.api.printer.UsbThermalPrinter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.List;

import android.util.Base64;

public class MainActivity extends SuperActivity {
    private Button btnTest, btnLoad, btnTelpos, btnTelproTest;
    private ImageView imgTest, imgResult, ImgQr;
    private LinearLayout lnrView;
    private Bitmap bitImag = null;
    UsbThermalPrinter mUsbThermalPrinter = new UsbThermalPrinter(MainActivity.this);
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();



    }

    private void initViews() {
        imgTest = findViewById(R.id.img_test);
        lnrView = findViewById(R.id.inr_view);
        imgResult = findViewById(R.id.img_result);
        ImgQr = findViewById(R.id.img_qr);

        btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(clickMain);

        btnLoad = findViewById(R.id.btn_load);
        btnLoad.setOnClickListener(clickMain);

        btnTelpos = findViewById(R.id.btn_telpos);
        btnTelpos.setOnClickListener(clickMain);

        btnTelproTest = findViewById(R.id.btn_telpos_test);
        btnTelproTest.setOnClickListener(clickMain);

        StringBuilder sb = new StringBuilder();
        sb.append(" المنال ويتلذذ بالآلام، الألم هو الألم ولكن نتيجة لظروف" + "\n\n" + " المنال ويتلذذ بالآلام، الألم هو الألم ولكن نتيجة لظروف");


        Bitmap bitmap = drawText(sb.toString(), 1000, 40);

        imgTest.setImageBitmap(bitmap);

        Bitmap qr = generateQrCode("Ajmal");
        ImgQr.setImageBitmap(qr);


        loadData();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "ajmal");
            jsonObject.put("adssad", "adasd");
            jsonObject.put("adsssadaad", "asdsa");
            jsonObject.put("33", 444);
            jsonObject.put("344", 77.9);
//            String s = encodeBase64(jsonObject.toString().getBytes(StandardCharsets.UTF_8));

            encodeJsonToBase64(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    int[] colors = new int[2];

    private void loadData() {

        LinearLayout linLayout = (LinearLayout) findViewById(R.id.lnr_data_view);
        LayoutInflater ltInflater = getLayoutInflater();


        colors[0] = Color.parseColor("#559966CC");
        colors[1] = Color.parseColor("#55336699");
        for (int i = 0; i < 2; i++) {


            View item = ltInflater.inflate(R.layout.layout_cart_list, linLayout, false);
            TextView tvName = (TextView) item.findViewById(R.id.txt_name);
            tvName.setText("trrt  " + i);
            TextView tvPosition = (TextView) item.findViewById(R.id.txt_ar_name);
            tvPosition.setText("Должность: " + i);
            item.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
            item.setBackgroundColor(colors[i % 2]);
            linLayout.addView(item);
        }

    }


    public final View.OnClickListener clickMain = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {

                case R.id.btn_test: {

                    if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {


                        PrinterUtils.getInstance().startPrint(bitImag);
                    } else {

                        reSetPrinter(_callback);
                    }

                    break;
                }
                case R.id.btn_load: {

                    bitImag = getBitmapFromView(lnrView);

                    float aspectRatio = bitImag.getWidth() /
                            (float) bitImag.getHeight();
                    int width = 480;
                    int height = Math.round(width / aspectRatio);

                    imgResult.setImageBitmap(Bitmap.createScaledBitmap(bitImag, 480, height, false));


                    break;
                }
                case R.id.btn_telpos: {

                    Intent i = new Intent(getApplicationContext(), UsbPrinterActivity.class);
                    startActivity(i);

                    break;
                }
                case R.id.btn_telpos_test: {

                    new contentPrintThread().start();
                    break;
                }

                default: {

                }
            }
        }
    };

    private class contentPrintThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
//                Bitmap bitmap=getBitmapFromView(ImgQr);
//                byte[] by= Utils.decodeBitmap(bitmap);
                mUsbThermalPrinter.addString("ajmal");
//                mUsbThermalPrinter.printLogoRaw(by,10,10);
                bitImag = getBitmapFromView(lnrView);

                float aspectRatio = bitImag.getWidth() /
                        (float) bitImag.getHeight();
                int width = 380;
                int height = Math.round(width / aspectRatio);
                Bitmap b = Bitmap.createScaledBitmap(bitImag, 380, height, false);
                mUsbThermalPrinter.printLogo(b, true);
                mUsbThermalPrinter.printString();


            } catch (Exception e) {
                e.printStackTrace();
//                Result = e.toString();
//                if (Result.equals("com.telpo.tps550.api.printer.NoPaperException")) {
//                    nopaper = true;
//                } else if (Result.equals("com.telpo.tps550.api.printer.OverHeatException")) {
//                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
//                } else {
//                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
//                }
            } finally {
//                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
//                if (nopaper){
//                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
//                    nopaper = false;
//                    return;
//                }
            }
        }
    }


    private final CallBack _callback = new CallBack() {
        @Override
        public void onSuccess(int serviceId, FormRes res) {

            if (serviceId == RE_PRINT_SERVICE_ID) {
                if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {

                    PrinterUtils.getInstance().startPrint(bitImag);
                }
            }

        }

        @Override
        public void onError(int serviceId, String errorMessage) {

        }
    };

/*
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String encodeBase64(byte[] encodeMe) {

        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] encodedBytes = Base64.getEncoder().encode(encodeMe);
        Log.e(TAG, "--            " + new String(encodedBytes));
        return new String(encodedBytes);
    }

 */

    private String encodeJsonToBase64(byte[] encodeMe) {
        String base64=null;
        try {
            base64  =  Base64.encodeToString(encodeMe, Base64.DEFAULT);

            Log.e(TAG, "--            " +base64);
        } catch (Exception e) {

        }
        return base64;
    }



}