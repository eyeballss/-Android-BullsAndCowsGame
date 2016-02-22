package kr.ac.kookmin.embedded.bluetoothgame;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button mBluetoothSelBtn;
    Button mWebSelBtn;

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
                Intent in = new Intent(MainActivity.this, WebGameActivity.class);
                startActivity(in);
            }
        });

    }
}