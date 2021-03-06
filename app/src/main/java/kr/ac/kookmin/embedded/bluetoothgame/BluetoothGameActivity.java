package kr.ac.kookmin.embedded.bluetoothgame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BluetoothGameActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    static final int ACTION_ENABLE_BT = 625;
    // 접속시 사용하는 고유 ID
    static final UUID BLUE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    ListView mDeviceList; //블루투스 디바이스를 나타내는 리스트
    ArrayList<String> mDevicesInList; //블루투스 디바이스 저장하는 자료구조
    TextView mStatusMsg; //상태 표시 뷰
    BluetoothAdapter mBluetoothAdapter; //블루투스 어댑터
    ServerThread mServerThrd; //서버 쓰레드
    ClientThread mClientThrd; //클라이언트 쓰레드
    DataThread mDataThrd; //데이터 송수신 쓰레드

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_game);

        mStatusMsg = (TextView) findViewById(R.id.statusMsg);

        // ListView 초기화. 리스트 어댑터를 만들고 리스트의 객체를 만듦.
        initListView();

        // 블루투스 사용 가능상태 판단
        boolean isBluetoothAbleToUse = canUseBluetooth();

        //블루투스가 사용 가능하고 활성화 되어있으면
        if (isBluetoothAbleToUse) getParedDevice();
    }

    // 페어링된 원격 디바이스 목록 구하기
    public void getParedDevice() {

        //페어링 된 원격 디바이스가 구해지기 전에 서버 쓰레드 객체 생성
        if (mServerThrd != null) return;
        // 서버 소켓 접속을 위한 스레드 생성 & 시작
        mServerThrd = new ServerThread(mBluetoothAdapter, mStatusMsg, mServerThrd, this);
        Log.d("Log", "Made a server thread");
        mServerThrd.start();
        Log.d("Log", "started Server thread");

        // 블루투스 어댑터에서 페어링된 원격 디바이스 목록을 구한다
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        // 디바이스 목록에서 하나씩 추출
        for (BluetoothDevice device : devices) {
            // 디바이스를 목록에 추가
            addDeviceToList(device.getName(), device.getAddress());
        }
        Log.d("Log", "done to add pared devices into list");
    }

    // 디바이스를 ListView 에 추가
    public void addDeviceToList(String name, String address) {
        // ListView 와 연결된 ArrayList 에 새로운 항목을 추가
        String deviceInfo = " " + name + " - " + address;
        Log.d("Log", "Device Find: " + deviceInfo);
        mDevicesInList.add(deviceInfo);
        // 화면을 갱신한다
        ArrayAdapter adapter = (ArrayAdapter) mDeviceList.getAdapter();
        adapter.notifyDataSetChanged();
    }


    // 블루투스 사용 가능상태 판단
    public boolean canUseBluetooth() {
        // 블루투스 어댑터를 구한다
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 블루투스 어댑터가 null 이면 블루투스 장비가 존재하지 않는다.
        if (mBluetoothAdapter == null) {
            Log.d("Log", "Bluetooth is not available.");
            mStatusMsg.setText("Device not found. You can't use Bluetooth.");
            return false;
        }

        mStatusMsg.setText("Device is exist");
        // 블루투스 활성화 상태라면 함수 탈출
        if (mBluetoothAdapter.isEnabled()) {
            mStatusMsg.append(" and Bluetooth is usable now.");
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
                mStatusMsg.append(" and Bluetooth is usable.");
                // 페어링된 원격 디바이스 목록 구하기
//                getParedDevice();
            }
            // 사용자가 블루투스 활성화 취소했을때
            else {
//                statusMsg.append("and Device can not use");
                Toast.makeText(this, "You have to ture on Bluetooth.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //버튼들의 클릭 이벤트
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchBtn: {
                Log.d("Log", "Clicked search button.");

                Button sBtn = (Button) v;
                Log.d("Log", "Made a temporary search button view.");

                if (sBtn.getText().equals("Search")) { //Search 상태일 때 클릭
                    //연결 가능한 디바이스 검색
                    startFindDevice();
                    sBtn.setText("Stop");
                } else if (sBtn.getText().equals("Stop")) { //Stop 상태일 때 클릭
                    //검색 중지
                    stopFindDevice();
                    sBtn.setText("Search");
                }
                break;
            }
            case R.id.fineMeBtn: {
                Log.d("Log", "Clicked find me button.");

                setDiscoverable();
                break;
            }

        }
    }

    // 원격 디바이스 검색 시작
    public void startFindDevice() {
        // 원격 디바이스 검색 중지
        stopFindDevice();
        // 디바이스 검색 시작
        mBluetoothAdapter.startDiscovery();
        // 원격 디바이스 검색 이벤트 리시버 등록
        registerReceiver(mBlueRecv, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    // 디바이스 검색 중지
    public void stopFindDevice() {
        // 현재 디바이스 검색 중이라면 취소한다
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            // 브로드캐스트 리시버를 등록 해제한다
            unregisterReceiver(mBlueRecv);
        }
    }

    // 다른 디바이스에게 자신을 검색 허용
    public void setDiscoverable() {
        // 현재 검색 허용 상태라면 함수 탈출
        if (mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            return;
        // 다른 디바이스에게 자신을 검색 허용 지정
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
        startActivity(intent);
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


    // ListView 초기화
    public void initListView() {
        // 어댑터 생성
        mDevicesInList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDevicesInList);
        // ListView 에 어댑터와 이벤트 리스너를 지정
        mDeviceList = (ListView) findViewById(R.id.deviceList);
        mDeviceList.setAdapter(adapter);
        mDeviceList.setOnItemClickListener(this); //onItemClick 메소드 호출
        Log.d("Log", "List is initialized.");
    }

    //리스트의 아이템이 클릭되었을 때
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //다음 액티비티를 호출해야겠지!
        // 사용자가 선택한 항목의 내용을 구한다
        String strItem = mDevicesInList.get(position); //클릭한 아이템을 얻음

        // 사용자가 선택한 디바이스의 주소를 구한다
        int pos = strItem.indexOf(" - ");
        if (pos <= 0) return;
        String address = strItem.substring(pos + 3);
        mStatusMsg.setText("Sel Device: " + address);

        // 디바이스 검색 중지
        stopFindDevice();
        // 서버 소켓 스레드 중지
        mServerThrd.cancel();
        mServerThrd = null;

        if (mClientThrd != null) return;
        // 상대방 디바이스를 구한다
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // 클라이언트 소켓 스레드 생성 & 시작

        mClientThrd = new ClientThread(device, mStatusMsg, mServerThrd, this);
        mClientThrd.start();
    }


    // 앱이 종료될 때 디바이스 검색 중지
    public void onDestroy() {
        super.onDestroy();
        // 디바이스 검색 중지
        stopFindDevice();

        // 스레드를 종료
        if (mClientThrd != null) {
            mClientThrd.cancel();
            mClientThrd = null;
        }
        if (mServerThrd != null) {
            mServerThrd.cancel();
            mServerThrd = null;
        }
        if (mDataThrd != null)
            mDataThrd = null;
    }



    public void gameStart(BluetoothSocket socket){

        //레이아웃 visibility를 바꿔주기 위함
        LinearLayout firstLayout = (LinearLayout)findViewById(R.id.firstLayout);
        LinearLayout secondLayout = (LinearLayout)findViewById(R.id.secondLayout);
        final EditText mEditxtForChat = (EditText)findViewById(R.id.editxtForChat);
        Button mSendBtn = (Button)findViewById(R.id.sendBtn); //Send 버튼
        final Button mReadyBtn = (Button)findViewById(R.id.readyBtn); //Ready 버튼
        final Button mHideBtn = (Button) findViewById(R.id.hideBtn); //Hide 버튼
        Button mStartBtn = (Button)findViewById(R.id.startBtn); //Start 버튼
        final EditText oneNum=(EditText)findViewById(R.id.oneNum); //첫번째 숫자
        final EditText twoNum=(EditText)findViewById(R.id.twoNum); //두번째 숫자
        final EditText threeNum=(EditText)findViewById(R.id.threeNum); //세번째 숫자
        final EditText fourNum=(EditText)findViewById(R.id.fourNum); //네번째 숫자
        final String[] number = new String[1]; //자기가 정한 숫자

        //야구공 움직여서 정답판 움직이기
        final EditText mAnswerTxt = (EditText)findViewById(R.id.answerTxt);
        ImageView mBallImg = (ImageView)findViewById(R.id.ballImg);
        final ImageView mBatImg = (ImageView)findViewById(R.id.batImg);

        //visibility를 바꿈. 이제 secondLayer 위에서 놀게 됨.
        firstLayout.setVisibility(View.GONE);
        secondLayout.setVisibility(View.VISIBLE);

        //채팅 리스트 객체 만들고 어댑터 적용
        ListView mChattingList = (ListView)findViewById(R.id.chattingList);
        ListView mHistoryList = (ListView)findViewById(R.id.historyList);
        final ArrayAdapter<String> mChattingAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_items);
        final ArrayAdapter<String> mHistoryAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_items);
        mChattingList.setAdapter(mChattingAdapter);
        mHistoryList.setAdapter(mHistoryAdapter);
        //내 턴인걸 알리는 말풍선
        ImageView mSpeechBubbleImg = (ImageView)findViewById(R.id.speechBubbleImg);
        //데이터 송수신 쓰레드 만듦
        mDataThrd = new DataThread(socket, mStatusMsg, mChattingAdapter, mHistoryAdapter, this, mSpeechBubbleImg);
        mDataThrd.start();




//        //send 버튼을 누르면
//        mSendBtn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//
//                //송수신 쓰레드가 null이면 아무 일 안함
//                if (mDataThrd == null) return;
//
//                if (mEditxtForChat.getText().length() > 0) {
//                    mDataThrd.write(mEditxtForChat.getText().toString());
//                    mEditxtForChat.setText("");
//                }
//            }
//        });

        //Ready 버튼을 누르면
        mReadyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //숫자를 다 안 넣었을 때
                if (oneNum.getText().toString().equals("") || twoNum.getText().toString().equals("") ||
                        threeNum.getText().toString().equals("") || fourNum.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), "Fill all numbers.", Toast.LENGTH_SHORT).show();
                    mDataThrd.setReady(null);
                    return;
                }

                HashSet<Integer> numbers = new HashSet<Integer>();
                numbers.add(Integer.parseInt(oneNum.getText().toString()));
                numbers.add(Integer.parseInt(twoNum.getText().toString()));
                numbers.add(Integer.parseInt(threeNum.getText().toString()));
                numbers.add(Integer.parseInt(fourNum.getText().toString()));

                if (numbers.size() != 4) { //중복된 숫자가 들어갔을 때
                    Toast.makeText(getApplicationContext(), "You have to use different numbers each other.", Toast.LENGTH_SHORT).show();
                    mDataThrd.setReady(null);
                    return;
                } else { //준비가 되었을 때
                    Toast.makeText(getApplicationContext(), "I'm ready!", Toast.LENGTH_SHORT).show();

                    //준비 되었으면 가위바위보 알림창을 띄움
                    mDataThrd.rockPaperScissorsDialog(false);

                    mReadyBtn.setVisibility(View.GONE); //다시 숫자를 세팅 못하도록 버튼을 없애버림
                    oneNum.setVisibility(View.GONE); //숫자 바꾸지 못하게 없애버림
                    twoNum.setVisibility(View.GONE);
                    threeNum.setVisibility(View.GONE);
                    fourNum.setVisibility(View.GONE);
//                    oneNum.setKeyListener(null); //숫자 바꾸지 못하게 막음
//                    twoNum.setKeyListener(null);
//                    threeNum.setKeyListener(null);
//                    fourNum.setKeyListener(null);

                    number[0] = oneNum.getText().toString() + twoNum.getText().toString() + threeNum.getText().toString() + fourNum.getText().toString();

                    mHideBtn.setText(number[0]); // Hide 버튼의 텍스트를 숫자로 고침
                    mDataThrd.setReady(number[0]);


                }

            }
        });

        //Start 버튼을 누르면
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                //내가 준비가 안 되어있을 때
//                if(mDataThrd.getReady()==null) {
//                    Toast.makeText(getApplicationContext(), "I'm not ready yet.", Toast.LENGTH_SHORT).show();
//                    return;
//                }

