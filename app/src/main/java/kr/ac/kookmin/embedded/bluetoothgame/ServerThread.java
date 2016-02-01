package kr.ac.kookmin.embedded.bluetoothgame;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;

// 서버 소켓을 생성해서 접속이 들어오면 클라이언트 소켓을 생성하는 스레드
public class ServerThread extends Thread {

    static final String BLUE_NAME = "Bulls And Cows";  // 접속시 사용하는 이름
    // 접속시 사용하는 고유 ID
    static final UUID BLUE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    //    DataThread mDataThrd; //데이터 송수신 쓰레드
    ServerThread mServerThrd; //서버 쓰레드
    private TextView mStatusMsg;
    private BluetoothServerSocket mmSSocket;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket cSocket;
    private MainActivity main;

    // 서버 소켓 생성
    public ServerThread(BluetoothAdapter mBluetoothAdapter, TextView mStatusMsg, ServerThread mServerThrd, MainActivity main) {
        this.main = main;
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.mStatusMsg = mStatusMsg;
        this.mServerThrd = mServerThrd;

        try {
            mmSSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(BLUE_NAME, BLUE_UUID);
        } catch (IOException e) {
            sendStatusMsg("Get Server Socket Error");
        }
    }

    public void run() {
        cSocket = null;

        // 원격 디바이스에서 접속을 요청할 때까지 기다린다
        try {
            cSocket = mmSSocket.accept();
        } catch (IOException e) {
            sendStatusMsg("Socket Accept Error");
            return;
        }

        // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
//        onConnected(cSocket);
//        sendStatusMsg("Socket connected");
        gameCallHandler("Socket connected");
    }


    // 서버 소켓 중지
    public void cancel() {
        try {
            mmSSocket.close();
        } catch (IOException e) {
            sendStatusMsg("Server Socket close error");
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
//
//    }


    //핸들러를 이용하여 메인에 있는 gameStart 메소드를 호출
    public void gameCallHandler(String strMsg) {
        Message msg = Message.obtain(mTest, 0, strMsg);
        mTest.sendMessage(msg);
    }
    Handler mTest= new Handler() {
        public void handleMessage(Message msg) {
            sendStatusMsg((String)msg.obj); //Socket connected
            main.gameStart(cSocket);
        }
    };

    // 메시지를 화면에 표시
    public void sendStatusMsg(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        mHandler.sendMessage(msg);
        Log.d("Log", "Message from server thread is : " + strMsg);
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