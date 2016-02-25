package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class WConnectThread extends Thread {
    WebGameActivity webGameAct;
    ArrayAdapter<String> wChattingAdapter;
    int portNum;
    String ipAddr, userId, opponentID;

    Socket socket;
    DataOutputStream dos;
    DataInputStream dis;

    ClientReceiver clientReceiver;
    ClientSender clientSender;

    boolean admit=false;

    //생성자
    public WConnectThread(ArrayAdapter<String> adapter, String ip, int port, WebGameActivity act, String id) {
        Log.d("Acting", "쓰레드 생성자 시작");
        wChattingAdapter = adapter;
        ipAddr = ip.toString();
        portNum = port;
        webGameAct = act;
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

            clientReceiver = new ClientReceiver();
            clientSender = new ClientSender();

            clientReceiver.start();
            clientSender.start();


            Log.d("Acting", "client Receiver, Sender를 생성");


            //서버에서 아이디 먼저 원하므로 아이디를 우선 보내줌.
//                dos.writeUTF(userId);
//                dos.flush();
//                Log.d("Acting", "서버로 아이디 먼저 보냄");


//            while (true) {
//                String obj = dis.readUTF();
//                sendChatMsg(obj);
//                Log.d("Acting", "데이터를 서버에서 받음");
//
//                if (obj.equals("exit")) {
//                    break;
//                }
//            }
//
//            socket.close();


        } catch (Exception e) {
            Log.d("Exception", "WConnectThread run 에러");
            e.printStackTrace();
        }
    }



    //콜백메소드 불러주는 녀석
    private void sendCallback(String msg) {
        Intent intent = new Intent("my-event");
        // add data
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(webGameAct).sendBroadcast(intent);
    }







    /***********************************
     * 아래는 데이터를 주고 받는 쓰레드
     * *********************************
     */

    //리시버 쓰레드
    class ClientReceiver extends Thread {

        public void run() {
            Log.d("Acting", "Client Receiver 시작");
            String inputStr = null;
            while (dis != null) {
                try {
                    inputStr = dis.readUTF();

                    //게임 초대장이라면
                    if(inputStr.equals("수락하셨습니다.")){
//                        sendCallback(userId+" : 수락하여서 콜백 메세지를 보냅니다.");
                        admit=true;
                    }
                    
//                    //수락 후 p2p 메세지라면
                    else if(inputStr.startsWith("*")) sendChatMsg(inputStr.substring(1));
                    else if (inputStr.length() > 6 && inputStr.subSequence(0, 5).equals("+game")) {
                        receiveGameRequestHdr(inputStr.substring(5)); //게임 받겠냐는 메세지를 띄움
                        opponentID=inputStr.substring(5);
                    }

//                    //게임 초대 메세지라면
//                    if(inputStr.length()>6 && inputStr.subSequence(0, 5).equals("+game")){
//                        System.out.println(inputStr.substring(5)+"님께서 게임을 요청하셨습니다. 수락하시겠습니까?(y/n)");
//                        opponentID = inputStr.substring(5);
//                    }
//                    //게임 초대 메세지가 아니라면
//                    else System.out.println(inputStr);

                    else sendChatMsg(inputStr);
                    Log.d("Acting", "데이터를 서버에서 받음");

                } catch (IOException e) {
                    Log.d("Exception", "Client Receiver run 에러");
                }
            }
        }
    }

    //센더 쓰레드
    class ClientSender extends Thread {
        String msg;

        public ClientSender() {
            try {
                Log.d("Acting", "Client Sender 생성자");
                dos.writeUTF(userId);
                dos.flush();
                Log.d("Acting", "서버로 아이디 먼저 보냄");
                sendChatMsg("[ info ] You have entered.");
            } catch (Exception e) {
                Log.d("Exception", "Client Sender 생성자 에러");
            }
        }

        @Override
        public void run() {
            Log.d("Acting", "Client Sender 시작");
            msg = null;

            while (dos != null) {
                try {
//                    if (msg.equals("exit"))
//                        System.exit(0);
//                    //game 요청 메세지라면 그냥 보내기
//                    if(msg.length()>5 && msg.substring(0,4).equals("game"))
//                        output.writeUTF(msg);
//                    else if(opponentID != null){
//                        //String answer = msg;//new Scanner(System.in).next();
//                        if(msg.startsWith("y") || msg.startsWith("Y"))
//                            msg = "y";
//                        else msg = "n";
//                        output.writeUTF(msg+"game"+opponentID);
//                        System.out.println(msg+"을 보냄");
//                        opponentID=null;
//                    }
//                    else {

                    //msg가 들어오면
                    if (msg != null) {

                        //게임 프로토콜 메세지라면
                        if(msg.length() > 5 && msg.substring(0, 4).equals("game") ||
                                msg.length()>6 && msg.substring(0,5).equals("ygame") ||
                                msg.length()>6 && msg.substring(0,5).equals("ngame")){
                            dos.writeUTF(msg);
                            dos.flush();
                        }else {
                            String p2p=((admit)?("*"+opponentID+"*"):("")); //p2p 로 보냄
                            dos.writeUTF(p2p+"[ " + userId + " ] " + msg);
                            dos.flush();
                            Log.d("Acting", "서버로 데이터 보냄");
                        }
                        msg = null; //다시 세팅
                    }

                } catch (IOException e) {
                    Log.d("Exception", "Client Sender run 에러");
                }
            }//while
        }

        //메세지를 정함.
        public void setMsg(String msgFromClient) {
            msg = msgFromClient.toString();
        }
    }

    //서버로 보낼 데이터를 저장. 센더에서 사용.
    public void write(String msg) {

        clientSender.setMsg(msg);
        if(msg.length()>5 && msg.substring(0,4).equals("game")) opponentID=msg.substring(4).trim();
        Log.d("Acting", "write에서 메세지 넘겨줌");
    }


    public void receiveGameRequest(final String opponentId) {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(webGameAct);
        alert_confirm.setMessage(opponentId + " wants to play with you.").setCancelable(false).setPositiveButton("admit",

                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {


                        write("ygame" + opponentId); //같이 하자 메세지를 보냄!
                    }
                }).setNegativeButton("deny",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        write("ngame" + opponentId);
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }


    /**************************
     * 아래는 핸들러
     * ************************
     */

    //게임 요청 메세지에 응답하기 위한 메소드
    public void receiveGameRequestHdr(String Msg) {
        Message msg = Message.obtain(mRecvGameHdr, 0, Msg);
        mRecvGameHdr.sendMessage(msg);
        Log.d("Log", "Chatting contents : " + Msg);
    }

    // 게임 요청 메세지에 응답하기 위한 핸들러
    Handler mRecvGameHdr = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String chatMsg = (String) msg.obj;
                receiveGameRequest(chatMsg);
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