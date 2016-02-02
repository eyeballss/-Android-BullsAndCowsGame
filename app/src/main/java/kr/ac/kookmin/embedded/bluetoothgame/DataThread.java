package kr.ac.kookmin.embedded.bluetoothgame;
import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataThread extends Thread {

    enum TURN {
        FIRST, SECOND
    };

    private MainActivity mMain; //mainActivity
    private final BluetoothSocket mmSocket; // 클라이언트 소켓
    private InputStream mmInStream; // 입력 스트림
    private OutputStream mmOutStream; // 출력 스트림
    private ArrayAdapter<String> mChattingAdapter; //채팅창 어댑터
    private TextView mStatusMsg; //상태 메세지
    private String number =null; //null이면 내가 준비 아직 안 됨. 값이 있으면 내가 준비 끝남
    //    private int ready=0; //상대가 준비가 되었는지 확인. 0이면 아직 1이면 준비 됨. 2면 게임함
    private int turn=-1; //-1이면 초기값. 0이면 상대턴 1이면 내 턴

    private String myRPS = null; //내가 고른 가위바위보
    private String opponentRPS =null; //상대 가위바위보 결과

    public DataThread(BluetoothSocket socket, TextView mStatusMsg, ArrayAdapter<String> mChattingAdapter, MainActivity mainActivity) {
        this.mStatusMsg = mStatusMsg;
        this.mMain = mainActivity;
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

    // 소켓에서 수신된 데이터를 화면에 표시한다 <받음!!>
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                // 입력 스트림에서 데이터를 읽는다
                bytes = mmInStream.read(buffer);
                String strBuf = new String(buffer, 0, bytes);

                if(strBuf.startsWith("-")){//-로 시작하면 가위바위보인지 의심
                    String temp = strBuf.replaceAll("-","");
                    if(temp.equals("Rock") || temp.equals("Paper") || temp.equals("Scissors")){
                        opponentRPS=temp;

                        //상대도 정했고 나도 정했으면
                        if(myRPS!=null){
                            whoIsWinner(myRPS, opponentRPS);
                        }
                        continue;
                    }
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

    // 데이터를 소켓으로 전송한다 <보냄!!>
    public void write(String strBuf) {
        try {
            // 출력 스트림에 데이터를 저장한다
            byte[] buffer = strBuf.getBytes();
            mmOutStream.write(buffer);

            //가위바위보 결과는 보내지 않음
            if(strBuf.startsWith("-")) { //-로 시작하는 입력이 들어오면 가위바위보 결과일지도 모름
                String temp = strBuf.replaceAll("-", "");
                if (temp.equals("Rock") || temp.equals("Paper") || temp.equals("Scissors")){
                    myRPS = temp; //내 가위바위보 저장

                    //나도 정했고 상대도 정했으면
                    if(opponentRPS!=null){
                        whoIsWinner(myRPS, opponentRPS);
                    }

                    return;
                }
            }
            sendChatMsg("Send: " + strBuf);
            showMessage("Send: " + strBuf);
        } catch (IOException e) {
            showMessage("Socket write error");
        }
    }

    private void whoIsWinner(String myRPS, String opponentRPS) {

        //내가 바위일 때
        if(myRPS.equals("Rock")){
            if(opponentRPS.equals("Rock")){
                //비겼다고 말해주기
                showMessage("Draw!");
                rockPaperScissorsDialog();//비겼음
            }
            else if(opponentRPS.equals("Paper")){
                turn=0;
                showMessage("I'm lose.. It's not my turn.");
            }
            else{
                turn=1;
                showMessage("I'm win! It's my turn!");
            }
        }
        //내가 보일 때
        else if(myRPS.equals("Paper")){
            if(opponentRPS.equals("Rock")){
                turn=1;
                showMessage("I'm win! It's my turn!");
            }
            else if(opponentRPS.equals("Paper")){
                //비겼다고 말해주기
                showMessage("Draw!");
                rockPaperScissorsDialog();//비겼음
            }
            else{
                turn=0;
                showMessage("I'm lose.. It's not my turn.");
            }
        }
        //내가 가위일 때
        else {
            if(opponentRPS.equals("Rock")){
                turn=0;
                showMessage("I'm lose.. It's not my turn.");
            }
            else if(opponentRPS.equals("Paper")){
                turn=1;
                showMessage("I'm win! It's my turn!");
            }
            else{
                //비겼다고 말해주기
                showMessage("Draw!");
                rockPaperScissorsDialog();//비겼음
            }
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

//    //내 가위바위보를 저장
//    public void setMyRPS(String myRPS){
//        this.myRPS = myRPS.replaceAll("-","");
//    }

//    //내 가위바위보를 부름
//    public String getMyRPS(){
//        return myRPS;
//    }
//
//    //상대 가위바위보를 부름
//    public String getOpponentRPS(){
//        return opponentRPS;
//    }

//    //순서를 알고 싶을 때
//    public int getTurn(){
//        return turn;
//    }





    AlertDialog.Builder alt_bld;
    //가위바위보 알림창
    public void rockPaperScissorsDialog(){
        //초기화
        myRPS=null;
        opponentRPS=null;

        final CharSequence[] rockPaperScissors= {"Rock", "Paper", "Scissors"};
        alt_bld = new AlertDialog.Builder(mMain);
        //알림창의 속성들 설정
        alt_bld.setTitle("Select!"); //제목
        alt_bld.setCancelable(false); //취소 금지
        alt_bld.setItems(rockPaperScissors, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(mMain, rockPaperScissors[which], Toast.LENGTH_SHORT).show();
                String myRPS = "---" + rockPaperScissors[which] + "---";

                //내 가위바위보 결과를 보냄
                write(myRPS);
//                dialog.cancel();
            }
        });
        doAgain(""); //다시 합시다.
    }



    public void doAgain(String chatMsg){
        Message msg = Message.obtain(mReplayHdr, 0, chatMsg);
        mReplayHdr.sendMessage(msg);
        Log.d("Log", "Chatting contents : " + chatMsg);
    }
    Handler mReplayHdr = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                AlertDialog alert = alt_bld.create();
                alert.show();
            }
        }
    };





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
                mStatusMsg.setText(strMsg);
            }
        }
    };

}