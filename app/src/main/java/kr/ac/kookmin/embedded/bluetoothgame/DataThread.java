package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataThread extends Thread {

    enum TURN {
        FIRST, SECOND
    }

    private BluetoothGameActivity mMain; //mainActivity
    private final BluetoothSocket mmSocket; // 클라이언트 소켓
    private InputStream mmInStream; // 입력 스트림
    private OutputStream mmOutStream; // 출력 스트림
    private ArrayAdapter<String> mChattingAdapter; //채팅창 어댑터
    private ArrayAdapter<String> mHistoryAdapter; //게임 결과창 어댑터
    private TextView mStatusMsg; //상태 메세지
    private String myNumber = null; //null이면 내가 준비 아직 안 됨. 값이 있으면 내가 준비 끝남
    //    private int ready=0; //상대가 준비가 되었는지 확인. 0이면 아직 1이면 준비 됨. 2면 게임함
    private int turn = -1; //-1이면 초기값. 0이면 상대턴 1이면 내 턴

    private String myRPS = null; //내가 고른 가위바위보
    private String opponentRPS = null; //상대 가위바위보 결과

    private ImageView mSpeechBubbleImg; //턴을 알려주는 말풍선 그림

    public DataThread(BluetoothSocket socket, TextView mStatusMsg, ArrayAdapter<String> mChattingAdapter, ArrayAdapter<String> mHistoryAdapter, BluetoothGameActivity mainActivity, ImageView mSpeechBubbleImg) {
        this.mHistoryAdapter = mHistoryAdapter;
        this.mStatusMsg = mStatusMsg;
        this.mMain = mainActivity;
        mmSocket = socket;
        this.mChattingAdapter = mChattingAdapter;
        this.mSpeechBubbleImg = mSpeechBubbleImg;

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

                if (turn!=-1 && strBuf.length()==10 && strBuf.substring(0, 5).equals("throw") && strBuf.endsWith("!")) {//상대가 던지는 질문은
                    String result = computeBall(strBuf);
                    mmOutStream.write(result.getBytes());//계산한 결과를 돌려줌
                    turn = 1; //돌려줬으니 내 턴이 됨
                    changeSpeechBubble(true); //말풍선으로 알려줌.
                    sendToastMsg("My turn");
//                    Toast.makeText(mMain, "My turn", Toast.LENGTH_SHORT).show(); //토스트로 알려줌

                    if(result.indexOf("4 strike 0 ball!")>-1){ //내가 지는 스트라이크 4 나온 경우
                        sendGameResultMsg("Opponent's throw : "+myNumber);
                        setReady(null); //myNumber를 null로 고침.
                        turn=-1;
                        changeSpeechBubble(false); //말풍선으로 알려줌.
                        showMessage("I lose!");
                    }
                    else {
                        showMessage("My turn!");

                    }
                    continue;
                }

                if(strBuf.length()>10 && turn==1 && strBuf.substring(0,6).equals("return") && strBuf.endsWith("!")){//상대가 계산한 결과를 받음
                    sendGameResultMsg(strBuf.substring(6)); //나한테만 올려주면 됨.
                    turn=0; //결과를 받았으니 턴이 종료
                    changeSpeechBubble(false); //말풍선으로 알려줌.
//                    Toast.makeText(mMain, "Opponent turn", Toast.LENGTH_SHORT).show(); //토스트로 알려줌

                    if(strBuf.indexOf("4 strike 0 ball!")>-1){ //내가 이기는 스트라이크 4 나온 경우
                        setReady(null); //myNumber를 null로 고침
                        turn=-1;
                        changeSpeechBubble(true); //말풍선으로 알려줌.
                        showMessage("I win!");
                    }
                    else showMessage("Opponent turn!");
                    continue;
                }

                if (strBuf.length()>=2 && strBuf.startsWith("-") && strBuf.endsWith("-")) {//-로 시작하고 끝나면 가위바위보인지 의심
                    String temp = strBuf.replaceAll("-", "");
                    if (temp.equals("Rock") || temp.equals("Paper") || temp.equals("Scissors")) {
                        opponentRPS = temp;

                        //상대도 정했고 나도 정했으면
                        if (myRPS != null) {
                            whoIsWinner(myRPS, opponentRPS);
                        }
                        continue;
                    }
                }
//                if(strBuf.endsWith("?")) {//?로 끝나면 묻는 걸로 의심
//                }
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


//            if(strBuf.endsWith("?")) {//?로 끝나는 입력이 들어오면 질문일지도 모름
//                if(gameQuestion(strBuf)) { //질문인지 확인해서 맞으면 OutStream으로 보냄
//                    //true가 나오면 정식 질문이므로 return. false가 나오면 정식 질문이 아니므로 채팅창에 올림
//                    mmOutStream.write(("="+strBuf+"=").getBytes()); //=을 붙여서 보냄
//                    return;
//                }
//            }

            //가위바위보 결과는 보내지 않음
            if (strBuf.length()>=2 && strBuf.startsWith("-") && strBuf.endsWith("-")) { //-로 시작하고 끝나는 입력이 들어오면 가위바위보 결과일지도 모름
                String temp = strBuf.replaceAll("-", "");
                if (temp.equals("Rock") || temp.equals("Paper") || temp.equals("Scissors")) {
                    myRPS = temp; //내 가위바위보 저장

                    mmOutStream.write(buffer);//내 가위바위보 보냄
                    //나도 정했고 상대도 정했으면
                    if (opponentRPS != null) {
                        whoIsWinner(myRPS, opponentRPS);
                    }

                    return;
                }
            }
            mmOutStream.write(buffer);//일반 문자 보냄

            sendChatMsg("Send: " + strBuf);
            showMessage("Send: " + strBuf);
        } catch (IOException e) {
            showMessage("Socket write error");
        }
    }