//                //상대가 준비가 안 되면 & 디폴트
//                if(mDataThrd.askReady()==0) {
//                    mDataThrd.write("Are you ready?");
//                }
//                //상대가 준비가 되면
//                else if(mDataThrd.askReady()==1){
//                    mDataThrd.write("Game Start!");
//                    if(mDataThrd.getTurn()==1){
//                        mDataThrd.write("It's my turn.");
//                    }
//                    mDataThrd.play();//게임 시작
//                }
            }
        });

        //Hide 버튼을 누르면 정했던 숫자와 Hide 단어를 번갈아가면서 보여줌
        mHideBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mHideBtn.getText().toString().equals("Hide")) {
                    mHideBtn.setText(mDataThrd.getReady());
                } else {
                    mHideBtn.setText("Hide");
                }
            }
        });

        //Send 버튼 클릭 리스너 다시 만듦
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //채팅 먼저 처리함
                //송수신 쓰레드가 null이면 아무 일 안함
                if (mDataThrd == null) return;

                if (mEditxtForChat.getText().length() > 0) {
                    mDataThrd.write(mEditxtForChat.getText().toString());
                    mEditxtForChat.setText("");
                }

                //--//

            }
        });




        //야구공 움직여서 정답판 움직이기
        mAnswerTxt.setVisibility(View.VISIBLE);
        mBallImg.setVisibility(View.VISIBLE);
        mBatImg.setVisibility(View.VISIBLE);
        mAnswerTxt.bringToFront();

        //방망이 움직이는 리스너
        mBatImg.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                FrameLayout.LayoutParams imgParams = (FrameLayout.LayoutParams) v.getLayoutParams();
                if (v.getId() != R.id.batImg) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        imgParams.topMargin = (int) event.getRawY() - v.getHeight() - 50;
                        imgParams.leftMargin = (int) event.getRawX() - (v.getWidth() / 2) - 50;
                        v.setLayoutParams(imgParams);
                        break;
                }
                return false;
            }
        });

        //야구공으로 정답판 움직이는 리스너
        mBallImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
