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
	public int level = 0;

	public long lastAttack = -100;
	public double attackX;
	public double attackY;
	public int lastDirection;
	public long coolDown = -1001;

	public boolean hasLost = false;
	public boolean ready = false;


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
		double up = 20;
		double down = 20;
		double left = 15;
		double right = 15;
		if (lastDirection == 0) {
			if (x >= attackX - attackRange && x <= attackX && y <= attackY + down && y >= attackY - up)
				return true;
		} else if (lastDirection == 1) {
			if (x >= attackX && x <= attackX + attackRange && y <= attackY + down && y >= attackY - up)
				return true;
		} else if (lastDirection == 2) {
			if (y >= attackY && y <= attackY + attackRange && x <= attackX + left && x >= attackX - right)
				return true;
		} else if (lastDirection == 3) {
			if (y >= attackY - attackRange && y <= attackY && x <= attackX + left && x >= attackX - right)
				return true;
		}
		return false;
	}

	public boolean beAttacked() {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			currentHealth -= 0.5;
			return true;
		}
		return false;
	}
}


class Room {	//room means room on the server, which contains many dungeons; each dungeon is just a room inside a room

	List<PlayerData> players = new ArrayList<PlayerData>();
	int roomID;
	List<Dungeon> dungeon = new ArrayList<Dungeon>();
	PlayerData loser = null;
	PlayerData winner = null;

	public void addDungeon() {
		if (dungeon.size() == 0)
			dungeon.add(new Dungeon(0, -1, -1, -1, -1));
		else {
			Random r = new Random();
			int newID = dungeon.size();
			int id = r.nextInt(newID);
			while (!dungeon.get(id).areAvailableDoors()) {
				id++;
				id = id % newID;
			}
			int direction = r.nextInt(4);
			while (dungeon.get(id).direction[direction] != -1) {
				direction++;
				direction = direction % 4;
			}
			dungeon.get(id).direction[direction] = newID;
			int LEFT = -1, RIGHT = -1, UP = -1, DOWN = -1;
			if (direction == 0)
				RIGHT = id;
			else if (direction == 1)
				LEFT = id;
			else if (direction == 2)
				DOWN = id;
			else if (direction == 3)
				UP = id;
			Dungeon newDungeon = new Dungeon(newID, LEFT, RIGHT, UP, DOWN);	
			dungeon.add(newDungeon);
		}
	}

