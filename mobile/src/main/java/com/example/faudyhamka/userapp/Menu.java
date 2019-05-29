package com.example.faudyhamka.userapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Menu extends AppCompatActivity {
    public static final String mypreference = "mypref";
    public static final String inputIP = "input_IP";
    public static final String inputAge = "input_Age";
    Configuration conf = new Configuration();
    Number n = new Number();
    Button buttip, butten, buttmenu, buttmap;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        buttip = (Button) findViewById(R.id.toip);
        butten = (Button) findViewById(R.id.toenum);
        buttmenu = (Button) findViewById(R.id.tomain);
        buttmap = (Button) findViewById(R.id.tomap);
        sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        String ip = sharedpreferences.getString(inputIP, "");
        String name = sharedpreferences.getString(inputAge, "");
        conf.setIP(ip); conf.setAge(name);
    }

    public void goToIPAge(View view)
    {
        Intent myintent = new Intent(Menu.this,IPAge.class);
        startActivity(myintent);
    }

    public void goToENumber(View view)
    {
        Intent myintent = new Intent(Menu.this,ENumber.class);
        startActivity(myintent);
    }

    public void goToMain(View view)
    {
        if ((conf.getAge() != null) && (conf.getIP() != null)) {
            Intent myintent = new Intent(Menu.this, MainActivity.class);
            startActivity(myintent);
        } else {
            Toast.makeText(getApplicationContext(), "Please go to configuration page first", Toast.LENGTH_SHORT).show();
        }
    }

    public void goToMap(View view)
    {
        if ((conf.getAge() != null) && (conf.getIP() != null)) {
            Intent myintent = new Intent(Menu.this,MyGPS.class);
            startActivity(myintent);
        } else {
            Toast.makeText(getApplicationContext(), "Please go to configuration page first", Toast.LENGTH_SHORT).show();
        }
    }
}