//                if (currentState != State.EDIT_MOVE) return false;

                FrameLayout.LayoutParams imgParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                FrameLayout.LayoutParams txtParams = (FrameLayout.LayoutParams) mAnswerTxt.getLayoutParams();
                if (view.getId() != R.id.ballImg) return false;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        imgParams.topMargin = (int) event.getRawY() - view.getHeight() - 50;
                        imgParams.leftMargin = (int) event.getRawX() - (view.getWidth() / 2) - 50;
                        view.setLayoutParams(imgParams);
                        txtParams.topMargin = (int) event.getRawY() - mAnswerTxt.getHeight() - 50 - 50;
                        txtParams.leftMargin = (int) event.getRawX() - (mAnswerTxt.getWidth() / 2) - 50;
                        mAnswerTxt.setLayoutParams(txtParams);
                        break;

//                    case MotionEvent.ACTION_UP:
//                        imgParams.topMargin = (int) event.getRawY() - view.getHeight() - 50;
//                        imgParams.leftMargin = (int) event.getRawX() - (view.getWidth() / 2) - 50;
//                        view.setLayoutParams(imgParams);
//                        txtParams.topMargin = (int) event.getRawY() - mAnswerTxt.getHeight() - 50 - 50;
//                        txtParams.leftMargin = (int) event.getRawX() - (mAnswerTxt.getWidth() / 2) - 50;
//                        mAnswerTxt.setLayoutParams(txtParams);
//                        break;

