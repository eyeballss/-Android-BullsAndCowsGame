package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
    static String userId = "";
    static String opponentId = "";

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


        /******************************
         * 아래는 리스너들 (Send, ListViewItem)
         * ******************************
         */

        //Send 버튼을 누르면 ....
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

        wChattingList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //첫번째 파라미터 : 클릭된 아이템을 보여주고 있는 AdapterView 객체(여기서는 ListView객체)
            //두번째 파라미터 : 클릭된 아이템 뷰
            //세번째 파라미터 : 클릭된 아이템의 위치(ListView이 첫번째 아이템(가장위쪽)부터 차례대로 0,1,2,3.....)
            //네번재 파리미터 : 클릭된 아이템의 아이디(특별한 설정이 없다면 세번째 파라이터인 position과 같은 값)
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemStr = wChattingAdapter.getItem(position).toString();
                opponentId = itemStr.substring(1, itemStr.indexOf("]")).toString().trim();

                sendGameRequest(thrd);

            }
        });
        //게임 요청을 보내는 메세지창

    }


    public void sendGameRequest(final WConnectThread thrd) {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
        alert_confirm.setMessage("send game request to " + opponentId + " ?").setCancelable(false).setPositiveButton("YES",

                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        thrd.write("game "+opponentId); //게임 요청 메세지를 보냄!
                    }
                }).setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
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
