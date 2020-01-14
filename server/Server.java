import java.io.*;
import java.net.*;
import java.util.*;


class PlayerData {
	public double x;
	public double y;
	public String socketID;
	public Socket socket;
	public DataOutputStream clientOutput;
	public DataInputStream clientInput;
	public Room currentRoom = null;

	public PlayerData(double x, double y, String socketID, Socket socket) throws IOException {
		this.x = x;
		this.y = y;
		this.socketID = socketID;
		this.socket = socket;
		clientOutput = new DataOutputStream(socket.getOutputStream());
		clientInput = new DataInputStream(socket.getInputStream());
	}
}


class Room extends Thread {

	List<PlayerData> players = new ArrayList<PlayerData>();
	int roomID;

	public Room(PlayerData player1, PlayerData player2, int roomID) {
		players.add(player1);
		players.get(0).currentRoom = this;
		players.add(player2);
		players.get(1).currentRoom = this;
		this.roomID = roomID;
	}
}


public class Server extends Thread{
	private int port = 8080;
	public ServerSocket serverSock = null; 
	int ID = 0;
	int roomID = 0;
	
	List<PlayerData> players = new ArrayList<PlayerData>();
	List<PlayerData> playersWithoutRooms = new ArrayList<PlayerData>();

	public static void main(String args[]) {
	   System.out.println("Hit control-c to exit the server.");
	   Server server = new Server();
	   server.start();
	   server.server();
	   try {
			server.serverSock.close();
	   } catch (Exception e) {}
	}

	private void server() {
		try {
			InetAddress serverAddr = InetAddress.getByName(null);            
	   		serverSock = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println(e.getMessage() + " failed to create server socket ");
			return;
		}
		while (true) {
		   try {
				Socket socket;
				synchronized(serverSock) {
					socket = serverSock.accept();
				}
				PlayerData newPlayer = new PlayerData(100, 100, Integer.toString(ID), socket);
				players.add(newPlayer);
				playersWithoutRooms.add(newPlayer);
				System.out.println("Player " + Integer.toString(ID) + " connected");
				ID++;
				ClientHandler newHandler = new ClientHandler(newPlayer, this);
				newHandler.start();
				if (playersWithoutRooms.size() >= 2) {
					PlayerData player1 = playersWithoutRooms.get(0);
					PlayerData player2 = playersWithoutRooms.get(1);
					playersWithoutRooms.remove(player1);
					playersWithoutRooms.remove(player2);
					Room newRoom = new Room(player1, player2, roomID++);
					newRoom.start();
				}
			} catch (IOException e) {
				System.out.println(e.getMessage() + ", failed to connect to client.");
			} 
		}	
	}


	public void broadcast(String s) {
		DataOutputStream dataOut = null;
		for (PlayerData player : players) {
			dataOut = (DataOutputStream)(player.clientOutput);
			try { 
				dataOut.writeUTF(s); 
			} catch (IOException x) {
				System.out.println(x.getMessage() + ": Failed to broadcast to client.");
			}
		}
	}


	public PlayerData getPlayerByID(String ID) {
		for (PlayerData player : players) {
			if (player.socketID == ID)
				return player;
		}
		return null;
	}
	
}


class ClientHandler extends Thread {
	private Server server;
	private PlayerData player;
	double wallLeftX = 29.918842;
	double wallUpY = 312.8834;
	double wallRightX = 583.09235;
	double wallDownY = 36.297974;


	ClientHandler(PlayerData player, Server server) throws IOException {
		this.player = player;
		this.server = server;
	}

	double validateX(double x) {
		if (x < wallLeftX)
			return wallLeftX;
		else if (x > wallRightX)
			return wallRightX;
		return x;
	}

	double validateY(double y) {
		if (y < wallDownY)
			return wallDownY;
		else if (y > wallUpY)
			return wallUpY;
		return y;
	}

	@Override
	public void run()  {
		while (true) {
			try {
				String command = player.clientInput.readUTF();
				System.out.println(command);
				if (command.startsWith("CONNECT")) {
					while (true) {
						if (player.currentRoom != null) {
							String id = player.socketID;
							String x = Double.toString(player.x);
							String y = Double.toString(player.y);
							player.clientOutput.writeUTF("CREATED " + id + " " + x + " " + y);
							break;
						} else Thread.sleep(100);
					}
				} else if (command.startsWith("PLAYERMOVED")) {
					String x = command.split(" ")[1];
					String y = command.split(" ")[2];
					player.x = validateX(Double.parseDouble(x));
					player.y = validateY(Double.parseDouble(y));
				} else if (command.startsWith("DISCONNECT")) {
					System.out.println("Player " + player.socketID + " has disconnected");
					server.broadcast("REMOVEPLAYER " + player.socketID);
					break;
				} else if (command.startsWith("UPDATE")) {
					if (player.currentRoom != null) {
						for (PlayerData enemy : player.currentRoom.players) {
							player.clientOutput.writeUTF("PLAYER_UPDATE " + enemy.socketID + " " + Double.toString(enemy.x) + " " + Double.toString(enemy.y));
						}
					}
				}
			} catch (Exception e) {
				server.players.remove(player);
				System.out.println("Player " + player.socketID + " has disconnected");
				break;
			} 
		}
	}
	
	

}