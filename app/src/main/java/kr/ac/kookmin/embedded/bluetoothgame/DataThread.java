package kr.ac.kookmin.embedded.bluetoothgame;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataThread extends Thread {
    private final BluetoothSocket mmSocket; // 클라이언트 소켓
    private InputStream mmInStream; // 입력 스트림
    private OutputStream mmOutStream; // 출력 스트림
    private ArrayAdapter<String> mChattingAdapter; //채팅창 어댑터
    private TextView mStatusMsg; //상태 메세지
    private String number =null; //null이면 내가 준비 아직 안 됨. 값이 있으면 내가 준비 끝남
    private int ready=0; //상대가 준비가 되었는지 확인. 0이면 아직 1이면 준비 됨. 2면 게임함
    private int turn=0; //0이면 상대턴 1이면 내 턴

    public DataThread(BluetoothSocket socket, TextView mStatusMsg, ArrayAdapter<String> mChattingAdapter) {
        this.mStatusMsg = mStatusMsg;
        mmSocket = socket;
        this.mChattingAdapter=mChattingAdapter;

        // 입력 스트림과 출력 스트림을 구한다
        try {
            mmInStream = socket.getInputStream();
            mmOutStream = socket.getOutputStream();
        } catch (IOException e) {
            showMessage("Get Stream error");
        }
    }

    // 소켓에서 수신된 데이터를 화면에 표시한다
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                // 입력 스트림에서 데이터를 읽는다
                bytes = mmInStream.read(buffer);
                String strBuf = new String(buffer, 0, bytes);
                if(strBuf.equals("Are you ready?")){
                    sendChatMsg("Receive: " + strBuf);
                    if(number==null) {
                        strBuf="I'm not ready yet.";
                        ready=0;
                    }
                    else {
                        strBuf="I'm ready!";
                        ready=1;
                    }
                    write(strBuf);
                    continue;
                }
                sendChatMsg("Receive: " + strBuf);
                showMessage("Receive: " + strBuf);
                SystemClock.sleep(1);

            } catch (IOException e) {
                showMessage("Socket disconneted");
                break;
            }
        }
    }

    // 데이터를 소켓으로 전송한다
    public void write(String strBuf) {
        try {
            // 출력 스트림에 데이터를 저장한다
            byte[] buffer = strBuf.getBytes();
            mmOutStream.write(buffer);
            sendChatMsg("Send: " + strBuf);
            showMessage("Send: " + strBuf);
        } catch (IOException e) {
            showMessage("Socket write error");
        }
    }

    //준비 상황을 보여줌.
    public String getReady(){
        return number;
    }

    //내 상태를 저장
    public void setReady(String ready){
        number=ready;
    }

    //상대는 준비 되었니?
    public int askReady(){
        return ready;
    }

    //게임을 시작하자
    public void play(){
        ready=2;
    }






    //채팅 메세지를 리스트뷰에 올림
    public void sendChatMsg(String chatMsg){
        Message msg = Message.obtain(mChatHdr, 0, chatMsg);
        mChatHdr.sendMessage(msg);
        Log.d("Log", "Chatting contents : "+ chatMsg);
    }
    // 메시지 화면 출력을 위한 핸들러
    Handler mChatHdr = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String chatMsg = (String) msg.obj;
                mChattingAdapter.add(chatMsg);
                mChattingAdapter.notifyDataSetChanged();
            }
        }
    };


    // 메시지를 화면에 표시
    public void showMessage(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        mHandler.sendMessage(msg);
        Log.d("Log", strMsg);
    }
    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
//                mStatusMsg.setText(strMsg);
            }
        }
    };

}