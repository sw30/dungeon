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
	public double maxHealth = 6.0;
	public double currentHealth = 6.0;
	public int level = 0;

	public long lastAttack = -100;
	public double attackX;
	public double attackY;
	public int lastDirection;
	public long coolDown = -1001;
	public int shield = 0;
	public boolean sharpness = false;
	public long sharpnessLifeTime = -30001;

	public boolean hasLost = false;
	public int procedure = 0;


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
		if (!(lastAttack > System.currentTimeMillis() - 100))
			return false;
		updateAttackXY();
		double up = 20;
		double down = 20;
		double left = 15;
		double right = 15;
		if (lastDirection == 0) {
			if (x + left >= attackX - attackRange && x <= attackX && y <= attackY + down && y >= attackY - up)
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

	public boolean beAttacked(PlayerData player) {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			if (shield <= 0) {
				if (player != null && player.sharpness)
					currentHealth -= 1.0;
				else
					currentHealth -= 0.5;
				shield = 0;
			} else shield--;
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
		for (int i = 0; i < 10; ++i) {
			addDungeon();
			if (i != 0 && i != 9)
				dungeon.get(dungeon.size() - 1).spawnRandomMonsters();
		}
		players.add(player1);
		players.get(0).currentRoom = this;
		players.get(0).currentDungeon = dungeon.get(0);
		players.add(player2);
		players.get(1).currentRoom = this;
		players.get(1).currentDungeon = dungeon.get(dungeon.size() - 1);
		this.roomID = roomID;
		players.get(0).procedure = 1;
		players.get(1).procedure = 1;
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
	public long coolDown = -1001;
	double m = 20.0;

	public Monster(int id, int x, int y, String type) {
		this.x = x;
		this.y = y;
		this.type = type;
		this.id = id;
		if (type == "FLY") {
			health = 0.5;
			velocity = 2.5;
		} else if (type == "GOBLIN") {
			health = 2.0;
			velocity = 1;
		} else if (type == "SLIME") {
			health = 5.0;
			velocity = 0.5;
		}
	}

	public boolean beAttacked(PlayerData player) {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			if (player != null && player.sharpness)
				health -= 1.0;
			else
				health -= 0.5;
			return true;
		}
		return false;
	}

	void move(PlayerData player) throws IOException {
		if (player.x + m > x && player.x - m > x)
			x += velocity;
		if (player.x - m < x && player.x + m < x)
			x -= velocity;
		if (player.y + m + 5 > y && player.y + 5 > y)
			y += velocity;
		if (player.y - m - 5 < y && player.y - 5 < y)
			y -= velocity;
		attack(player);
	}

	void attack(PlayerData player) throws IOException {
		if (player.x - 25 < x && x < player.x + 30) {
			if (player.y - 10 < y && y < player.y + 20) {
				player.beAttacked(null);
				synchronized (player.clientOutput) {
					player.clientOutput.writeUTF("HEALTH_UPDATE " + player.currentHealth + " " + player.maxHealth);
					player.clientOutput.writeUTF("CHANGE_SPRITE " + player.socketID);
				}
			}
		}
	}

}


class Dungeon {
	int ID;
	List<Monster> monsters = new ArrayList<Monster>();
	public int direction[] = new int[4];
						//LEFT, RIGHT, UP, DOWN
	public boolean wasEmpty = true;
	public boolean wasRewarded = false;
	
	public Dungeon(int ID, int LEFT, int RIGHT, int UP, int DOWN) {
		this.ID = ID;
		direction[0] = LEFT;
		direction[1] = RIGHT;
		direction[2] = UP;
		direction[3] = DOWN;
	}