//    //질문이 정식 질문인지 확인
//    private boolean gameQuestion(String question) {
//        question=question.replaceAll("'?'","").replaceAll("=","");
//
//        if(question.length()!=4) return false; //질문한 숫자가 4개가 아닐 때 리턴
//
//        try{
//            if(1234>Integer.parseInt(question) && Integer.parseInt(question)>9876)
//                return false; //질문한 숫자가 범위를 넘어가면 리턴
//        }
//        catch (Exception e){
//            return false; //quiestion이 숫자가 아니라서 Integer.parseInt 할 수 없다면 리턴
//        }
//
//        //해쉬셋 넣어야 함!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//
//
//        return true; //정식 질문임
////        try{
////            //암호화해서 보냄
////
////            return true;
////        }
////        catch (Exception e){
////            showMessage("Question send error");
////            return false;
////        }
//
////        HashSet<String> questionNums = new HashSet<String>();
////        for(int i=0; i<question.length(); i++)
////            questionNums.add(String.valueOf(question.charAt(i)));
//    }

    //내가 상대에게 공을 던짐
    public void throwBall(String ball) {
        try {
            mmOutStream.write(("throw" + ball + "!").getBytes());
        } catch (Exception e) {
            showMessage("Throw ball error");
        }
    }

    //상대가 던진 공을 내 것과 계산. 그래서 결과를 반환
    private String computeBall(String ballFromOpponent) {
        ballFromOpponent = ballFromOpponent.substring(5, ballFromOpponent.length()-1);

        String result = " : "; //결과가 될 녀석

        int strike = 0;
        int ball = 0;
        int out = 0;

        for (int i = 0; i < 4; i++) {
            // i번째 내 번호와 상대 넘버가 같은 경우 스트라이크
            if (String.valueOf(myNumber.charAt(i)).equals(String.valueOf(ballFromOpponent.charAt(i)))) {
                strike++;
            }
            // 상대 넘버가 내 번호에 있을 경우 볼
            else if (myNumber.indexOf(String.valueOf(ballFromOpponent.charAt(i))) >= 0) {
                ball++;
            }
            //이도 저도 아닌 경우 아웃
            else out++;
        }

        if(out==4) result+="out!"; //out인 경우
        else result += strike+" strike " +ball+" ball!";

        return "return"+ballFromOpponent + result;
    }

    //가위바위보 계산
    private void whoIsWinner(String myRPS, String opponentRPS) {

        //내가 바위일 때
        if (myRPS.equals("Rock")) {
            if (opponentRPS.equals("Rock")) {
                //비겼다고 말해주기
//                showMessage("Draw!");
                rockPaperScissorsDialog(true);//비겼음
            } else if (opponentRPS.equals("Paper")) {
                turn = 0;
//                showMessage("I lose.. It's not my turn.");
            } else {
                turn = 1;
//                showMessage("I win! It's my turn!");
            }
        }
        //내가 보일 때
        else if (myRPS.equals("Paper")) {
            if (opponentRPS.equals("Rock")) {
                turn = 1;
//                showMessage("I win! It's my turn!");
            } else if (opponentRPS.equals("Paper")) {
                //비겼다고 말해주기
//                showMessage("Draw!");
                rockPaperScissorsDialog(true);//비겼음
            } else {
                turn = 0;
//                showMessage("I lose.. It's not my turn.");
            }
        }
        //내가 가위일 때
        else {
            if (opponentRPS.equals("Rock")) {
                turn = 0;
//                showMessage("I lose.. It's not my turn.");
            } else if (opponentRPS.equals("Paper")) {
                turn = 1;
//                showMessage("I win! It's my turn!");
            } else {
                //비겼다고 말해주기
//                showMessage("Draw!");
                rockPaperScissorsDialog(true);//비겼음
            }
        }

        if(turn==1) {
            showMessage("I win! It's my turn!");
            changeSpeechBubble(true); //이긴 경우에만 말풍선 보이기
            sendToastMsg("My turn");
//            Toast.makeText(mMain, "My turn", Toast.LENGTH_SHORT).show(); //토스트로 알려줌
        }
        else if(turn==0){
            showMessage("I lose.. It's not my turn.");
            changeSpeechBubble(false);//진 경우엔 말풍선 숨기기
            sendToastMsg("Opponent turn");
//            Toast.makeText(mMain, "Opponent turn", Toast.LENGTH_SHORT).show(); //토스트로 알려줌
        }
        else{
            showMessage("Draw!");
            sendToastMsg("Draw!");
//            Toast.makeText(mMain, "Draw!", Toast.LENGTH_SHORT).show(); //토스트로 알려줌
        }

    }


    //준비 상황을 보여줌.
    public String getReady() {
        return myNumber;
    }

    //내 상태를 저장
    public void setReady(String ready) {
        myNumber = ready;
    }

    //턴 확인. false이면 상대턴. true이면 내 턴
    public boolean checkTurn() {
        if (turn == 1) return true;
        else return false;
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
    public void rockPaperScissorsDialog(boolean init) {

        if (init) { //비겨서 초기화 해야 하면
            //초기화
            myRPS = null;
            opponentRPS = null;
        }

        final CharSequence[] rockPaperScissors = {"Rock", "Paper", "Scissors"};
        alt_bld = new AlertDialog.Builder(mMain);
        //알림창의 속성들 설정
        alt_bld.setTitle("Select!"); //제목
        alt_bld.setCancelable(false); //취소 금지
        alt_bld.setItems(rockPaperScissors, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(mMain, rockPaperScissors[which], Toast.LENGTH_SHORT).show();
                String myRPSTemp = "-" + rockPaperScissors[which] + "-";

                write(myRPSTemp);

//                dialog.cancel();
            }
        });
        doAgain(""); //다시 합시다.
    }


    //비겼을 때
    public void doAgain(String chatMsg) {
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


    //상대 질문에 대한 결과 메세지를 리스트뷰에 올림. 히스토리 리스트
    public void sendGameResultMsg(String resultMsg) {
        Message msg = Message.obtain(mChatHdr, 0, resultMsg);
        mGameResultHdr.sendMessage(msg);
        Log.d("Log", "Game result : " + resultMsg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mGameResultHdr = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String chatMsg = (String) msg.obj;
                mHistoryAdapter.add(chatMsg);
                mHistoryAdapter.notifyDataSetChanged();
            }
        }
    };


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
                Toast.makeText(mMain, strMsg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    // 말풍선 출력
    public void changeSpeechBubble(boolean result) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mSpeechBubbleHandler , 0, result);
        mSpeechBubbleHandler.sendMessage(msg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mSpeechBubbleHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                boolean result = (boolean) msg.obj;
                if(turn==-1)//결판이 났을 때
                { //win, lose로 바꿔줌
                    mSpeechBubbleImg.setVisibility(View.VISIBLE);
                    if(result) mSpeechBubbleImg.setImageResource(R.drawable.winbubble);
                    else mSpeechBubbleImg.setImageResource(R.drawable.losebubble);
                }
                else { //아직 시합중일 때
                    if (result) mSpeechBubbleImg.setVisibility(View.VISIBLE);
                    else mSpeechBubbleImg.setVisibility(View.INVISIBLE);
                }
            }
        }
    };



}