	public Room(PlayerData player1, PlayerData player2, int roomID) {
		for (int i = 0; i < 10; ++i)
			this.addDungeon();
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


class Monster {
	public double x;
	public double y;
	public String type;
	public double health;
	public double velocity;
	public int id;

	public Monster(int id, int x, int y, String type) {
		this.x = x;
		this.y = y;
		this.type = type;
		this.id = id;
		if (type == "FLY") {
			health = 1.0;
			velocity = 3.0;
		} else if (type == "GOBLIN") {
			health = 2.0;
			velocity = 1.5;
		} else if (type == "SLIME") {
			health = 4.0;
			velocity = 0.75;
		}
	}

}


class Dungeon {
	int ID;
	List<Monster> monsters = new ArrayList<Monster>();
	public int direction[] = new int[4];
						//LEFT, RIGHT, UP, DOWN
	
	public Dungeon(int ID, int LEFT, int RIGHT, int UP, int DOWN) {
		this.ID = ID;
		direction[0] = LEFT;
		direction[1] = RIGHT;
		direction[2] = UP;
		direction[3] = DOWN;
		monsters.add(new Monster(0, 100, 200, "FLY"));
		monsters.add(new Monster(1, 400, 200, "GOBLIN"));
		monsters.add(new Monster(2, 250, 100, "SLIME"));
	}

	public boolean areMonstersKilled() {
		if (monsters.size() == 0)
			return true;
		return false;
	}

	public boolean areAvailableDoors() {
		for (int i = 0; i < 4; ++i)
			if (direction[i] == -1)
				return true;
		return false;
	}
}


public class Server extends Thread {
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

			} catch (IOException e) {
				System.out.println(e.getMessage() + ", failed to connect to client.");
			} 
		}	
	}

	@Override
	public void run()  {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			if (playersWithoutRooms.size() >= 2) {
				for (int i = 0; i < playersWithoutRooms.size(); ++i) {
					PlayerData player1 = playersWithoutRooms.get(i);
					for (int j = 1; j < playersWithoutRooms.size(); ++j) {
						if (i != j) {
							PlayerData player2 = playersWithoutRooms.get(j);
							if (!player1.ready && player1.currentRoom == null && !player2.ready && player2.currentRoom == null) {
								playersWithoutRooms.remove(player1);
								playersWithoutRooms.remove(player2);
								Room newRoom = new Room(player1, player2, roomID++);
								System.out.println("Created room " + roomID);
							}
						}
					}
				}
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
		while (!player.hasLost) {
			try {
				String command = "";
				synchronized (player.clientInput) {
					command = player.clientInput.readUTF();
				}
				//System.out.println(command);
				if (!player.ready && command.startsWith("CONNECT")) {
					while (true) {
						if (player.currentRoom != null) {
							player.x = 300;
							player.y = 150;
							String id = player.socketID;
							String x = Double.toString(player.x);
							String y = Double.toString(player.y);
							player.ready = true;
							synchronized (player.clientOutput) {
								player.clientOutput.writeUTF("CREATED " + id + " " + x + " " + y);
							}
							break;
						} else Thread.sleep(100);
					}
				} else if (player.ready && command.startsWith("PLAYERMOVED")) {
					String x = command.split(" ")[1];
					String y = command.split(" ")[2];
					player.x = validateX(Double.parseDouble(x));
					player.y = validateY(Double.parseDouble(y));
				} else if (command.startsWith("DISCONNECT")) {
					System.out.println("Player " + player.socketID + " has disconnected");
					server.broadcast("REMOVEPLAYER " + player.socketID);
					server.broadcast("ECHO Player " + player.socketID + " has disconnected");
					break;
				} else if (player.ready && command.startsWith("UPDATE")) {
					if (player.currentHealth <= 0.0) {
						synchronized (player.clientOutput) {
							player.clientOutput.writeUTF("LOST");
						}
						player.hasLost = true;
						server.players.remove(player);
					}
					if (player.currentRoom != null) {
						int doorID = isPlayerInDoors(player.x, player.y, player.currentDungeon);
						if (doorID != -1 && !player.currentRoom.areBothPlayersAliveInDungeon() && player.currentDungeon.areMonstersKilled()) {
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
							synchronized (player.clientOutput) {
								player.clientOutput.writeUTF("RESET_MONSTERS");
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
									if (player != enemy) {
										if (enemy.currentHealth <= 0.0) {
											player.currentRoom = null;
											player.level++;
											synchronized (player.clientOutput) {
												player.clientOutput.writeUTF("LEVEL_UP " + player.level);
											}
											server.broadcast("ECHO Player " + player.socketID + " has progressed to " + player.level + " level");
											Thread.sleep(3000);
											server.playersWithoutRooms.add(player);
											player.ready = false;
											break;
										} else if (player.checkIfAttacked(enemy.x, enemy.y) && enemy.beAttacked()) {
											synchronized (enemy.clientOutput) {
												enemy.clientOutput.writeUTF("HEALTH_UPDATE " + enemy.currentHealth);
												enemy.clientOutput.writeUTF("CHANGE_SPRITE " + enemy.socketID);
											}
											synchronized (player.clientOutput) {
												player.clientOutput.writeUTF("CHANGE_SPRITE " + enemy.socketID);
											}
										}
									}
								}
							} else {
								synchronized (enemy.clientOutput) {
									enemy.clientOutput.writeUTF("RESET_PLAYERS");
								}
							}
							if (enemy.coolDown + 1000 < System.currentTimeMillis()) {
								synchronized (player.clientOutput) {
									player.clientOutput.writeUTF("RESET_SPRITE " + enemy.socketID);
								}
							}
						}
						for (Monster monster : player.currentDungeon.monsters) {
							synchronized (player.clientOutput) {
								player.clientOutput.writeUTF("MONSTER_UPDATE " + monster.id + " " + monster.type + " " + monster.x + " " + monster.y);
							}
						}
						if (player.ready && player.currentRoom != null) {
							synchronized (player.clientOutput) {
								if (player.currentDungeon.areMonstersKilled() && !player.currentRoom.areBothPlayersAliveInDungeon())
									player.clientOutput.writeUTF("ROOM_UPDATE_OPENED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
								else
									player.clientOutput.writeUTF("ROOM_UPDATE_CLOSED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
							}
						}
					}
				} else if (player.ready && command.startsWith("ATTACK")) {
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
				e.printStackTrace();
				if (player.currentRoom != null) {
					for (PlayerData enemy : player.currentRoom.players) {
						try {
							enemy.currentRoom = null;
							synchronized (enemy.clientOutput) {
								enemy.clientOutput.writeUTF("LEVEL_UP " + enemy.level);
							}
						} catch (Exception f) {
							f.printStackTrace();
							System.out.println("Player " + enemy.socketID + " has disconnected");
							server.broadcast("REMOVEPLAYER " + enemy.socketID);
							server.broadcast("ECHO Player " + enemy.socketID + " has disconnected");
							break;
						}
							enemy.ready = true;
							server.playersWithoutRooms.add(enemy);
							enemy.ready = false;
					}
				}
				break;
			} 
		}
	}
	
	

}