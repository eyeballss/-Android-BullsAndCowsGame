import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class TestServer {
    private HashMap<String, DataOutputStream> clients;
	private int player =0;
    private ServerSocket serverSocket;
 
    public static void main(String[] args) {
        new MServer().start();
    }
 
    public void start() {
        try {
            Socket socket;
 
            // 리스너 소켓 생성
            serverSocket = new ServerSocket(5001);
            System.out.println("서버가 시작되었습니다.");
 
            // 클라이언트와 연결되면
            while (player<2) {
                // 통신 소켓을 생성하고 스레드 생성(소켓은 1:1로만 연결된다)
                socket = serverSocket.accept();
                player++;
                ServerReceiver receiver = new ServerReceiver(socket);
                receiver.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    class ServerReceiver extends Thread {
        Socket socket;
        DataInputStream input;
        DataOutputStream output;
    	String name;
 
        public ServerReceiver(Socket socket) {
            this.socket = socket;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
            }
        }
 
        @Override
        public void run() {
            try {
                // 클라이언트가 서버에 접속하면 대화방에 알린다.
            	if(clients.size()==0) name="one";
            	else name = "two";
                clients.put(name, output);
                
                // 메세지 전송
                String inputStr=null;
                while (input != null) {
                	
                	inputStr = input.readUTF();
                	//받고 난 후에는?

                	String id;
                	if(name.equals("one")) id="two";
                	else id="one";
                	
                }
            } catch (IOException e) {
            } 
        }
 
        public void sendTo(String message){
            try {

                DataOutputStream dos = clients.get(id);
                dos.writeUTF(message);
            } catch (Exception e) {
            }
        }
        
//        public void sendTo(String message, String id) {
// 
//                try {
//                    DataOutputStream dos = clients.get(clients.get(id));
//                    dos.writeUTF(message);
//                } catch (Exception e) {
//                
//            }
//        }
    }
}