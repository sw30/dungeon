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
	public Dungeon currentDungeon = null;
	public double attackRange = 20;
	public double maxHealth = 3.0;
	public double currentHealth = 3.0;

	public long lastAttack = -100;
	public double attackX;
	public double attackY;
	public int lastDirection;
	public long coolDown = -1001;


	public PlayerData(double x, double y, String socketID, Socket socket) throws IOException {
		this.x = x;
		this.y = y;
		this.socketID = socketID;
		this.socket = socket;
		clientOutput = new DataOutputStream(socket.getOutputStream());
		clientInput = new DataInputStream(socket.getInputStream());
	}

	public void attack(int direction) {
		lastDirection = direction;
		lastAttack = System.currentTimeMillis();
		attackX = x;
		attackY = y;
		if (direction == 0)
			attackX -= attackRange;
		else if (direction == 1)
			attackX += attackRange;
		else if (direction == 2) 
			attackY += attackRange;
		else if (direction == 3)
			attackY -= attackRange;
	}

	public void updateAttackXY() {
		attackX = x;
		attackY = y;
		if (lastDirection == 0)
			attackX -= attackRange;
		else if (lastDirection == 1)
			attackX += attackRange;
		else if (lastDirection == 2) 
			attackY += attackRange;
		else if (lastDirection == 3)
			attackY -= attackRange;
	}

	public boolean checkIfAttacked(double x, double y) {
		if (lastDirection == 0) {
			if (x >= attackX - attackRange && x <= attackX )
				return true;
		} else if (lastDirection == 1) {
			if (x >= attackX && x <= attackX + attackRange)
				return true;
		} else if (lastDirection == 2) {
			if (y >= attackY && y <= attackY + attackRange)
				return true;
		} else if (lastDirection == 3) {
			if (y >= attackY - attackRange && y <= attackY)
				return true;
		}
		return false;
	}

	public void beAttacked() {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			currentHealth -= 0.5;
		}
	}
}


class Room {	//room means room on the server, which contains one dungeon with its rooms

	List<PlayerData> players = new ArrayList<PlayerData>();
	int roomID;
	List<Dungeon> dungeon = new ArrayList<Dungeon>();

	public Room(PlayerData player1, PlayerData player2, int roomID) {
		dungeon.add(new Dungeon(0, -1, -1, -1, 1));
		dungeon.add(new Dungeon(1, 2, -1, 0, -1));
		dungeon.add(new Dungeon(2, 3, 1, -1, -1));
		dungeon.add(new Dungeon(3, -1, 2, -1, -1));
		players.add(player1);
		players.get(0).currentRoom = this;
		Random r = new Random();
		players.get(0).currentDungeon = dungeon.get(0);
		players.add(player2);
		players.get(1).currentRoom = this;
		players.get(1).currentDungeon = dungeon.get(r.nextInt(dungeon.size() - 2) + 1);
		this.roomID = roomID;
	}

	public boolean areBothPlayersAliveInDungeon() {
		if (players.size() != 2)
			return false;
		if (players.get(0).currentDungeon == players.get(1).currentDungeon && players.get(0).currentHealth > 0 && players.get(1).currentHealth > 0)
			return true;
		return false;
	}

}

class Dungeon {
	int ID;
	List<String> monsters = new ArrayList<String>();
	public int direction[] = new int[4];
						//LEFT, RIGHT, UP, DOWN
	
	public Dungeon(int ID, int LEFT, int RIGHT, int UP, int DOWN) {
		this.ID = ID;
		direction[0] = LEFT;
		direction[1] = RIGHT;
		direction[2] = UP;
		direction[3] = DOWN;
	}

