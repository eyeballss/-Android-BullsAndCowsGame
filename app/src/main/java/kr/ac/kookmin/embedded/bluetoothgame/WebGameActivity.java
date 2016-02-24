package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class WebGameActivity extends Activity {


    static int portNum = 5000;
    static String ipAddr = "192.168.1.5";

    Button wSendBtn;
    EditText wEditTextForChat;
    //채팅 리스트뷰
    ListView wChattingList;
    //유저 아이디
    static String userId ="";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_game);

        wSendBtn = (Button) findViewById(R.id.WsendBtn);
        wEditTextForChat = (EditText) findViewById(R.id.WeditxtForChat);
        wChattingList = (ListView) findViewById(R.id.WchattingList);
        final ArrayAdapter<String> wChattingAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_items);
        wChattingList.setAdapter(wChattingAdapter);


        Intent i = getIntent();
        userId = i.getStringExtra("ID");

        final WConnectThread thrd = new WConnectThread(wChattingAdapter, ipAddr, portNum, this, userId);
        thrd.start();

        wSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Acting", "센드 버튼 누름");
                if (thrd == null) {
                    Log.d("Exception", "WConnectThread가 없음");
                    return;
                }
                if (wEditTextForChat.getText().length() > 0) {
                    thrd.write(wEditTextForChat.getText().toString());
                    Log.d("Acting", "쓰레드로 에딧 값 넘김");
                    wEditTextForChat.setText("");
                }
            }
        });
    }



//    public void getUserId() {
//        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(WebGameActivity.this);
//        final EditText input = new EditText(this);
//        alert_confirm.setView(input);
//        alert_confirm.setMessage("사용 할 아이디를 입력하세요").setCancelable(false).setPositiveButton("확인",
//
//
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (!input.getText().toString().equals("")) {
//                            userId = input.getText().toString();
//                        } else getUserId();
//                    }
//                });
//        AlertDialog alert = alert_confirm.create();
//        alert.show();
//    }


}
