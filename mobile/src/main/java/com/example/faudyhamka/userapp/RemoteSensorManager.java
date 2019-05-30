package com.example.faudyhamka.userapp;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.example.faudyhamka.userapp.data.Sensor;
import  com.example.faudyhamka.userapp.data.SensorDataPoint;
import  com.example.faudyhamka.userapp.data.SensorNames;
import  com.example.faudyhamka.userapp.events.BusProvider;
import com.example.faudyhamka.userapp.events.NewSensorEvent;
import com.example.faudyhamka.userapp.events.SensorUpdatedEvent;
import com.example.shared.ClientPaths;
import com.example.shared.DataMapKeys;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.NodeClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RemoteSensorManager {
    private static final String TAG = "MOBILE/RSM";
    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;
    private static RemoteSensorManager instance;
    private Context context;
    private ExecutorService executorService;
    private SparseArray<Sensor> sensorMapping;
    private ArrayList<Sensor> sensors;
    private SensorNames sensorNames;
    private GoogleApiClient googleApiClient;

    public static synchronized RemoteSensorManager getInstance(Context context) {
        if (instance == null) { instance = new RemoteSensorManager(context.getApplicationContext()); }
        return instance;
    }

    private RemoteSensorManager(Context context) {
        this.context = context;
        this.sensorMapping = new SparseArray<Sensor>();
        this.sensors = new ArrayList<Sensor>();
        this.sensorNames = new SensorNames();

        // * Inisiasi koneksi GoogleApiClient instance dengan service wearable
        this.googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        this.executorService = Executors.newCachedThreadPool();
    }

    public List<Sensor> getSensors() {
        return (List<Sensor>) sensors.clone();
    }

    public Sensor getSensor(long id) {
        return sensorMapping.get((int) id);
    }

    private Sensor createSensor(int id) {
        Sensor sensor = new Sensor(id, sensorNames.getName(id));
        sensors.add(sensor);
        sensorMapping.append(id, sensor);
        BusProvider.postOnMainThread(new NewSensorEvent(sensor));
        return sensor;
    }

    private Sensor getOrCreateSensor(int id) {
        Sensor sensor = sensorMapping.get(id);
        if (sensor == null) {
            sensor = createSensor(id);
        }
        return sensor;
    }

    public synchronized void addSensorData(int sensorType, int accuracy, long timestamp, float[] values) {

        // ** Menarik Data Sensor dari Map Data
        Sensor sensor = getOrCreateSensor(sensorType);

        // TODO: We probably want to pull sensor data point objects from a pool here
        SensorDataPoint dataPoint = new SensorDataPoint(timestamp, accuracy, values);
        sensor.addDataPoint(dataPoint);
        BusProvider.postOnMainThread(new SensorUpdatedEvent(sensor, dataPoint));
    }

    public void filterBySensorId(final int sensorId) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                filterBySensorIdInBackground(sensorId);
            }
        });
    };

    private void filterBySensorIdInBackground(final int sensorId) {
        Log.d(TAG, "filterBySensorId(" + sensorId + ")");

        // *** Sinkronisasi Antara Wear dan Handheld
        if (validateConnection()) {
            PutDataMapRequest dataMap = PutDataMapRequest.create("/filter");

            dataMap.getDataMap().putInt(DataMapKeys.FILTER, sensorId);
            dataMap.getDataMap().putLong(DataMapKeys.TIMESTAMP, System.currentTimeMillis());

            PutDataRequest putDataRequest = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "Filter by sensor " + sensorId + ": " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
    }

    public void startMeasurement() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground(ClientPaths.START_MEASUREMENT);
            }
        });
    }

        public void startXYZ() {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    controlMeasurementInBackground(ClientPaths.START_XYZ);
                }
            });
        }

        public void stopXYZ() {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    controlMeasurementInBackground(ClientPaths.STOP_XYZ);
                }
            });
        }

    public void stopMeasurement() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                controlMeasurementInBackground(ClientPaths.STOP_MEASUREMENT);
            }
        });
    }

    private void controlMeasurementInBackground(final String path) {
        if (validateConnection()) {
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await().getNodes();

            Log.d(TAG, "Sending to nodes: " + nodes.size());

            for (Node node : nodes) {
                Wearable.MessageApi.sendMessage(
                        googleApiClient, node.getId(), path, null
                ).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "controlMeasurementInBackground(" + path + "): " + sendMessageResult.getStatus().isSuccess());
                    }
                });
            }
        } else {
            Log.w(TAG, "No connection possible");
        }
    }

    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }
        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        return result.isSuccess();
    }
}