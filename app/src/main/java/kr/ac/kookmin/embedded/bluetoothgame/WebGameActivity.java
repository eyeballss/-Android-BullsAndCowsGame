package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class WebGameActivity extends Activity {


    static int portNum = 5001;
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

        final ConnectThread thrd = new ConnectThread(wChattingAdapter);
        thrd.start();

        //먼저 유저 아이디를 만들어 보냄
        getUserId();
        thrd.write(userId);

        wSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(thrd==null) return;
                if (wEditTextForChat.getText().length() > 0) {
                    thrd.write(wEditTextForChat.getText().toString());
                    wEditTextForChat.setText("");
                }
            }
        });
    }

    class ConnectThread extends Thread {
        ArrayAdapter<String> wChattingAdapter;
        DataOutputStream dos;
        DataInputStream dis;
        Socket socket;

        public ConnectThread(ArrayAdapter<String> adapter) {
            wChattingAdapter = adapter;
        }

        //쓰레드 시작. 여기서 서버로부터 값을 받음
        public void run() {
            try {

                socket = new Socket(ipAddr, portNum);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                while (true) {
                    String obj = dis.readUTF();
                    sendChatMsg(obj);

                    if (obj.equals("exit")) {
                        break;
                    }
                }

                socket.close();
                finish();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //여기서 edittext의 입력값을 서버로 보냄
        public void write(String str) {
            try {
                dos.writeUTF(str);
                dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //채팅 메세지를 리스트뷰에 올림
        public void sendChatMsg(String chatMsg) {
            Message msg = Message.obtain(mChatHdr, 0, chatMsg);
            mChatHdr.sendMessage(msg);
            Log.d("Log", "Chatting contents : " + chatMsg);
        }

        // 메시지 화면 출력을 위한 핸들러
        Handler mChatHdr = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    String chatMsg = (String) msg.obj;
                    wChattingAdapter.add(chatMsg);
                    wChattingAdapter.notifyDataSetChanged();
                }
            }
        };
    }






    public void getUserId(){
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(WebGameActivity.this);
        final EditText input = new EditText(this);
        alert_confirm.setView(input);
        alert_confirm.setMessage("사용 할 아이디를 입력하세요").setCancelable(false).setPositiveButton("확인",


                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!input.getText().toString().equals("")){
                            userId= input.getText().toString();
                        }
                        else getUserId();
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }






    // 토스트 출력
    public void sendToastMsg(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mToastHandler, 0, strMsg);
        mToastHandler.sendMessage(msg);
        Log.d("Log", strMsg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mToastHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                Toast.makeText(getApplicationContext(), strMsg, Toast.LENGTH_SHORT).show();
            }
        }
    };

}
