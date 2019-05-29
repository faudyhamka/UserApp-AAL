package com.example.faudyhamka.userapp;

import android.content.Context;
import android.content.Intent;;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ENumber extends AppCompatActivity {
    public static final String myPreference = "my_pref";
    EditText enumber;
    Button butt;
    TextView num0, num1, num2, num3, num4;
    Integer i;
    boolean a;
    Number n = new Number();
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enumber);

        enumber = (EditText) findViewById(R.id.editText2);
        butt = (Button) findViewById(R.id.button);
        num0 = (TextView) findViewById(R.id.num0);
        num1 = (TextView) findViewById(R.id.num1);
        num2 = (TextView) findViewById(R.id.num2);
        num3 = (TextView) findViewById(R.id.num3);
        num4 = (TextView) findViewById(R.id.num4);

        sharedpreferences = getSharedPreferences(myPreference, Context.MODE_PRIVATE);
        String en0 = sharedpreferences.getString("number0", "");
        String en1 = sharedpreferences.getString("number1", "");
        String en2 = sharedpreferences.getString("number2", "");
        String en3 = sharedpreferences.getString("number3", "");
        String en4 = sharedpreferences.getString("number4", "");
        num0.setText(en0); num1.setText(en1);
        num2.setText(en2); num3.setText(en3);
        num4.setText(en4);

        butt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if ((enumber.getText().toString().length() == 0)) {
                    Toast.makeText(getApplicationContext(), "You must fill an emergency number", Toast.LENGTH_SHORT).show();
                } else {
                    i = 0;
                    a = false;
                    while (!a || i<5) {
                        if (n.getNum(i).length() < 3) {
                            String m = enumber.getText().toString();
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString("number"+Integer.toString(i), m);
                            editor.apply();
                            n.setNum(i, m);
                            a = true;
                        }
                        i++;
                    }
                    Intent myIntent = new Intent(ENumber.this,Menu.class);
                    startActivity(myIntent);
                }
            }
        });
    }
}