	public void spawnRandomMonsters() {
		Random r = new Random();
		int dice = r.nextInt(5);
		if (dice == 0) {
			monsters.add(new Monster(0, 100, 220, "FLY"));
			monsters.add(new Monster(1, 200, 180, "FLY"));
			monsters.add(new Monster(2, 300, 200, "FLY"));
			monsters.add(new Monster(3, 400, 240, "FLY"));
		} else if (dice == 1) {
			monsters.add(new Monster(0, 400, 200, "GOBLIN"));
			monsters.add(new Monster(1, 250, 100, "SLIME"));
		} else if (dice == 2) {
			monsters.add(new Monster(0, 100, 100, "FLY"));
			monsters.add(new Monster(1, 200, 100, "GOBLIN"));
			monsters.add(new Monster(2, 300, 100, "FLY"));
		} else if (dice == 3) {
			monsters.add(new Monster(0, 250, 100, "SLIME"));
		} else if (dice == 4) {
			monsters.add(new Monster(0, 300, 200, "SLIME"));
			monsters.add(new Monster(1, 100, 220, "FLY"));
			monsters.add(new Monster(2, 100, 300, "FLY"));
		}
		wasEmpty = false;
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

	public boolean isConnected(PlayerData player) {
		try {
			synchronized (player.clientOutput) {
				player.clientOutput.writeUTF("CONFIG_TEST");
			}
		} catch (Exception e) {
			return false;
		}
		return true;
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
					if (!isConnected(player1)) {
						playersWithoutRooms.remove(player1);
						players.remove(player1);
						break;
					}
					for (int j = 1; j < playersWithoutRooms.size(); ++j) {
						if (i != j) {
							PlayerData player2 = playersWithoutRooms.get(j);
							if (!isConnected(player2)) {
								playersWithoutRooms.remove(player2);
								players.remove(player2);
								break;
							}
							if (player1.procedure == 0 && player1.procedure == 0) {
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
		List <PlayerData> toRemove = new ArrayList <PlayerData>();
		for (PlayerData player : players) {
			dataOut = (DataOutputStream)(player.clientOutput);
			try { 
				synchronized (dataOut) {
					dataOut.writeUTF(s); 
				}
			} catch (IOException x) {
				System.out.println(x.getMessage() + ": Failed to broadcast to client.");
				toRemove.add(player);
			}
		}
		for (PlayerData player : toRemove) {
			players.remove(player);
			broadcast("REMOVEPLAYER " + player.socketID);
			broadcast("ECHO Player " + player.socketID + " has disconnected");
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
				if (player.procedure != 2 && command.startsWith("CONNECT")) {
					while (true) {
						if (player.procedure == 1) {
							player.x = 300;
							player.y = 150;
							String id = player.socketID;
							String x = Double.toString(player.x);
							String y = Double.toString(player.y);
							synchronized (player.clientOutput) {
								player.clientOutput.writeUTF("CREATED " + id + " " + x + " " + y);
							}
							if (player.sharpness)
								player.sharpnessLifeTime = System.currentTimeMillis();
							player.procedure = 2;
							break;
						} else Thread.sleep(100);
					}
				} else if (player.procedure == 2 && command.startsWith("PLAYERMOVED")) {
					String x = command.split(" ")[1];
					String y = command.split(" ")[2];
					player.x = validateX(Double.parseDouble(x));
					player.y = validateY(Double.parseDouble(y));
				} else if (command.startsWith("DISCONNECT")) {
					System.out.println("Player " + player.socketID + " has disconnected");
					server.broadcast("REMOVEPLAYER " + player.socketID);
					server.players.remove(player);
					server.broadcast("ECHO Player " + player.socketID + " has disconnected");
					break;
				} else if (player.procedure == 2 && command.startsWith("UPDATE")) {
					if (player.currentRoom != null) {
						int doorID = isPlayerInDoors(player.x, player.y, player.currentDungeon);
						if (doorID != -1 && !player.currentRoom.areBothPlayersAliveInDungeon() && player.currentDungeon.areMonstersKilled()) {
							if (player.sharpness == true && player.sharpnessLifeTime + 30000 > System.currentTimeMillis())
								player.sharpness = false;
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
						} else if (!player.currentRoom.areBothPlayersAliveInDungeon() && player.currentDungeon.areMonstersKilled() && !player.currentDungeon.wasEmpty && !player.currentDungeon.wasRewarded) {
							player.currentDungeon.wasRewarded = true;
							String rewards[] = {"Heart container", "Half heart container", "Bandage", "Weird potion", "Knife sharpener"};
							String descriptions[] = {"+1.0 health", "+0.5 health", "Full health", "Unknown", "Temporary DMG UP"};
							Random r = new Random();
							int randomValue = r.nextInt(rewards.length * 2);
							if (randomValue < rewards.length)
								synchronized(player.clientOutput) {
									player.clientOutput.writeUTF("NEW_ITEM\t" + rewards[randomValue] + "\t" + descriptions[randomValue]);
								}
							if (randomValue == 0) 		player.currentHealth = player.currentHealth + 1.0 % player.maxHealth;
							else if (randomValue == 1)	player.currentHealth = player.currentHealth + 0.5 % player.maxHealth;
							else if (randomValue == 2 && player.currentHealth < player.maxHealth)	player.currentHealth = player.maxHealth;
							else if (randomValue == 3) {
								int secondRandom = r.nextInt(6);
								if (secondRandom == 0)		player.currentHealth += 0.5;
								else if (secondRandom == 1)	player.maxHealth += 0.5;
								else if (secondRandom == 2)	player.attackRange *= 1.1;
								else if (secondRandom == 3 && player.attackRange > 15)	player.attackRange *= 0.9;
							} else if (randomValue == 4) {
								player.sharpness = true;
								player.sharpnessLifeTime = System.currentTimeMillis();
							}
							if (randomValue < rewards.length)
								synchronized (player.clientOutput) {
									player.clientOutput.writeUTF("HEALTH_UPDATE " + player.currentHealth + " " + player.maxHealth);
								}


						}
						for (PlayerData enemy : player.currentRoom.players) {
							if (player.currentDungeon == enemy.currentDungeon) {
								synchronized (player.clientOutput) {
									player.clientOutput.writeUTF("PLAYER_UPDATE " + enemy.socketID + " " + Double.toString(enemy.x) + " " + Double.toString(enemy.y));
								}
								if (player.lastAttack > System.currentTimeMillis() - 100 && player.currentDungeon == enemy.currentDungeon) {
									player.updateAttackXY();
									synchronized (enemy.clientOutput) {
										enemy.clientOutput.writeUTF("DRAW_ATTACK " + player.attackX + " " + player.attackY);
									}
								}
							} else if (enemy.procedure == 2){
								synchronized (enemy.clientOutput) {
									enemy.clientOutput.writeUTF("RESET_PLAYERS");
								}
							}
							if (player != enemy) {
								if (enemy.currentHealth <= 0.0) {
									player.level++;
									player.procedure = 0;
									player.currentDungeon.monsters.clear();
									synchronized (player.clientOutput) {
										player.clientOutput.writeUTF("LEVEL_UP " + player.level);
									}
									Random r = new Random();
									String rewards[] = {"Small shield", "Medium shield", "Great shield", "Helmet pilow", "Better armor", "Magic book", "Spear", "Sharp sword"};
									String descriptions[] = {"+1 shield", "+3 shield", "+5 shield", "HP UP", "HP UP", "High range", "Medium range", "Temporary DMG UP, small range"}; 
									int randomValue = r.nextInt(rewards.length);
									if (randomValue == 0) 		player.shield++;
									else if (randomValue == 1)	player.shield += 3;
									else if (randomValue == 2)	player.shield += 5;
									else if (randomValue == 3 || randomValue == 4) {
										player.maxHealth += 1.0;
										player.currentHealth += 1.0;
										synchronized (player.clientOutput) {
											player.clientOutput.writeUTF("HEALTH_UPDATE " + player.currentHealth + " " + player.maxHealth);
										}
									} else if (randomValue == 5) player.attackRange = 50;
									else if (randomValue == 6)	player.attackRange = 35;
									else if (randomValue == 7) {
										player.attackRange = 25;
										player.sharpness = true;
									}
									synchronized (player.clientOutput) {
										player.clientOutput.writeUTF("NEW_ITEM\t" + rewards[randomValue] + "\t" + descriptions[randomValue]);
									}
									server.broadcast("ECHO Player " + player.socketID + " has progressed to " + player.level + " level");
									Thread.sleep(3000);
									server.playersWithoutRooms.add(player);
									break;
								} else if (player.currentDungeon == enemy.currentDungeon && player.checkIfAttacked(enemy.x, enemy.y) && enemy.beAttacked(player)) {
									synchronized (enemy.clientOutput) {
										enemy.clientOutput.writeUTF("HEALTH_UPDATE " + enemy.currentHealth + " " + enemy.maxHealth);
										enemy.clientOutput.writeUTF("CHANGE_SPRITE " + enemy.socketID);
									}
									synchronized (player.clientOutput) {
										player.clientOutput.writeUTF("CHANGE_SPRITE " + enemy.socketID);
									}
								}
							}
							if (player.currentDungeon == enemy.currentDungeon && enemy.coolDown + 1000 < System.currentTimeMillis()) {
								synchronized (player.clientOutput) {
									player.clientOutput.writeUTF("RESET_SPRITE " + enemy.socketID);
								}
							}
						}
						if (player.currentHealth <= 0.0) {
							synchronized (player.clientOutput) {
								player.clientOutput.writeUTF("LOST");
							}
							player.hasLost = true;
							server.players.remove(player);
						}
						List <Monster> toDelete = new ArrayList <Monster>();
						for (Monster monster : player.currentDungeon.monsters) {
							if (player.checkIfAttacked(monster.x, monster.y) && monster.beAttacked(player)) {
								if (monster.health <= 0.0) {
									for (PlayerData enemy : player.currentRoom.players) {
										if (player.currentDungeon == enemy.currentDungeon) {
											synchronized (enemy.clientOutput) {
												enemy.clientOutput.writeUTF("DELETE_MONSTER " + monster.id);
											}
										}
									}
									toDelete.add(monster);
								}
							}
						}
						for (Monster monster : toDelete)
							player.currentDungeon.monsters.remove(monster);
						for (Monster monster : player.currentDungeon.monsters) {
							synchronized (player.clientOutput) {
								monster.move(player);
								player.clientOutput.writeUTF("MONSTER_UPDATE " + monster.id + " " + monster.type + " " + monster.x + " " + monster.y);
							}
						}
						if (player.currentRoom != null) {
							synchronized (player.clientOutput) {
								if (player.currentDungeon.areMonstersKilled() && !player.currentRoom.areBothPlayersAliveInDungeon())
									player.clientOutput.writeUTF("ROOM_UPDATE_OPENED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
								else
									player.clientOutput.writeUTF("ROOM_UPDATE_CLOSED " + player.currentDungeon.direction[0] + " " + player.currentDungeon.direction[1] + " " +  player.currentDungeon.direction[2] + " " + player.currentDungeon.direction[3]);
							}
						}
					}
				} else if (player.procedure == 2 && command.startsWith("ATTACK")) {
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
							enemy.procedure = 0;
							enemy.currentDungeon.monsters.clear();
							synchronized (enemy.clientOutput) {
								enemy.clientOutput.writeUTF("LEVEL_UP " + enemy.level);
							}
							server.playersWithoutRooms.add(enemy);
						} catch (Exception f) {
							f.printStackTrace();
							System.out.println("Player " + enemy.socketID + " has disconnected");
							server.players.remove(enemy);
							server.broadcast("REMOVEPLAYER " + enemy.socketID);
							server.broadcast("ECHO Player " + enemy.socketID + " has disconnected");
						}
					}
				}
				break;
			} 
		}
	}
	
	

}