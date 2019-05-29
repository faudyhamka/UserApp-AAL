package com.example.faudyhamka.userapp;


import com.example.faudyhamka.userapp.data.Sensor;

//1. Deklarasi dalam mendapatkan data sensor baru dari Smartwatch
public class NewSensorEvent {
    private Sensor sensor;

    public NewSensorEvent(Sensor sensor) {
        this.sensor = sensor;
    }

    public Sensor getSensor() {
        return sensor;
    }
}
