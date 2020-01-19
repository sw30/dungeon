import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server extends Thread {
	private int port = 8080;
	public ServerSocket serverSock = null; 
	int ID = 0;
	int roomID = 0;
	
	List<PlayerData> players = new CopyOnWriteArrayList<PlayerData>();
	List<PlayerData> playersWithoutRooms = new CopyOnWriteArrayList<PlayerData>();


	public static void main(String args[]) {
	   System.out.println("Hit control-c to exit the server.");
	   Server server = new Server();
	   server.start();
	   server.server();
	   try {
			server.serverSock.close();
	   } catch (Exception e) {}
	}


	/**
	 * Function checks if the player is connected to the server by sending "CONFIG_TEST" message to him.
	 * If player can't read it, function will return false
	 * @param player - player to check connection
	 *
	 * @return true or false depending on being connected or not
	 */
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


	/**
	 * Method with never ending loop, which purpose is to accept clients.
	 * Accepted client is added to a list and is waiting till the server will find his opponent and create room for them.
	 */
	public void server() {
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
				System.out.println("Player " + ID + " connected");
				ID++;
				ClientHandler newHandler = new ClientHandler(newPlayer, this);
				newHandler.start();

			} catch (IOException e) {
				System.out.println(e.getMessage() + ", failed to connect to client.");
			} 
		}	
	}

	/**
	 *  Thread keeps on finding a pair to the player to let them start the game
	 */
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


	/**
	 * Sends a message to every player available
	 * @param s - Message to be sent
	 */
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


	/**
	 * Searches for player with ID given as argument
	 * @param ID - player to be find
	 * @return - player's PlayerData if player is found and null if not
	 */
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

	/**
	 * Sets information about player
	 * @param player - Information about client's hero
	 * @param server - Server that handler hands
	 */

	ClientHandler(PlayerData player, Server server) throws IOException {
		this.player = player;
		this.server = server;
	}


	/**
	 * Validates if player x isn't outside the room
	 * @param x Place on X-axis where player is trying to move
	 * @return Returns x if validated correctly or wall's x
	 */
	double validateX(double x) {
		if (x < wallLeftX)
			return wallLeftX;
		else if (x > wallRightX)
			return wallRightX;
		return x;
	}


	/**
	 * Validates if player y isn't outside the room
	 * @param y Place on Y-axis where player is trying to move
	 * @return Returns y if validated correctly or wall's y
	 */
	double validateY(double y) {
		if (y < wallDownY)
			return wallDownY;
		else if (y > wallUpY)
			return wallUpY;
		return y;
	}


	/**
	 * Checks if player should be teleported to a different room. Doesn't check if doors are open
	 * @param x - player's x
	 * @param y - player's y
	 * @param dungeon - Dungeon's room where the player is
	 * @return dungeon's room ID where player is supposed to go or -1 on fail
	 */
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


	/**
	 * Thread that handles the whole game for a single client
	 * Reacts on such commands as:
	 * CONNECT - If player is in room, lets him to begin the game
	 * PLAYERMOVED - Decides if player should move or not
	 * UPDATE - Sends player every information he needs to render the scene
	 * ATTACK - Checks if player can attack; if so - will decrease opponent's health. Sends information about drawing attack too
	 *
	 */
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
							if (player.sharpness == true && player.sharpnessLifeTime + 30000 < System.currentTimeMillis())
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
							if (randomValue == 0 && player.currentHealth < player.maxHealth) 	player.currentHealth += 1.0;
							else if (randomValue == 1 && player.currentHealth < player.maxHealth)	player.currentHealth += 0.5;
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
									String rewards[] = {"Small shield", "Medium shield", "Great shield", "Helmet pillow", "Better armor", "Magic book", "Spear", "Sharp sword"};
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
										player.sharpnessLifeTime = System.currentTimeMillis();
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