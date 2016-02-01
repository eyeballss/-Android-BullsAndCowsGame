package kr.ac.kookmin.embedded.bluetoothgame;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;

// 클라이언트 소켓 생성을 위한 스레드
public class ClientThread extends Thread {

    // 접속시 사용하는 고유 ID
    static final UUID BLUE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    ServerThread mServerThrd; //서버 스레드
    //    DaataThread mDataThrd; //데이터 송수신 스레드
    private BluetoothSocket mmCSocket;
    private TextView mStatusMsg;
    private MainActivity main;

    // 원격 디바이스와 접속을 위한 클라이언트 소켓 생성
    public ClientThread(BluetoothDevice device, TextView mStatusMsg, ServerThread mServerThrd, MainActivity main) {

        this.main = main;
        this.mStatusMsg = mStatusMsg;
        this.mServerThrd = mServerThrd;

        try {
            mmCSocket = device.createInsecureRfcommSocketToServiceRecord(BLUE_UUID);
        } catch (IOException e) {
            sendStatusMsg("Create Client Socket error");
            return;
        }
    }

    public void run() {
        // 원격 디바이스와 접속 시도
        try {
            mmCSocket.connect();
        } catch (IOException e) {
            sendStatusMsg("Connect to server error");
            // 접속이 실패했으면 소켓을 닫는다
            try {
                mmCSocket.close();
            } catch (IOException e2) {
                sendStatusMsg("Client Socket close error");
            }
            return;
        }

        // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
//        onConnected(mmCSocket);
        gameCallHandler("Socket connected");
    }

    // 클라이언트 소켓 중지
    public void cancel() {
        try {
            mmCSocket.close();
        } catch (IOException e) {
            sendStatusMsg("Client Socket close error");
        }
    }


//    // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
//    public void onConnected(BluetoothSocket socket) {
//        sendStatusMsg("Socket connected");
//
//        // 데이터 송수신 스레드를 시작
////        mDataThrd = new DataThread(socket);
////        mDataThrd.start();
//
////        main.gameStart();
//        gameCallHandler("");
//    }


    //핸들러를 이용하여 메인에 있는 gameStart 메소드를 호출
    public void gameCallHandler(String strMsg) {
        Message msg = Message.obtain(mTest, 0, strMsg);
        mTest.sendMessage(msg);
    }
    Handler mTest= new Handler() {
        public void handleMessage(Message msg) {
            sendStatusMsg((String)msg.obj); //Socket connected
            main.gameStart(mmCSocket);
        }
    };


    // 메시지를 화면에 표시
    public void sendStatusMsg(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        mHandler.sendMessage(msg);
        Log.d("Log", "Message from client thread is : " + strMsg);
    }
    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                mStatusMsg.setText(strMsg);
            }
        }
    };
}