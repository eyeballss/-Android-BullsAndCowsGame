import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
 
public class MClient {
    private String name;
    private Socket socket;
    private String serverIp = "192.168.1.5";
    private static String opponentID=null;
 
    public static void main(String[] args) {
        new MClient().start();
    }
 
    public void start() {
        try {
            socket = new Socket(serverIp, 7777);
            System.out.println("서버와 연결되었습니다.");
            
            while(true){
            	
            	System.out.print("아이디를 입력하세요: ");
                name = new Scanner(System.in).nextLine();
                if(name.length()<3) System.out.println("아이디는 3글자 이상 넣어주세요.");
                else if(name.equals("game")) System.out.println("game을 아이디로 할 수 없습니다.");
                else break;
            }
 
            ClientReceiver clientReceiver = new ClientReceiver(socket);
            ClientSender clientSender = new ClientSender(socket);
             
            clientReceiver.start();
            clientSender.start();
        } catch (IOException e) {
        }
    }
 
    class ClientReceiver extends Thread {
        Socket socket;
        DataInputStream input;
    	DataOutputStream output;
 
        public ClientReceiver(Socket socket) {
            this.socket = socket;
            try {
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
            }
        }
 
        @Override
        public void run() {
        	String inputStr=null;
            while (input != null) {
                try {
                	inputStr = input.readUTF();
                	
                	//게임 초대 메세지라면
                	if(inputStr.length()>6 && inputStr.subSequence(0, 5).equals("+game")){
                		System.out.println(inputStr.substring(5)+"님께서 게임을 요청하셨습니다. 수락하시겠습니까?(y/n)");
                		opponentID = inputStr.substring(5);
                	}
                	//게임 초대 메세지가 아니라면
                	else System.out.println(inputStr);
                } catch (IOException e) {
                }
            }
        }
    }
 
    
    
    
    
    
    
    class ClientSender extends Thread {
        Socket socket;
        DataOutputStream output;
 
        public ClientSender(Socket socket) {
            this.socket = socket;
            try {
                output = new DataOutputStream(socket.getOutputStream());
                output.writeUTF(name);
                System.out.println("대화방에 입장하였습니다.");
            } catch (Exception e) {
            }
        }
 
        @Override
        public void run() {
            Scanner sc = new Scanner(System.in);
            String msg = "";
 
            while (output != null) {
                try {
                    msg = sc.nextLine();
                    if(msg.equals("exit"))
                        System.exit(0);
                    //game 요청 메세지라면 그냥 보내기
                    if(msg.length()>5 && msg.substring(0,4).equals("game"))
                        output.writeUTF(msg);
                    else if(opponentID != null){
                		//String answer = msg;//new Scanner(System.in).next();
                		if(msg.startsWith("y") || msg.startsWith("Y")) 
                			msg = "y";
                		else msg = "n";
                		output.writeUTF(msg+"game"+opponentID);
                		System.out.println(msg+"을 보냄");
                		opponentID=null;
                    }
                    else
                    	output.writeUTF("[" + name + "]" + msg);
                } catch (IOException e) {
                }
            }
        }
    }
}