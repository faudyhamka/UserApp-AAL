package com.example.faudyhamka.userapp;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/*public class XYZService
onCreate > client, builder, startMeasurement()
onDestroy
startMeasurement > accelerometerSensor
onSensorChanged > SENS_ACCELEROMETER
*/
public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "WEAR/SS";
    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_HEARTRATE = Sensor.TYPE_HEART_RATE;

    private DeviceClient client;
    SensorManager mSensorManager;
    private Sensor mHeartrateSensor;
    ScheduledExecutorService hrScheduler;
    Notification.Builder builder;

    float gravity[] = {0f, 0f, 0f}, linear_acceleration[] = {0f, 0f, 0f}; //0f = float 0
    double Zvalue, totLinear, totAcc, FallCounter = 0, threshold = 38.22, g = 9.8;
    double Sigma1, Sigma2, Sigma3, Sigma4, Sigma5, Sigma6, Sigma7, Sigma8, Sigma9, Sigma10, Sigma11, Sigma12 = 0;
    double kuadrat, kalidua, rawsigma = 0;
    double Aj, Ajtot, Mu, AItot = 0;
    int N, activate = 0;
    int stateactivity = 0;
    double AImax = 0;
    double AImin = 100.0;

    Handler handler;
    int delay = 50;
    private float tmpHR = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        //6. Inisialisasi Client
        client = DeviceClient.getInstance(this);

        Log.d("SS", client.toString());

        builder = new Notification.Builder(this);
        builder.setContentTitle("Fall Detection");
        builder.setContentText("Collecting heartrate and acceleration sensor data..");
        builder.setSmallIcon(R.mipmap.ic_launcher);

        startForeground(1, builder.build());
        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void startMeasurement() {

        //1. Inisialisasi sensor awal
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(SENS_ACCELEROMETER);
        mHeartrateSensor = mSensorManager.getDefaultSensor(SENS_HEARTRATE);

        //Register Listener
        if (mSensorManager != null) {
            //Accelerometer Data
            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Accelerometer found");
            }
            //Heartrate Data
            if (mHeartrateSensor != null) {
                final int measurementDuration   = 30;   // Seconds
                final int measurementBreak      = 15;    // Seconds

                hrScheduler = Executors.newScheduledThreadPool(1);
                hrScheduler.scheduleAtFixedRate(
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "register Heartrate Sensor");
                                mSensorManager.registerListener(SensorService.this, mHeartrateSensor, SensorManager.SENSOR_DELAY_NORMAL);

                                try {
                                    Thread.sleep(measurementDuration * 1000);
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Interrupted while waitting to unregister Heartrate Sensor");
                                }

                                Log.d(TAG, "unregister Heartrate Sensor");
                                mSensorManager.unregisterListener(SensorService.this, mHeartrateSensor);
                            }
                        }, 3, measurementDuration + measurementBreak, TimeUnit.SECONDS);
            } else {
                Log.d(TAG, "No Heartrate Sensor found");
            }
        }
        //8. Pemanggilan fungsi activity recognition


        handler = new Handler();
        handler.postDelayed(new Runnable(){
            public void run(){
                activityRecognition();
                handler.postDelayed(this, delay);
            }
        }, delay);

    }

    private void stopMeasurement() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (hrScheduler != null)
            hrScheduler.shutdown();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //2. Deteksi data yang dibutuhkan melalui sensor (Accelerometer dan Heartrate)
        //Heartrate Data
        if (event.sensor.getType() == SENS_HEARTRATE) {
            tmpHR = event.values[0];

            Log.d(TAG,"Broadcast HR.");
            Log.d("Sensor Detecting HR: ", event.accuracy + "," + event.timestamp + "," + String.valueOf(tmpHR));

            //4. Mengirimkan data yang sudah didapatkan ke MainActivity menggunakan Intent Broadcast
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast");
            intent.putExtra("HR", event.values);
            intent.putExtra("ACCR", event.accuracy);
            intent.putExtra("TIME", event.timestamp);
            sendBroadcast(intent);
        }

        //Accelerometer Data
        if (event.sensor.getType() == SENS_ACCELEROMETER) {
            //3. Melakukan processing fall detection menggunakan data accelerometer yang sudah didapatkan
            final float alpha = (float) 0.8;
            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]; // gravity = 0.8 * gravity[0] + (1-0.8) * acceleration
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];

            Log.i("Fall", " gravity 0:" + gravity[0] + " gravity 1: " + gravity[1] + " gravity 2: " + gravity[2]);
            Log.i("Fall", " acc 0:" + event.values[0] + " gravity 1: " + event.values[1] + " gravity 2: " + event.values[2]);
            Log.i("Fall", " linear 0:" + linear_acceleration[0] + " linear 1: " + linear_acceleration[1] + " linear 2: " + linear_acceleration[2]);

            totAcc = Math.sqrt(event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]);

            Log.i("Fall", "totAcc = " + totAcc);

            totLinear = Math.sqrt(linear_acceleration[0] * linear_acceleration[0] +
                    linear_acceleration[1] * linear_acceleration[1] +
                    linear_acceleration[2] * linear_acceleration[2]);

            Log.i("Fall", "totLinear = " + totLinear);
            Zvalue = ((totAcc * totAcc) - (totLinear * totLinear) - (g * g))/(2 *g);

            Log.i("Fall", "Z value = " + Zvalue);
            float currentacc [] = {linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]};

            Log.d(TAG,"Broadcast ACC.");
            Log.d(TAG, event.accuracy + "," + event.timestamp + "," + Arrays.toString(currentacc));

            FallCounter = ((Zvalue > threshold) ? FallCounter + 1 : 0); //if fall counter = totacc > threshold, fallcounter = +1, else 0.

            if (Zvalue > threshold) {
                Log.i("Fall", "melebihi threshold");
            }

            Log.i("Fall", "fall counter = " + FallCounter);

            Log.i("State Activity", "state activity = " + stateactivity);

            //9. Mengirimkan Fall Status melalui Device Client ke Smartphone
            if (FallCounter == 3) { //if (FallCounter == 5 && !detected)
                Log.i("fall", "fall detected");
                client.sendFallStatus(true);
            } else {
                client.sendFallStatus(false);
            }

            //10. Penentuan nilai activity state
            if (stateactivity == 1) {
                event.values[0] = 1000;
                event.values[1] = 1000;
                event.values[2] = 1000;
            }

            if (stateactivity == 2) {
                event.values[0] = 2000;
                event.values[1] = 2000;
                event.values[2] = 2000;
            }

            if (stateactivity == 3) {
                event.values[0] = 3000;
                event.values[1] = 3000;
                event.values[2] = 3000;
            }

            if (stateactivity == 0) {
                event.values[0] = 5000;
                event.values[1] = 5000;
                event.values[2] = 5000;
            }

            float after [] = {event.values[0], event.values[1], event.values[2]};
            Log.d("Sensor activity: ", "Sensor based on activity recognition: " + event.accuracy + "," + event.timestamp + "," + Arrays.toString(after));

        }

        // 5. Mengirimkan data yang sudah didapatkan melalui Device Client ke Smartphone menggunakan perintah client send sensor data (heartrate dalam bentuk sensor awal dan accelerometer dalam bentuk state)
        client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //7. Fungsi Activity Recognition
    public void activityRecognition() {
        if (N < 1200) {
            N = N + 1;

            Aj = totLinear;

            Log.i("AI", "Aj " + N + " " + Aj);


            if (N <= 100) {


                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 100) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma1 " + rawsigma);
                    Sigma1 = Math.sqrt(rawsigma / 100.0);
                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;
                    Log.i("AI raw", "Sigma1 " + Sigma1);
                    Log.i("AI raw", "raw AItot1 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 100) && (N <= 200)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 200) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma2 " + rawsigma);
                    Sigma2 = Math.sqrt(rawsigma / 100.0);

                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;
                    Log.i("AI raw", "Sigma2 " + Sigma2);
                    Log.i("AI raw", "raw AItot2 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 200) && (N <= 300)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 300) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma3 " + rawsigma);
                    Sigma3 = Math.sqrt(rawsigma / 100.0);

                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;
                    Log.i("AI raw", "Sigma3 " + Sigma3);
                    Log.i("AI raw", "raw AItot3 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;

                }
            }
            if ((N > 300) && (N <= 400)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 400) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma4 " + rawsigma);
                    Sigma4 = Math.sqrt(rawsigma / 100.0);
                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;
                    Log.i("AI raw", "Sigma4 " + Sigma4);
                    Log.i("AI raw", "raw AItot4 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;

                }
            }
            if ((N > 400) && (N <= 500)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 500) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma5 " + rawsigma);
                    Sigma5 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma5 " + Sigma5);
                    Log.i("AI raw", "raw AItot5 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 500) && (N <= 600)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 600) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma6 " + rawsigma);
                    Sigma6 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma6 " + Sigma6);
                    Log.i("AI raw", "raw AItot6 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 600) && (N <= 700)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 700) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma7 " + rawsigma);
                    Sigma7 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma7 " + Sigma7);
                    Log.i("AI raw", "raw AItot7 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 700) && (N <= 800)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 800) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma8 " + rawsigma);
                    Sigma8 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma8 " + Sigma8);
                    Log.i("AI raw", "raw AItot8 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 800) && (N <= 900)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 900) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma9 " + rawsigma);
                    Sigma9 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma9 " + Sigma9);
                    Log.i("AI raw", "raw AItot9 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 900) && (N <= 1000)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 1000) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma10 " + rawsigma);
                    Sigma10 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma10 " + Sigma10);
                    Log.i("AI raw", "raw AItot10 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 1000) && (N <= 1100)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 1100) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma11 " + rawsigma);
                    Sigma11 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;

                    Log.i("AI raw", "Sigma11 " + Sigma11);
                    Log.i("AI raw", "raw AItot11 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                }
            }
            if ((N > 1100) && (N <= 1200)) {

                Ajtot = Ajtot + Aj;
                kuadrat = kuadrat + (Aj * Aj);
                kalidua = kalidua + (2 * Aj);

                if (N == 1200) {
                    Mu = Ajtot / 100.0;
                    rawsigma = (100.0 * Mu * Mu) - (Mu * kalidua) + kuadrat;
                    Log.i("AI raw", "rawsigma12 " + rawsigma);
                    Sigma12 = Math.sqrt(rawsigma / 100.0);


                    AItot = Sigma1 + Sigma2 + Sigma3 + Sigma4 + Sigma5 + Sigma6 + Sigma7 + Sigma8 + Sigma9 + Sigma10 + Sigma11 + Sigma12;
                    N = 0;
                    Log.i("AI raw", "Sigma12 " + Sigma12);
                    Log.i("AI raw", "raw AItot12 " + AItot);

                    Ajtot = 0;
                    kuadrat = 0;
                    kalidua = 0;
                    activate = 1;
                    N = 0;
                }
            }

            if (AItot > AImax){
                AImax = AItot;
                Log.i("AItot max", "tmp AI max, " + AImax);
            }
            if (AItot < AImin){
                AImin = AItot;
                Log.i("AItot max", "tmp AI min, " + AImin);
            }
        }

        if (activate == 1) {
            if ((AItot != 0) && (AItot < 7.0)) {
                Log.i("AI Activity conclusion", "Resting, AItot " + AItot);
                stateactivity = 1;
            }
            if ((AItot != 0) && (AItot >= 7.0) && (AItot < 35.0)) {
                Log.i("AI Activity conclusion", "Moderate Activity, AItot " + AItot);
                stateactivity = 2;
            }
            if ((AItot != 0) && (AItot >= 35.0)) {
                Log.i("AI Activity conclusion", "Vigurous Activity, AItot " + AItot);
                stateactivity = 3;
            }
        } else {
            stateactivity = 0;
        }
    }
}