package com.example.faudyhamka.userapp;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.example.shared.DataMapKeys;
import com.example.shared.ClientPaths;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageReceiverService extends WearableListenerService {
    private static final String TAG = "WEAR/MRS";
    private DeviceClient deviceClient;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MRS", "HEHE");
        //1. Deklarasi pemanggilan Device Client
        deviceClient = DeviceClient.getInstance(this);
        Log.d("MRS", deviceClient.toString());
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("MRS", "onDataChanged");
        super.onDataChanged(dataEvents);

        for (DataEvent dataEvent : dataEvents) {
            //Indicates that the enclosing DataEvent was triggered by a data item being added or changed.
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                //2. Pemanggilan Data Item Berdasarkan Uri yang terdiri dari creator dan path
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();

                if (path.startsWith("/filter")) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    int filterById = dataMap.getInt(DataMapKeys.FILTER);
                    deviceClient.setSensorFilter(filterById);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Received message: " + messageEvent.getPath());

        //3. Mengembalikan Path Message Yang Dikirimkan
        if (messageEvent.getPath().equals(ClientPaths.START_MEASUREMENT)) {
            startService(new Intent(this, SensorService.class));
            /*startService(new Intent(this, XYZService,class));*/
        }

        if (messageEvent.getPath().equals(ClientPaths.STOP_MEASUREMENT)) {
            stopService(new Intent(this, SensorService.class));
            /*stopService(new Intent(this, XYZService,class));*/
        }
        /*
        if (messageEvent.getPath().equals(ClientPaths.START_XYZ)) {
            startService(new Intent(this, XYZService,class));
        if (messageEvent.getPath().equals(ClientPaths.STOP_XYZ)) {
            stopService(new Intent(this, XYZService,class));*/
    }
}