//                    case MotionEvent.ACTION_DOWN:
//                        view.setLayoutParams(imgParams);
//                        mAnswerTxt.setLayoutParams(txtParams);
//                        break;
                }

                return true;
            }
        }); //여기까지 야구공 움직이기


        final HashSet<String> answerNumSet = new HashSet<String>();
        //야구방망이를 누르면
        mBatImg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                //내 턴이고 정답판에 뭔가 있을 때
                if (mAnswerTxt.getText().length() > 0 && mDataThrd.checkTurn()) {
                    //정답판 판독
                    for (int i = 0; i < mAnswerTxt.length(); i++) {
                        answerNumSet.add(String.valueOf(mAnswerTxt.getText().charAt(i)));
                    }
                    if (answerNumSet.size() != 4) {
                        Toast.makeText(getApplicationContext(), "Try correct numbers : " + mAnswerTxt.getText(), Toast.LENGTH_SHORT).show();
                    } else {
                        mDataThrd.throwBall(String.valueOf(mAnswerTxt.getText())); //mDataThrd로 넘겨서 게임 확인
                        Toast.makeText(getApplicationContext(), "throw " + mAnswerTxt.getText(), Toast.LENGTH_SHORT).show();
                        mAnswerTxt.setText("");
                    }
                    answerNumSet.clear(); //셋을 정리해준다.
                }


            }
        });
    }


//    //가위바위보 알림창
//    private void rockPaperScissorsDialog(){
//        final CharSequence[] rockPaperScissors= {"Rock", "Paper", "Scissors"};
//        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
//        //알림창의 속성들 설정
//        alt_bld.setTitle("Select!"); //제목
//        alt_bld.setCancelable(false); //취소 금지
//        alt_bld.setItems(rockPaperScissors, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(getApplicationContext(), rockPaperScissors[which], Toast.LENGTH_SHORT).show();
//                String myRPS="---"+rockPaperScissors[which]+"---";
//
//                //내 가위바위보 결과를 보냄
//                mDataThrd.write(myRPS);
////                dialog.cancel();
//            }
//        });
//        AlertDialog alert = alt_bld.create();
//        alert.show();
//    }
}


//재시합 하게 해주세요.
//야구방망이
//두 개로 나눠봄
//스크롤 ..어떻게 해보세요.