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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ENumber extends AppCompatActivity {
    public static final String myPreference = "my_pref";
    public static final String mypreference = "mypref";
    public static final String inputIP = "input_IP";
    EditText enumber;
    Button butt;
    TextView num0, num1, num2, num3, num4;
    String ip, en0, en1, en2, en3, en4;
    Integer i;
    Number n = new Number();
    RequestQueue queue;
    SharedPreferences sharedPreferences, sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enumber);
        queue = Volley.newRequestQueue(this);

        enumber = (EditText) findViewById(R.id.editText2);
        butt = (Button) findViewById(R.id.button);
        num0 = (TextView) findViewById(R.id.num0);
        num1 = (TextView) findViewById(R.id.num1);
        num2 = (TextView) findViewById(R.id.num2);
        num3 = (TextView) findViewById(R.id.num3);
        num4 = (TextView) findViewById(R.id.num4);

        sharedPreferences = getSharedPreferences(myPreference, Context.MODE_PRIVATE);
        sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
        ip = sharedpreferences.getString(inputIP, "");
        en0 = sharedPreferences.getString("number0", "");
        en1 = sharedPreferences.getString("number1", "");
        en2 = sharedPreferences.getString("number2", "");
        en3 = sharedPreferences.getString("number3", "");
        en4 = sharedPreferences.getString("number4", "");
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
                    while (true) {
                        String check = sharedPreferences.getString("number"+i, "");
                        if (check.length() < 3) {
                            String m = enumber.getText().toString();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("number"+Integer.toString(i), m);
                            editor.apply();
                            try {
                                JSONObject num = new JSONObject();
                                num.put("enumber", m);
                                num.put("id", String.valueOf(i+1));
                                POST("http://"+ip+":3000/enumber", num);
                            } catch (JSONException e) {e.printStackTrace();}
                            break;
                        }
                        if (i==5) {
                            Toast.makeText(getApplicationContext(), "Emergency Number List is Full", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        i++;
                    }
                    Intent myIntent = new Intent(ENumber.this,Menu.class);
                    startActivity(myIntent);
                }
            }
        });
    }

    public void del1(View view) {
        if (en0.length()>3) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("number0", " ");
            editor.apply();
            num0.setText("");
            try {
                JSONObject num = new JSONObject();
                num.put("enumber", "0");
                num.put("id", "1");
                POST("http://"+ip+":3000/enumber", num);
            } catch (JSONException e) {e.printStackTrace();}
        } else {
            Toast.makeText(getApplicationContext(), "No emergency number in this row", Toast.LENGTH_SHORT).show();
        }
    }

    public void del2(View view) {
        if (en1.length()>3) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("number1", " ");
            editor.apply();
            num1.setText("");
            try {
                JSONObject num = new JSONObject();
                num.put("enumber", "0");
                num.put("id", "2");
                POST("http://"+ip+":3000/enumber", num);
            } catch (JSONException e) {e.printStackTrace();}
        } else {
            Toast.makeText(getApplicationContext(), "No emergency number in this row", Toast.LENGTH_SHORT).show();
        }
    }

    public void del3(View view) {
        if (en2.length()>3) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("number2", " ");
            editor.apply();
            num2.setText("");
            try {
                JSONObject num = new JSONObject();
                num.put("enumber", "0");
                num.put("id", "3");
                POST("http://"+ip+":3000/enumber", num);
            } catch (JSONException e) {e.printStackTrace();}
        } else {
            Toast.makeText(getApplicationContext(), "No emergency number in this row", Toast.LENGTH_SHORT).show();
        }
    }

    public void del4(View view) {
        if (en3.length()>3) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("number3", " ");
            editor.apply();
            num3.setText("");
            try {
                JSONObject num = new JSONObject();
                num.put("enumber", "0");
                num.put("id", "4");
                POST("http://"+ip+":3000/enumber", num);
            } catch (JSONException e) {e.printStackTrace();}
        } else {
            Toast.makeText(getApplicationContext(), "No emergency number in this row", Toast.LENGTH_SHORT).show();
        }
    }

    public void del5(View view) {
        if (en4.length()>3) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("number4", " ");
            editor.apply();
            num4.setText("");
            try {
                JSONObject num = new JSONObject();
                num.put("enumber", "0");
                num.put("id", "5");
                POST("http://"+ip+":3000/enumber", num);
            } catch (JSONException e) {e.printStackTrace();}
        } else {
            Toast.makeText(getApplicationContext(), "No emergency number in this row", Toast.LENGTH_SHORT).show();
        }
    }

    public void POST(String URLA, JSONObject B) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URLA, B,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {}
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) { error.printStackTrace(); }
        });
        request.setTag("Enumber");
        queue.add(request);
    }
}