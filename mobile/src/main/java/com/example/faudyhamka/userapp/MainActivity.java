package com.example.faudyhamka.userapp;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.example.faudyhamka.userapp.database.DatabaseHandler;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    public static final String myPreference = "my_pref";
    public static final String mypreference = "mypref";
    public static final String inputIP = "input_IP";
    private static RemoteSensorManager remoteSensorManager;
    TextView hrTxt;
    TextView falldetectionTxt;
    TextView lastSyncTxt;
    Switch mSwitch;
    long lastMeasurementTime = 0L;
    boolean isRunning = false;
    boolean isStop = false;
    boolean fallconfirmation = false;
    int stateactivity;
    Calendar calendar;
    String Timestamp, Timestamp2, Timestamp3;
    SimpleDateFormat simpleDateFormat, simpleDateFormat2, simpleDateFormat3;

    RequestQueue queue;
    Configuration conf = new Configuration();
    SharedPreferences sharedpreferences, sharedPreferences;

    //Save age for heartrate processing
    public int ageValue2;
    public String eN1, eN2, eN3, eN4, eN5, ip;
    private final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        queue = Volley.newRequestQueue(this);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        //2. Mengaktifkan Remote Sensor Manager
        remoteSensorManager = RemoteSensorManager.getInstance(this);
        mSwitch = (Switch) findViewById(R.id.hrSwitch);
        lastSyncTxt = (TextView)findViewById(R.id.lastSyncTxt);
        hrTxt = (TextView) findViewById(R.id.hrTxt);
        falldetectionTxt = (TextView) findViewById(R.id.falldetectionTxt);

        //6. Firebase inisiai
        mSwitch.setOnCheckedChangeListener(checkBtnChange);
        sharedPreferences = getSharedPreferences(myPreference, Context.MODE_PRIVATE);
        sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        ip = sharedpreferences.getString(inputIP, "");
        eN1 = sharedPreferences.getString("number0", "");
        eN2 = sharedPreferences.getString("number1", "");
        eN3 = sharedPreferences.getString("number2", "");
        eN4 = sharedPreferences.getString("number3", "");
        eN5 = sharedPreferences.getString("number4", "");

        //5. Pengiriman START_TIME melalui Broadcast Intent
        registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));
        Intent intent = new Intent();
        intent.setAction("com.example.Broadcast");
        intent.putExtra("START_TIME", 0L); // clear millisec time
        sendBroadcast(intent);
    }

    //7. Mendeklarasikan Button Sebagai Trigger Activity Dimulai
    CompoundButton.OnCheckedChangeListener checkBtnChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            lastMeasurementTime = System.currentTimeMillis();
            remoteSensorManager.startMeasurement();
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast1");
            intent.putExtra("START_TIME", lastMeasurementTime); // get current millisec time
            sendBroadcast(intent);
            lastSyncTxt.setText(String.valueOf(getDate()));
            SharedPreferences pref = getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong("START_TIME", lastMeasurementTime);
            editor.apply();

        } else {
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast1");
            intent.putExtra("START_TIME", 0L); // clear millisec time
            sendBroadcast(intent);
            lastSyncTxt.setText("");
            remoteSensorManager.stopMeasurement();
            SharedPreferences pref = getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putLong("START_TIME", 0L);
            editor.apply();
        }
        }
    };

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        // Extract data included in the Intent
        try {
            isRunning = intent.getBooleanExtra("IS_RUNNING",false);
            if (!isRunning) {
                //Thread.sleep(3000);
                isStop = true;
            } else {
                isStop = false;
            }
            if (mSwitch != null) {
                if (isStop) {
                    mSwitch.setChecked(isRunning);
                }
            }

            //8. Menerima Broadcast Intent Berisi Data Sensor Dari Sensor Receiver Service
            int message2 = intent.getIntExtra("ACCR", 0);
            int sensorType = intent.getIntExtra("SENSOR_TYPE", 0);
            fallconfirmation = intent.getBooleanExtra("FALLSTATE",false);

            //Receive accelerometer data
            float[] message3 = intent.getFloatArrayExtra("CURRENT");
            //Raspberry Accelerometer processing into conclusion
            if (sensorType == 1) {
                float tmpX = (int)Math.ceil(message3[0]);
                float tmpY = (int)Math.ceil(message3[1]);
                float tmpZ = (int)Math.ceil(message3[2]);
                float currentacc [] = {tmpX, tmpY, tmpZ};
                Log.d("Receiver", "Got Accelerometer: " + Arrays.toString(currentacc) + ". Got Accuracy: " + message2);

                if ((tmpX == 1000)&&(tmpY == 1000)&&(tmpZ == 1000)) {
                    stateactivity = 1; //Resting
                }
                if ((tmpX == 2000)&&(tmpY == 2000)&&(tmpZ == 2000)) {
                    stateactivity = 2; //Moderate
                }
                if ((tmpX == 3000)&&(tmpY == 3000)&&(tmpZ == 3000)) {
                    stateactivity = 3; //Vigorous
                }

                //14. Kesimpulan Fall
                if (fallconfirmation == false) {
                    falldetectionTxt.setText("No fall Detected.");
                } else {
                    Log.d("Fall", "fall detected");
                    falldetectionTxt.setText("Fall Detected.");

                    //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                    showNotificationFall();
                    String fallstate = "FALL DETECTED";

                    //6. to Raspberry HTTP SEND
                    calendar = Calendar.getInstance();
                    simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                    Timestamp = simpleDateFormat.format(calendar.getTime());
                    JSONObject fstate = new JSONObject();
                    fstate.put("fstate", Timestamp);
                    POST("http://"+ip+ ":3000/fstate", fstate);

                    //11. in Raspberry SMS Gateway
                    //Check permission for SMS gateway
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                    } else {
                        SmsManager sms = SmsManager.getDefault();
                        String message = "FALL DETECTED! A fall has been detected on your family member, open your Ambient Assisted Living app to see their location!";

                        Log.i("family number", "fall familynumber size");
                        Log.i("family number", "fall familynumber value ");
                        if ((eN1 != null) && (eN1.length() > 3)){
                            sms.sendTextMessage(eN1, null, message, null, null);}
                        if ((eN2 != null) && (eN2.length() > 3)){
                            sms.sendTextMessage(eN2, null, message, null, null);}
                        if ((eN3 != null) && (eN3.length() > 3)){
                            sms.sendTextMessage(eN3, null, message, null, null);}
                        if ((eN4 != null) && (eN4.length() > 3)){
                            sms.sendTextMessage(eN4, null, message, null, null);}
                        if ((eN5 != null) && (eN5.length() > 3)){
                            sms.sendTextMessage(eN5, null, message, null, null);}
                    }
                }
                DatabaseHandler db = new DatabaseHandler(MainActivity.this);
                long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                lastSyncTxt.setText(String.valueOf(getDate()) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
            }

            //Receive heartrate data
            float[] message1 = intent.getFloatArrayExtra("HR");
            if ((message1 != null ) && (sensorType == 21)) {
                Log.d("Receiver", "Got HR: " + message1[0] + ". Got Accuracy: " + message2);
                int tmpHr = (int)Math.ceil(message1[0] - 0.5f);
                ageValue2 = Integer.parseInt(conf.getAge());
                if (tmpHr > 0) {
                    hrTxt.setText(String.valueOf(tmpHr));

                    //6. to Raspberry HTTP SEND
                    String Hrate = Integer.toString(tmpHr);
                    calendar = Calendar.getInstance();
                    simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                    Timestamp = simpleDateFormat.format(calendar.getTime());
                    JSONObject hrate = new JSONObject();
                    hrate.put("hrate", Hrate);
                    hrate.put("time", Timestamp);
                    POST("http://"+ip+ ":3000/hrate",hrate);

                    //9. Raspberry Heart Rate State
                    if (tmpHr < 60.0) { //(tmpHr< 60.0)
                            /*Log.i("Testing", " Testing. HEART ABNORMALITY DETECTED");
                            Log.i("Testing", "Testing. Got HR: " + tmpHr);
                            Log.i("Testing", " Testing. Got Age value " + ageValue2);
                            Log.i("Testing", "Testing. Activity State: " + stateactivity);*/
                        Log.i("abnormality detected", "HEART EMERGENCY, Got HR: " + tmpHr);

                        //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                        showNotificationHRbelow();
                        String hrstate = "HEARTRATE ABNORMAL";

                        //6. to Raspberry HTTP SEND
                        calendar = Calendar.getInstance();
                        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        Timestamp = simpleDateFormat.format(calendar.getTime());
                        JSONObject hstate = new JSONObject();
                        hstate.put("hstate", Timestamp);
                        POST("http://"+ip+ ":3000/hstate",hstate);

                        //11. Raspberry SMS Gateway
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                        } else {
                            SmsManager sms = SmsManager.getDefault();
                            String message = "HEARTRATE ABNORMALITY DETECTED! A heart rate abnormality on your family member has been detected, open your Ambient Assisted Living app to check their location!";

                            Log.i("family number", "familynumber size");
                            if ((eN1 != null) && (eN1.length() > 3)){
                                sms.sendTextMessage(eN1, null, message, null, null);}
                            if ((eN2 != null) && (eN2.length() > 3)){
                                sms.sendTextMessage(eN2, null, message, null, null);}
                            if ((eN3 != null) && (eN3.length() > 3)){
                                sms.sendTextMessage(eN3, null, message, null, null);}
                            if ((eN4 != null) && (eN4.length() > 3)){
                                sms.sendTextMessage(eN4, null, message, null, null);}
                            if ((eN5 != null) && (eN5.length() > 3)){
                                sms.sendTextMessage(eN5, null, message, null, null);}
                        }
                    }

                    //13. Raspberry Activity Recognition dari accelerometer
                    if (stateactivity == 1) {
                        Log.i("AI Activity conclusion", "Activity Conclusion: Resting Activity, State: " + stateactivity);
                        //9. Memproses Heart Rate Abnormality State
                        if (tmpHr > 100.0) { //(tmpHr > 100.0)
                                /*Log.i("Testing", " Testing. HEART ABNORMALITY DETECTED");
                                Log.i("Testing", "Testing. Got HR: " + tmpHr);
                                Log.i("Testing", " Testing. Got Age value " + ageValue2);
                                Log.i("Testing", "Testing. Activity: Resting, State: " + stateactivity);*/
                            Log.i("abnormality detected", "HEART EMERGENCY, Got HR: " + tmpHr);

                            //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                            showNotificationHabove();
                            String hrstate = "HEARTRATE ABNORMAL";

                            //6. to Raspberry HTTP SEND
                            calendar = Calendar.getInstance();
                            simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                            Timestamp = simpleDateFormat.format(calendar.getTime());
                            JSONObject hstate2 = new JSONObject();
                            hstate2.put("hstate", "H"+Timestamp);
                            POST("http://"+ip+ ":3000/hstate",hstate2);

                            //11. Pengiriman Pesan Darurat Melalui SMS Gateway
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                            } else {
                                SmsManager sms = SmsManager.getDefault();
                                String message = "HEARTRATE ABNORMALITY DETECTED! A heart rate abnormality on your family member has been detected, open your Ambient Assisted Living app to check their location!";

                                Log.i("family number", "familynumber size");
                                if ((eN1 != null) && (eN1.length() > 3)) {
                                    sms.sendTextMessage(eN1, null, message, null, null);
                                }
                                if ((eN2 != null) && (eN2.length() > 3)) {
                                    sms.sendTextMessage(eN2, null, message, null, null);
                                }
                                if ((eN3 != null) && (eN3.length() > 3)) {
                                    sms.sendTextMessage(eN3, null, message, null, null);
                                }
                                if ((eN4 != null) && (eN4.length() > 3)) {
                                    sms.sendTextMessage(eN4, null, message, null, null);
                                }
                                if ((eN5 != null) && (eN5.length() > 3)) {
                                    sms.sendTextMessage(eN5, null, message, null, null);
                                }
                            }
                        }
                    }

                    if (stateactivity == 2) {
                        Log.i("AI Activity conclusion", "Activity Conclusion: Moderate Activity, State: " + stateactivity);
                        //9. Memproses Heart Rate Abnormality State
                        if (ageValue2 != 0) {
                            if (tmpHr > ((220 - ageValue2) * 0.7)) { //(tmpHr > ((220 - ageValue2) * 0.7))
                                    /*Log.i("Testing", " Testing. OVERWORKED CONDITION");
                                    Log.i("Testing", "Testing. Got HR: " + tmpHr);
                                    Log.i("Testing", " Testing. Got Age value " + ageValue2);
                                    Log.i("Testing", "Testing. Gor max heartrate " + ((136 - ageValue2) * 0.7));
                                    Log.i("Testing", "Testing. Activity: Moderate Activity, State: " + stateactivity);*/
                                Log.i("user age process", "user age " + ageValue2 + ", hr max " + ((220 - ageValue2) * 0.7));
                                Log.i("abnormality detected", "YOU ARE TIRED, Got HR: " + tmpHr);

                                //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                showNotificationHRwarning();
                            }
                        }
                    }

                    if (stateactivity == 3) {
                        Log.i("AI Activity conclusion", "Activity Conclusion: Vigorous Activity, State: " + stateactivity);
                        //9. Memproses Heart Rate Abnormality State
                        if (ageValue2 != 0) {
                            if (tmpHr > ((220 - ageValue2) * 0.85)) { //(tmpHr > ((220 - ageValue2) * 0.85))
                                    /*Log.i("Testing", " Testing. OVERWORKED CONDITION");
                                    Log.i("Testing", "Testing. Got HR: " + tmpHr);
                                    Log.i("Testing", " Testing. Got Age value " + ageValue2);
                                    Log.i("Testing", "Testing. Gor max heartrate " + ((220 - ageValue2) * 0.85));
                                    Log.i("Testing", "Testing. Activity: Vigorous Activity, State: " + stateactivity);*/
                                Log.i("user age process", "user age " + ageValue2 + ", hr max " + ((220 - ageValue2) * 0.85));
                                Log.i("abnormality detected", "YOU ARE TIRED, Got HR: " + tmpHr);

                                //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                showNotificationHRwarning();
                            }
                        }
                    }
                }
                DatabaseHandler db = new DatabaseHandler(MainActivity.this);
                long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                lastSyncTxt.setText(String.valueOf(getDate()) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    };

    public void POST(String URLA, JSONObject B) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URLA, B,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {}
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { error.printStackTrace(); }
        });
        request.setTag("Menu");
        queue.add(request);
    }

    public void showNotificationFall() {
        final NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(MainActivity.this);
        note.setContentTitle("FALL ALERT!");
        note.setContentText("A fall has been detected.");
        note.setTicker("FALL ALERT!");
        note.setAutoCancel(true);
        note.setPriority(Notification.PRIORITY_HIGH);
        note.setVibrate(new long[] {0, 100, 100, 100});
        note.setDefaults(Notification.DEFAULT_SOUND);
        note.setSmallIcon(R.mipmap.ic_launcher);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, MainActivity.class), 0);
        note.setContentIntent(pi);
        mgr.notify(693020, note.build());
    }

    public void showNotificationHRbelow() {
        final NotificationManager mgr1 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(MainActivity.this);
        note.setContentTitle("HEART RATE ABNORMALITY!");
        note.setStyle(new NotificationCompat.BigTextStyle().bigText("Your heart rate is below 60bpm, which is below normal range. Consult with your doctor!"));
        note.setContentText("Your heart rate is below 60bpm, which is below normal range. Consult with your doctor!");
        note.setTicker("HEART RATE ABNORMALITY!");
        note.setAutoCancel(true);
        note.setPriority(Notification.PRIORITY_HIGH);
        note.setVibrate(new long[] {0, 100, 100, 100});
        note.setDefaults(Notification.DEFAULT_SOUND);
        note.setSmallIcon(R.mipmap.ic_launcher);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, MainActivity.class), 0);
        note.setContentIntent(pi);
        mgr1.notify(693020, note.build());
    }

    public void showNotificationHabove() {
        final NotificationManager mgr1 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(MainActivity.this);
        note.setContentTitle("HEART RATE ABNORMALITY!");
        note.setStyle(new NotificationCompat.BigTextStyle().bigText("Your heart rate is above 100bpm, which is above normal range. Consult with your doctor!"));
        note.setContentText("Your heart rate is above 100bpm, which is above normal range. Consult with your doctor!");
        note.setTicker("HEART RATE ABNORMALITY!");
        note.setAutoCancel(true);
        note.setPriority(Notification.PRIORITY_HIGH);
        note.setVibrate(new long[] {0, 100, 100, 100});
        note.setDefaults(Notification.DEFAULT_SOUND);
        note.setSmallIcon(R.mipmap.ic_launcher);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, MainActivity.class), 0);
        note.setContentIntent(pi);
        mgr1.notify(693020, note.build());
    }

    public void showNotificationHRwarning() {
        final NotificationManager mgr2 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(MainActivity.this);
        note.setContentTitle("PLEASE TAKE A REST!");
        note.setContentText("You've overworked yourself. Please take a rest!");
        note.setTicker("PLEASE TAKE A REST!");
        note.setPriority(Notification.PRIORITY_HIGH);
        note.setVibrate(new long[] {0, 100, 100, 100});
        note.setDefaults(Notification.DEFAULT_SOUND);
        note.setSmallIcon(R.mipmap.ic_launcher);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, MainActivity.class), 0);
        note.setContentIntent(pi);
        mgr2.notify(960302, note.build());
    }

    //12. Fungsi getDate Sebagai TimeStamp Yang Ditunjukan Sebagai TextView
    private String getDate(){
        try{
            DateFormat sdf = new SimpleDateFormat("dd cc yy HH:mm a");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            Date netDate = (new Date());
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "7:00";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //BusProvider.getInstance().register(this);
        //List<Sensor> sensors = RemoteSensorManager.getInstance(this).getSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //BusProvider.getInstance().unregister(this);
    }
}
