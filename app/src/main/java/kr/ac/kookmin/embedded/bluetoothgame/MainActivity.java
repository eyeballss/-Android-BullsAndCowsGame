package kr.ac.kookmin.embedded.bluetoothgame;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {
    static final int ACTION_ENABLE_BT = 101;
    static final String BLUE_NAME = "Bulls And Cows";  // 접속시 사용하는 이름
    // 접속시 사용하는 고유 ID
    static final UUID BLUE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    static int listMode=0; //리스트 모드가 0이면 블루투스 디바이스 리스트로 사용
                        //리스트 모드가 1이면 채팅창으로 사용(터치시 아무 이벤트도 안 발생)

    Button mSendBtn; //보내기 버튼
    Button mSearchBtn; // 검색, 중지 버튼
    Button mFindMeBtn; // 내 디바이스 검색 버튼
    TextView mTextMsg; // 상태 보여주는 뷰
    EditText mEditData; // 채팅 편집 뷰
    BluetoothAdapter mBA; //블루투스 어댑터
    ListView mListDevice; // 접속 가능한 블루투스 디바이스 리스트
    ArrayList<String> mArDevice; // 원격 디바이스 목록
    ClientThread mCThread = null; // 클라이언트 소켓 접속 스레드
    ServerThread mSThread = null; // 서버 소켓 접속 스레드
    SocketThread mSocketThread = null; // 데이터 송수신 스레드
    ScrollView mScrollViw = null; //키패드를 피해 올라가도록 만든 스크롤뷰

    //게임에 쓰이는 아이템들
    EditText oneNum=null, twoNum=null, threeNum=null, fourNum=null;
    int ready=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextMsg = (TextView) findViewById(R.id.textMessage);
        mEditData = (EditText) findViewById(R.id.editData);
        mSearchBtn = (Button) findViewById(R.id.searchBtn);
        mFindMeBtn = (Button) findViewById(R.id.fineMeBtn);
        mScrollViw = (ScrollView)findViewById(R.id.scrollView);
        oneNum=(EditText)findViewById(R.id.oneNum);
        twoNum=(EditText)findViewById(R.id.twoNum);
        threeNum=(EditText)findViewById(R.id.threeNum);
        fourNum=(EditText)findViewById(R.id.fourNum);

        // ListView 초기화
        initListView();


        // 블루투스 사용 가능상태 판단
        boolean isBlue = canUseBluetooth();
        if (isBlue)
            // 페어링된 원격 디바이스 목록 구하기
            getParedDevice();




        //채팅할 때 키패드와 함께 에딧텍스트도 올라가도록 만듦
//        mEditData.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if(hasFocus==true){
//                    mScrollViw.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            mScrollViw.smoothScrollBy(0,800);
//                        }
//                    },100);
//                }
//            }
//        });
    }


    // 페어링된 원격 디바이스 목록 구하기
    public void getParedDevice() {
        if (mSThread != null) return;
        // 서버 소켓 접속을 위한 스레드 생성 & 시작
        mSThread = new ServerThread();
        mSThread.start();

        // 블루투스 어댑터에서 페어링된 원격 디바이스 목록을 구한다
        Set<BluetoothDevice> devices = mBA.getBondedDevices();
//        mBA.startDiscovery();
        // 디바이스 목록에서 하나씩 추출
        for (BluetoothDevice device : devices) {
            // 디바이스를 목록에 추가
            addDeviceToList(device.getName(), device.getAddress());
        }

//        // 원격 디바이스 검색 시작
//        startFindDevice();

//        // 다른 디바이스에 자신을 노출
//        setDiscoverable();
    }


    // 디바이스를 ListView 에 추가
    public void addDeviceToList(String name, String address) {
        // ListView 와 연결된 ArrayList 에 새로운 항목을 추가
        String deviceInfo = name + " - " + address;
        Log.d("tag1", "Device Find: " + deviceInfo);
        mArDevice.add(deviceInfo);
        // 화면을 갱신한다
        ArrayAdapter adapter = (ArrayAdapter) mListDevice.getAdapter();
        adapter.notifyDataSetChanged();
    }



    // 블루투스 사용 가능상태 판단
    public boolean canUseBluetooth() {
        // 블루투스 어댑터를 구한다
        mBA = BluetoothAdapter.getDefaultAdapter();
        // 블루투스 어댑터가 null 이면 블루투스 장비가 존재하지 않는다.
        if (mBA == null) {
            mTextMsg.setText("Device not found");
            return false;
        }

        mTextMsg.setText("Device is exist");
        // 블루투스 활성화 상태라면 함수 탈출
        if (mBA.isEnabled()) {
            mTextMsg.append("\nDevice can use");
            return true;
        }

        // 사용자에게 블루투스 활성화를 요청한다
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, ACTION_ENABLE_BT);
        return false;
    }

    // 블루투스 활성화 요청 결과 수신
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_ENABLE_BT) {
            // 사용자가 블루투스 활성화 승인했을때
            if (resultCode == RESULT_OK) {
                mTextMsg.append("\nDevice can use");
                // 페어링된 원격 디바이스 목록 구하기
                getParedDevice();
            }
            // 사용자가 블루투스 활성화 취소했을때
            else {
                mTextMsg.append("\nDevice can not use");
                Toast.makeText(this, "블루투스를 켜야 합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }









    // ListView 항목 선택 이벤트 함수
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        // 사용자가 선택한 항목의 내용을 구한다
        if(listMode>0) return;
        String strItem = mArDevice.get(position); //클릭한 아이템을 얻음

        // 사용자가 선택한 디바이스의 주소를 구한다
        int pos = strItem.indexOf(" - ");
        if (pos <= 0) return;
        String address = strItem.substring(pos + 3);
        mTextMsg.setText("Sel Device: " + address);

        // 디바이스 검색 중지
        stopFindDevice();
        // 서버 소켓 스레드 중지
        mSThread.cancel();
        mSThread = null;

        if (mCThread != null) return;
        // 상대방 디바이스를 구한다
        BluetoothDevice device = mBA.getRemoteDevice(address);
        // 클라이언트 소켓 스레드 생성 & 시작
//        Toast.makeText(this, address+"!!!!!", Toast.LENGTH_LONG).show();
        mCThread = new ClientThread(device);
        mCThread.start();
    }


    // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
    public void onConnected(BluetoothSocket socket) {
        showMessage("Socket connected");

        //리스트뷰를 없애버림.
        new Thread()
        {
            public void run()
            {
                Message msg = removeLitHdr.obtainMessage();
                removeLitHdr.sendMessage(msg);
            }
        }.start();

        // 데이터 송수신 스레드가 생성되어 있다면 삭제한다
        if (mSocketThread != null)
            mSocketThread = null;
        // 데이터 송수신 스레드를 시작
        mSocketThread = new SocketThread(socket);
        mSocketThread.start();
    }

    //리스트뷰를 없애버리는 핸들러였으나 지금은 리스트뷰를 남겨두고 게임 모드로 넘어가게 함
    final Handler removeLitHdr = new Handler()
    {
        public void handleMessage(Message msg)
        {
            //서치버튼 사라져라~!
//            mSearchBtn.setVisibility(View.GONE);
            mSearchBtn.setText("Ready");
//            mFindMeBtn.setVisibility(View.GONE);
            mFindMeBtn.setText("History");

            initListView();
            listMode++;
//            gameStart();//게임을 시작합니다.
        }
    };

//    public void gameStart(){
//        oneNum=(EditText)findViewById(R.id.oneNum);
//        twoNum=(EditText)findViewById(R.id.twoNum);
//        threeNum=(EditText)findViewById(R.id.threeNum);
//        fourNum=(EditText)findViewById(R.id.fourNum);
//        mSendBtn=(Button)findViewById(R.id.sendBtn);
//
//        initGame();
//    }
//
//    public void initGame(){
//        initListView();
//        mEditData.setVisibility(View.VISIBLE);
////        oneNum.setFocusable(true);
////        twoNum.setClickable(true);
////        threeNum.setClickable(true);
////        fourNum.setClickable(true);
//        mSendBtn.setVisibility(View.VISIBLE);
//    }

    public void readyCheck(){
        //중복 숫자 검사
        new Thread()
        {
            public void run()
            {
                Message msg = startGame.obtainMessage();
                startGame.sendMessage(msg);
            }
        }.start();
    }

    final Handler startGame = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(!mSearchBtn.getText().equals("Ready")) return;
//            if(oneNum.getText()==null || twoNum.getText()==null || threeNum.getText()==null || fourNum.getText()==null)
//            Toast.makeText(getApplicationContext(), "숫자를 채우세요", Toast.LENGTH_SHORT).show();
            HashSet<String> readyTest = new HashSet<String>();
            readyTest.add(String.valueOf(oneNum.getText()));
            readyTest.add(String.valueOf(twoNum.getText()));
            readyTest.add(String.valueOf(threeNum.getText()));
            readyTest.add(String.valueOf(fourNum.getText()));
            if(readyTest.size()!=4){
                Toast.makeText(getApplicationContext(), "Input correct numbers!", Toast.LENGTH_SHORT).show();
            }
            else {
                mSearchBtn.setText("Hide");
                oneNum.setClickable(false);
                twoNum.setClickable(false);
                threeNum.setClickable(false);
                fourNum.setClickable(false);
                oneNum.setFocusable(false);
                twoNum.setFocusable(false);
                threeNum.setFocusable(false);
                fourNum.setFocusable(false);

                if (mSocketThread == null) return;
                // 사용자가 입력한 텍스트를 소켓으로 전송한다
                mSocketThread.write("준비되었습니다.");

                // 상대가 준비되었고 방금 내가 준비를 끝냈으면
                if(ready==2) {
                    gameStart();
                }
                // 상대 준비가 안되어있는데 내가 준비를 끝냈으면
                else ready=1;
            }
        }
    };

    private void gameStart(){
        //게임 클래스를 만들고 게임을 시작합니다. 이제 start에서 다 처리함.
        Game game = new Game(String.valueOf(oneNum.getText()), String.valueOf(twoNum.getText()), String.valueOf(threeNum.getText()), String.valueOf(fourNum.getText()), //숫자들
                mSearchBtn, mEditData, mSendBtn, mFindMeBtn,//레디 버튼과 채팅창과 보내기 버튼과 히스토리 버튼
                mTextMsg, mArDevice, mListDevice, // 상태창과 채팅창 블루투스 어댑터와 채팅창리스트뷰
                mCThread, mSThread, mSocketThread // 클라이언트 소켓 접속 스레드, 서버 소켓 접속 스레드, 데이터 송수신 스레드
        );
//                    game.start();
    }

    // 버튼 클릭 이벤트 함수. 모든 버튼이 여기 있음.
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchBtn:{
                if(mSearchBtn.getText().equals("Search")) {
                    //연결 가능한 디바이스 검색
                    startFindDevice();
                    mSearchBtn.setText("Stop");
                }
                else if(mSearchBtn.getText().equals("Stop")){
                    //검색 중지
                    stopFindDevice();
                    mSearchBtn.setText("Search");
                }
                else if(mSearchBtn.getText().equals("Ready")){
                    readyCheck(); //숫자 4개 다 맞는지 체크
                }
                break;
            }
            case R.id.fineMeBtn:{
                //내 디바이스 검색 허용.
                if(mFindMeBtn.getText().equals("Find me")) {
                    setDiscoverable();
                }
                else{
                    //여기다가 히스토리 다이얼로그 띄우기
                }
                break;
            }
            case R.id.sendBtn: { //Send 버튼을 누르면 에딧텍스트 내용을 보냄.
                // 데이터 송수신 스레드가 생성되지 않았다면 함수 탈출
                if (mSocketThread == null) return;
                // 사용자가 입력한 텍스트를 소켓으로 전송한다
                String strBuf = mEditData.getText().toString();
                if (strBuf.length() < 1) return;
                mEditData.setText("");
                mSocketThread.write(strBuf);
                break;
            }
            case R.id.refreshBtn: {
                onStop();
                onResume();
            }
        }
    }

    // 원격 디바이스 검색 이벤트 수신
    BroadcastReceiver mBlueRecv = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == BluetoothDevice.ACTION_FOUND) {
                // 인텐트에서 디바이스 정보 추출
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 페어링된 디바이스가 아니라면
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                    // 디바이스를 목록에 추가
                    addDeviceToList(device.getName(), device.getAddress());
            }
        }
    };











    // 앱이 종료될 때 디바이스 검색 중지
    public void onDestroy() {
        super.onDestroy();
        // 디바이스 검색 중지
        stopFindDevice();

        // 스레드를 종료
        if (mCThread != null) {
            mCThread.cancel();
            mCThread = null;
        }
        if (mSThread != null) {
            mSThread.cancel();
            mSThread = null;
        }
        if (mSocketThread != null)
            mSocketThread = null;
    }

    // 클라이언트 소켓 생성을 위한 스레드
    private class ClientThread extends Thread {
        private BluetoothSocket mmCSocket;

        // 원격 디바이스와 접속을 위한 클라이언트 소켓 생성
        public ClientThread(BluetoothDevice device) {
            try {
                mmCSocket = device.createInsecureRfcommSocketToServiceRecord(BLUE_UUID);
            } catch (IOException e) {
                showMessage("Create Client Socket error");
                return;
            }
        }

        public void run() {
            // 원격 디바이스와 접속 시도
            try {
                mmCSocket.connect();
            } catch (IOException e) {
                showMessage("Connect to server error");
                // 접속이 실패했으면 소켓을 닫는다
                try {
                    mmCSocket.close();
                } catch (IOException e2) {
                    showMessage("Client Socket close error");
                }
                return;
            }

            // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
            onConnected(mmCSocket);
        }

        // 클라이언트 소켓 중지
        public void cancel() {
            try {
                mmCSocket.close();
            } catch (IOException e) {
                showMessage("Client Socket close error");
            }
        }
    }

    // 서버 소켓을 생성해서 접속이 들어오면 클라이언트 소켓을 생성하는 스레드
    private class ServerThread extends Thread {
        private BluetoothServerSocket mmSSocket;

        // 서버 소켓 생성
        public ServerThread() {
            try {
                mmSSocket = mBA.listenUsingInsecureRfcommWithServiceRecord(BLUE_NAME, BLUE_UUID);
            } catch (IOException e) {
                showMessage("Get Server Socket Error");
            }
        }

        public void run() {
            BluetoothSocket cSocket = null;

            // 원격 디바이스에서 접속을 요청할 때까지 기다린다
            try {
                cSocket = mmSSocket.accept();
            } catch (IOException e) {
                showMessage("Socket Accept Error");
                return;
            }

            // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
            onConnected(cSocket);
        }

        // 서버 소켓 중지
        public void cancel() {
            try {
                mmSSocket.close();
            } catch (IOException e) {
                showMessage("Server Socket close error");
            }
        }
    }

    // 데이터 송수신 스레드
    private class SocketThread extends Thread {
        private final BluetoothSocket mmSocket; // 클라이언트 소켓
        private InputStream mmInStream; // 입력 스트림
        private OutputStream mmOutStream; // 출력 스트림

        public SocketThread(BluetoothSocket socket) {
            mmSocket = socket;

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
                    sendChatMsg("Receive: " + strBuf);
                    showMessage("Receive: " + strBuf);
                    SystemClock.sleep(1);

                    if(strBuf.equals("준비되었습니다.")){
                        //나는 준비 된 상황에서 상대 준비가 끝나면 게임 시작.
                        if(ready==1){
                            gameStart();
                       }
                       //나는 준비가 안 되어있는데 상대가 준비가 되면
                       else ready=2;
                    }

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
    }







    // 원격 디바이스 검색 시작
    public void startFindDevice() {
        // 원격 디바이스 검색 중지
        stopFindDevice();
        // 디바이스 검색 시작
        mBA.startDiscovery();
        // 원격 디바이스 검색 이벤트 리시버 등록
        registerReceiver(mBlueRecv, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    // 디바이스 검색 중지
    public void stopFindDevice() {
        // 현재 디바이스 검색 중이라면 취소한다
        if (mBA.isDiscovering()) {
            mBA.cancelDiscovery();
            // 브로드캐스트 리시버를 등록 해제한다
            unregisterReceiver(mBlueRecv);
        }
    }
    // 다른 디바이스에게 자신을 검색 허용
    public void setDiscoverable() {
        // 현재 검색 허용 상태라면 함수 탈출
        if (mBA.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            return;
        // 다른 디바이스에게 자신을 검색 허용 지정
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 200);
        startActivity(intent);
    }






    //채팅 메세지를 리스트뷰에 올림
    public void sendChatMsg(String chatMsg){
        Message msg = Message.obtain(mChatHdr, 0, chatMsg);
        mChatHdr.sendMessage(msg);
        Log.d("tag1", chatMsg);
    }
    // 메시지 화면 출력을 위한 핸들러
    Handler mChatHdr = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String chatMsg = (String) msg.obj;
                addChatToList(chatMsg);
            }
        }
    };

    //채팅 내용을 리스트에 올림
    public void addChatToList(String content) {
        Log.d("tag1", "Add '" + content + "' to List");
        mArDevice.add(content);
        ArrayAdapter adapter = (ArrayAdapter) mListDevice.getAdapter();
        adapter.notifyDataSetChanged();
    }



    // 메시지를 화면에 표시
    public void showMessage(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        mHandler.sendMessage(msg);
        Log.d("tag1", strMsg);
    }
    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String) msg.obj;
                mTextMsg.setText(strMsg);
            }
        }
    };

    // ListView 초기화
    public void initListView() {
        // 어댑터 생성
        mArDevice = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mArDevice);
        // ListView 에 어댑터와 이벤트 리스너를 지정
        mListDevice = (ListView) findViewById(R.id.listDevice);
        mListDevice.setAdapter(adapter);
        mListDevice.setOnItemClickListener(this);
    }
}