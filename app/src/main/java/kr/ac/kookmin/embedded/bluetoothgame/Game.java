package kr.ac.kookmin.embedded.bluetoothgame;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by kesl on 2016-01-28.
 */
public class Game {

    String one, two, three, four;
    EditText mChatText;
    Button mHideBtn, mSendBtn, mHistoryBtn;
    TextView mStatusMsg;
    ArrayList<String> mArDevice;
    ListView mListDevice;
    Thread mCThread, mSThread, mSocketThread;

    //생성자
    public Game(String oneNum, String twoNum, String threeNum, String fourNum, //숫자들
                Button mHideBtn, EditText mChatText, Button mSendBtn, Button mHistoryBtn,//숨기기버튼과 채팅창과 보내기 버튼과 히스토리 버튼
                TextView mStatusMsg, ArrayList<String> mArDevice, ListView mListDevice, // 상태창과 채팅창 블루투스 어댑터와 채팅창리스트뷰
                Thread mCThread, Thread mSThread, Thread mSocketThread // 클라이언트 소켓 접속 스레드, 서버 소켓 접속 스레드, 데이터 송수신 스레드
    ) {

        this.mChatText=mChatText;
        this.mSendBtn=mSendBtn;
/////////////////////////
        Handler mChatHdr = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    String chatMsg = (String) msg.obj;
                    addChatToList(chatMsg);
                }
            }
        };

        //채팅 내용을 리스트에 올림



/////////////////////////



    }

    public void addChatToList(String content) {
        mChatText.setVisibility(View.VISIBLE);
        mSendBtn.setVisibility(View.VISIBLE);
    }
}