	public boolean areMonstersKilled() {
		if (monsters.size() == 0)
			return true;
		return false;
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
				PlayerData newPlayer = new PlayerData(300, 150, Integer.toString(ID), socket);
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
					//newRoom.start();
					System.out.println("Created room " + roomID);
				}
			} catch (IOException e) {
				System.out.println(e.getMessage() + ", failed to connect to client.");
			} 
		}	
	}


	synchronized public void broadcast(String s) {
		DataOutputStream dataOut = null;
		for (PlayerData player : players) {
			dataOut = (DataOutputStream)(player.clientOutput);
			try { 
				synchronized (dataOut) {
					dataOut.writeUTF(s); 
				}
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
	double wallLeftX = 15.918842;
	double wallUpY = 305.8834;
	double wallRightX = 578.09235;
	double wallDownY = 29.297974;


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

	int isPlayerInDoors(double x, double y, Dungeon dungeon) {
		if (x > 289 && x < 310) {
			if (y < wallDownY + 3.0 && dungeon.direction[3] != -1)
				return dungeon.direction[3];
			else if (y > wallUpY - 3.0 && dungeon.direction[2] != -1)
				return dungeon.direction[2];
		} else if (y > 149 && y < 166) {
			if (x < wallLeftX + 3.0 && dungeon.direction[0] != -1)
				return dungeon.direction[0];
			else if (x > wallRightX - 3.0 && dungeon.direction[1] != -1)
				return dungeon.direction[1];
		}
		return -1;
	}

	@Override
	public void run()  {
		while (true) {
				try {
					String command = "";
					synchronized (player.clientInput) {
						command = player.clientInput.readUTF();
					}
					System.out.println(command);
					if (command.startsWith("CONNECT")) {
						while (true) {
							if (player.currentRoom != null) {
								String id = player.socketID;
								String x = Double.toString(player.x);
								String y = Double.toString(player.y);
								synchronized (player.clientOutput) {
									player.clientOutput.writeUTF("CREATED " + id + " " + x + " " + y);
								}
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
							int doorID = isPlayerInDoors(player.x, player.y, player.currentDungeon);
							if (doorID != -1) {
								if (doorID == player.currentDungeon.direction[0]) {
									player.x = 575;
									player.y = 150;
								} else if (doorID == player.currentDungeon.direction[1]) {
									player.x = 16;
									player.y = 150;
								} else if (doorID == player.currentDungeon.direction[2]) {
									player.x = 300;
									player.y = 30;
								} else {
									player.x = 300;
									player.y = 305;
								}
								player.currentDungeon = player.currentRoom.dungeon.get(doorID);
							}
							for (PlayerData enemy : player.currentRoom.players) {
								if (player.currentDungeon == enemy.currentDungeon) {
									synchronized (player.clientOutput) {
										player.clientOutput.writeUTF("PLAYER_UPDATE " + enemy.socketID + " " + Double.toString(enemy.x) + " " + Double.toString(enemy.y));
									}
									if (player.lastAttack > System.currentTimeMillis() - 300 && player.currentDungeon == enemy.currentDungeon) {
										player.updateAttackXY();
										synchronized (enemy.clientOutput) {
											enemy.clientOutput.writeUTF("DRAW_ATTACK " + player.attackX + " " + player.attackY);
										}
										if (player != enemy && player.checkIfAttacked(enemy.x, enemy.y)) {
											enemy.beAttacked();
											synchronized (enemy.clientOutput) {
												enemy.clientOutput.writeUTF("HEALTH_UPDATE " + enemy.currentHealth);
											}
										}
									}
								} else {
									synchronized (enemy.clientOutput) {
										enemy.clientOutput.writeUTF("RESET_PLAYERS");
									}
								}
							}
							synchronized (player.clientOutput) {
								if (player.currentDungeon.areMonstersKilled() && !player.currentRoom.areBothPlayersAliveInDungeon())
									player.clientOutput.writeUTF("ROOM_UPDATE_OPENED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
								else
									player.clientOutput.writeUTF("ROOM_UPDATE_CLOSED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
							}
						}
					} else if (command.startsWith("ATTACK")) {
						int direction = Integer.parseInt(command.split(" ")[1]);
						player.attack(direction);
						for (PlayerData enemy : player.currentRoom.players) {
							if (player.currentDungeon == enemy.currentDungeon) {
								synchronized (enemy.clientOutput) {
									enemy.clientOutput.writeUTF("DRAW_ATTACK " + player.attackX + " " + player.attackY);
								}
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