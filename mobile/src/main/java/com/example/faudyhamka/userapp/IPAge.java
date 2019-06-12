package com.example.faudyhamka.userapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.support.v7.widget.Toolbar;

public class IPAge extends AppCompatActivity {
    public static final String mypreference = "mypref";
    public static final String inputIP = "input_IP";
    public static final String inputAge = "input_Age";
    EditText IP, Age;
    Button btnStore;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipage);
        IP = (EditText) findViewById(R.id.editIP);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,4}(\\.(\\d{1,4}(\\.(\\d{1,4}(\\.(\\d{1,4})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
        IP.setFilters(filters);
        Age = (EditText) findViewById(R.id.editAge);
        btnStore = (Button) findViewById(R.id.buttStore);

        btnStore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ((IP.getText().toString().length() == 0) && (Age.getText().toString().length() == 0)) {
                    Toast.makeText(getApplicationContext(), "Please fill both forms", Toast.LENGTH_SHORT).show();
                } else if ((IP.getText().toString().length() == 0) && (Age.getText().toString().length() != 0)) {
                    Toast.makeText(getApplicationContext(), "Please fill your IP Address", Toast.LENGTH_SHORT).show();
                } else if ((Age.getText().toString().length() == 0) && (IP.getText().toString().length() != 0)) {
                    Toast.makeText(getApplicationContext(), "Please fill your Age", Toast.LENGTH_SHORT).show();
                } else {
                    sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
                    String n = IP.getText().toString();
                    String e = Age.getText().toString();
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString(inputIP, n);
                    editor.putString(inputAge, e);
                    editor.apply();

                    // go to menu
                    Intent myIntent = new Intent(IPAge.this, Menu.class);
                    startActivity(myIntent);
                }
            }
        });
    }
}
