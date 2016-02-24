package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Button mBluetoothSelBtn;
    Button mWebSelBtn;
    String userId=null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothSelBtn = (Button)findViewById(R.id.playBluetooth);
        mWebSelBtn = (Button)findViewById(R.id.playWeb);

        mBluetoothSelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent in = new Intent(MainActivity.this,BluetoothGameActivity.class);
                startActivity(in);
            }
        });

        mWebSelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //아이디가 없으면 아이디 먼저 적음
                if (userId == null) {
                    getUserId();
                } else {
                    Intent in = new Intent(MainActivity.this, WebGameActivity.class);
                    in.putExtra("ID", userId);
                    startActivity(in);
                }
            }
        });

    }


    public void getUserId(){
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        alert_confirm.setView(input);
        alert_confirm.setMessage("Input your ID").setCancelable(false).setPositiveButton("OK",


                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!input.getText().toString().equals("")) {

                            if (input.getText().length() < 3) {
                                Toast.makeText(getApplicationContext(), "ID length is 3 at least",
                                        Toast.LENGTH_SHORT).show();
                                getUserId();
                            } else if (input.getText().toString().equals("game")) {
                                Toast.makeText(getApplicationContext(), "you can not use this as ID",
                                        Toast.LENGTH_SHORT).show();
                                getUserId();
                            } else {
                                userId = input.getText().toString();
                                mWebSelBtn.setText("[" + userId + "]" + "\n" + "WEB Game Start");
                                Log.d("Acting", "유저 아이디 정해짐 : " + userId);
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "Input your ID",
                                    Toast.LENGTH_SHORT).show();
                            getUserId();
                        }
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }
}