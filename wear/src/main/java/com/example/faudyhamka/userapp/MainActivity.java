package com.example.faudyhamka.userapp;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private TextView timeStampTxt, hrTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //1. Pengaturan layout wear
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                //* 3. Menampilkan data sensor heartrate sebagai TextView Wear
                timeStampTxt = (TextView) stub.findViewById(R.id.timeStampTxt);
                hrTxt = (TextView) stub.findViewById(R.id.hrTxt);
            }
        });

        this.registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));

    }

    // *2. Pemanggilan hasil data sensor Accelerometer dan Heartrate yang terdeteksi di SensorService menggunakan Broadcast Receiver
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Get timestamp
            long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
            timeStampTxt.setText(String.valueOf(getDate()));

            //Get accuracy
            int message2 = intent.getIntExtra("ACCR", 0);

            // Extract data included in the Intent
            float[] message1 = intent.getFloatArrayExtra("HR");
            Log.d("Receiver", "Got message1: " + message1[0] + "Got message2: " + message2);
            int tmpHr = (int)Math.ceil(message1[0] - 0.5f);
            hrTxt.setText(String.valueOf(tmpHr));

        }
    };


    @Override
    public void onResume() {
        super.onResume();
        // Register mMessageReceiver to receive messages.
        this.registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));

    }

    //4. Menggunakan fungsi GetDate untuk mendapatkan timestamp dari data sensor yang didapat dan menampilkannya sebagai TextView
    private String getDate(){

        try{
            DateFormat sdf = new SimpleDateFormat("HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            Date netDate = (new Date());
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "7:00";
        }
    }
}
