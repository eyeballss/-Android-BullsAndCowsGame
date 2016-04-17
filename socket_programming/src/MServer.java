import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class MServer {
	private HashMap<String, DataOutputStream> clients;
	private ServerSocket serverSocket;
	private static int playerCount = 0;
	private Socket socket1, socket2;

	public static void main(String[] args) {

//		String msg = "game abc";
//        if(msg.substring(0,4).equals("game")) System.out.println(msg.substring(4).trim());
        
		new MServer().start();
	}

	public MServer() {
		clients = new HashMap<String, DataOutputStream>();

		// 여러 스레드에서 접근할 것이므로 동기화
		Collections.synchronizedMap(clients);
	}

	public void start() {
		try {
			Socket socket;

			// 리스너 소켓 생성
			serverSocket = new ServerSocket(5000);
			System.out.println("서버가 시작되었습니다.");

			// 클라이언트와 연결되면
			while (true) {
				// 통신 소켓을 생성하고 스레드 생성(소켓은 1:1로만 연결된다)
				socket = serverSocket.accept();
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
			String name = "";
			try {
				// 클라이언트가 서버에 접속하면 대화방에 알린다.
				name = input.readUTF();
				// sendToAll("#" + name + "[" + socket.getInetAddress() + ":"
				// + socket.getPort() + "]" + "님이 대화방에 접속하였습니다.");
				sendToAll("[ info ] \"" + name + "\" is joined.");

				clients.put(name, output);
				System.out.println(
						name + "[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "님이 대화방에 접속하였습니다.");
				System.out.println("현재 " + clients.size() + "명이 대화방에 접속 중입니다.");

				// 메세지 전송
				String inputStr = null;
				while (input != null) {
					inputStr = input.readUTF();
					 System.out.println("------"+inputStr);
					// 게임 요청 메세지가 들어오면
					// if(inputStr.equals("play")){
					// System.out.println("성공");
					// new GameThrd(socket, clients.get(inputStr.substring(5)));
					// break; // 기존 쓰레드를 닫음.
					// }
					// else
					
					//p2p. 클라이언트가 적어서 보냄
					if(inputStr.startsWith("*")){
						inputStr=inputStr.substring(1);
						String oppoId = inputStr.substring(0, inputStr.indexOf("*"));
						inputStr=inputStr.substring(inputStr.indexOf("*")+1);
						
						System.out.println(oppoId+" 에게 별을 보냄");
						sendTo(oppoId, "*"+inputStr);
						sendTo(name, "*"+inputStr);
					}
					else if (inputStr.length() > 5 && inputStr.subSequence(0, 4).equals("game")) {
						// game 뒤쪽에 온 게 아이디면
						if (!inputStr.substring(4).trim().equals(name)
								&& clients.get(inputStr.substring(4).trim()) != null) {
							sendTo(inputStr.substring(4).trim(), "+game" + name);
						}
						// game 뒤쪽에 온 게 아이디가 아니면 그냥 전체 출력
						else
							sendToAll(inputStr);
					}

					else if (inputStr.length() > 5) {
						if (inputStr.substring(0, 5).equals("ngame")) {
							sendTo(name, "거절하셨습니다.");
							sendTo(inputStr.substring(5), "거절하셨습니다.");
						} else if (inputStr.substring(0, 5).equals("ygame")) {
							sendTo(name, "수락하셨습니다.");
							sendTo(inputStr.substring(5), "수락하셨습니다.");

						} else {
							sendToAll(inputStr);
						}
					}
					// 게임 요청 메세지가 아닌 모든 메세지는
					else {
						System.out.println("모두에게 보냅니다 : " + inputStr);
						sendToAll(inputStr);
					}
				}
			} catch (IOException e) {
			} finally {
				// 접속이 종료되면
				clients.remove(name);
				// sendToAll("#" + name + "[" + socket.getInetAddress() + ":"
				// + socket.getPort() + "]" + "님이 대화방에서 나갔습니다.");
				sendToAll("[ info ] \"" + name + "\" has left.");
				System.out.println(
						name + "[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "님이 대화방에서 나갔습니다.");
				System.out.println("현재 " + clients.size() + "명이 대화방에 접속 중입니다.");
			}
		}

		public void sendTo(String id, String message) {
			try {
				DataOutputStream dos = clients.get(id);
				dos.writeUTF(message);
			} catch (Exception e) {
			}
		}

		public void sendToAll(String message) {
			Iterator<String> it = clients.keySet().iterator();

			while (it.hasNext()) {
				try {
					DataOutputStream dos = clients.get(it.next());
					dos.writeUTF(message);
				} catch (Exception e) {
				}
			}
		}
	}

	// 승낙되면 스탠바이 메소드 불러서 함께 쓰레드를 만들자
	// 부르면 쓰레드를 멈추자. 그래서 전체 채팅방에서 나가게 하는거야.
	public void standByGame(Socket sock) {
		if (playerCount++ == 0) {
			socket1 = sock;
			System.out.println("player 1");
		} else if (playerCount++ == 1) {
			socket2 = sock;
			System.out.println("player 2");

			// 게임하는 두 사람의 쓰레드를 만듦.
			GameThrd game1 = new GameThrd(socket1, socket2);
			GameThrd game2 = new GameThrd(socket2, socket1);
			game1.start();
			game2.start();

			System.out.println("game Thread start");

			playerCount = 0; // 초기화
		}
	}

	class GameThrd extends Thread {
		Socket mySocket, oppoSocket;

		DataOutputStream myDos, oppoDos;
		DataInputStream myDis;

		// 생성자에서 미리 dos, dis를 만들어 둠
		public GameThrd(Socket sock1, Socket sock2) {
			System.out.println("game Thread!!");
			mySocket = sock1;
			oppoSocket = sock2;

			try {
				myDos = new DataOutputStream(mySocket.getOutputStream());
				oppoDos = new DataOutputStream(oppoSocket.getOutputStream());
				myDis = new DataInputStream(mySocket.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}

			// System.out.println("성공!");
			// mySocket = sock;
			//
			// try { // 내가 사용할 인아웃풋 스트림 형성
			// myDis = new DataInputStream(sock.getInputStream());
			// myDos = new DataOutputStream(sock.getOutputStream());
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			//
			// opponentDos = dos; // 상대방의 아웃풋 스트림
		} // 생성자 끝

		public void run() {

			try {
				while (myDis != null) {
					// 내가 받아온 것은 상대에게 바로 넘김
					String msg = myDis.readUTF();

					if (msg.startsWith("*")) {// 나한테서 온 것은 상대에게 보냄
						oppoDos.writeUTF(msg.substring(1));
					} else { // 상대에게서 온 것은 나에게 보냄
						myDos.writeUTF(msg);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();

			}
		}
	}

}