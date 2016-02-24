package kr.ac.kookmin.embedded.bluetoothgame;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

class WConnectThread extends Thread {
    WebGameActivity webGameAct;
    ArrayAdapter<String> wChattingAdapter;
    DataOutputStream dos;
    DataInputStream dis;
    Socket socket;
    int portNum;
    String ipAddr, userId, opponentID;

    public WConnectThread(ArrayAdapter<String> adapter, String ip, int port, WebGameActivity act, String id) {
        Log.d("Acting", "쓰레드 생성자 시작");
        wChattingAdapter = adapter;
        ipAddr=ip.toString();
        portNum=port;
        webGameAct=act;
        userId = id;

    }

    //쓰레드 시작. 여기서 서버로부터 값을 받음
    public void run() {
        try {

                socket = new Socket(ipAddr, portNum);

                sendToastMsg("connected Server successfully");
                Log.d("Acting", "소켓 생성");

                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                Log.d("Acting", "dis, dos 생성");

                //서버에서 아이디 먼저 원하므로 아이디를 우선 보내줌.
                dos.writeUTF(userId);
                dos.flush();
                Log.d("Acting", "서버로 아이디 먼저 보냄");




            while (true) {
                String obj = dis.readUTF();
                sendChatMsg(obj);
                Log.d("Acting", "데이터를 서버에서 받음");

                if (obj.equals("exit")) {
                    break;
                }
            }

            socket.close();


        } catch (Exception e) {
            Log.d("Exception", "run 에러");
            e.printStackTrace();
        }
    }

    public void write(String msg){
        try {
            dos.writeUTF("[ " + userId + " ] " +msg);
            dos.flush();
            Log.d("Acting", "데이터를 서버로 보냄");
        } catch (Exception e) {
            Log.d("Exception", "서버로 보내기 에러");
            e.printStackTrace();
        }
    }

    /*

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


     */



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
                Toast.makeText(webGameAct, strMsg, Toast.LENGTH_SHORT).show();
            }
        }
    };
}