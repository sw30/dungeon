package com.gdx.dungeon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.gdx.dungeon.screens.Play;
import com.gdx.dungeon.sprites.Attack;
import com.gdx.dungeon.sprites.Door;
import com.gdx.dungeon.sprites.Hero;
import com.gdx.dungeon.sprites.Monster;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class DungeonClient extends Game {
	private static final int PORT = 8080;
	private static final String HOST = "localhost";
	public Socket socket;
	String id = null;
	public Hero player = null;
	Texture playerHero;
	Texture anotherHero;
	public HashMap<String, Hero> anotherPlayers = new HashMap<String, Hero>();
	Socket clientSocket;
	public DataInputStream dataInput;
	public DataOutputStream dataOutput;
	ServerListener serverListener;
	Play play;
	public double posX;
	public double posY;
	public boolean foundOpponent = false;
	public boolean isGameFinished = false;
	public String statusMessage = "";
	public long statusLifetime = -5000;
	public String itemMessage = "Found item: Armor";
	public String itemEffect = "Effect: HP UP";
	public long itemMessageLifeTime = 0;
	Texture upDoorTexture;
	Texture downDoorTexture;
	Texture leftDoorTexture;
	Texture rightDoorTexture;
	Texture upDoorTextureOpen;
	Texture downDoorTextureOpen;
	Texture leftDoorTextureOpen;
	Texture rightDoorTextureOpen;
	Texture attackTexture;
	Texture hitHeroTexture;
	Texture flyTexture;
	Texture goblinTexture;
	Texture slimeTexture;

	@Override
	public void create() {
		connectSocket();
		play = new Play(this);
		setScreen(play);
		playerHero = new Texture("heroes/knight/knight_idle_spritesheet.png");
		anotherHero = new Texture("heroes/knight/knight_idle_spritesheet_green.png");
		hitHeroTexture = new Texture("heroes/knight/knight_idle_spritesheet_hit.png");
		upDoorTexture = new Texture("tiles/wall/door_closed.png");
		downDoorTexture = new Texture("tiles/wall/door_closed_down.png");
		leftDoorTexture = new Texture("tiles/wall/door_closed_left.png");
		rightDoorTexture = new Texture("tiles/wall/door_closed_right.png");
		upDoorTextureOpen = new Texture("tiles/wall/door_open.png");
		downDoorTextureOpen = new Texture("tiles/wall/door_open_down.png");
		leftDoorTextureOpen = new Texture("tiles/wall/door_open_right.png");
		rightDoorTextureOpen = new Texture("tiles/wall/door_open_left.png");
		attackTexture = new Texture("effects /explosion_anim_f2.png");
		flyTexture = new Texture("enemies/flying creature/fly_anim_spritesheet.png");
		goblinTexture = new Texture("enemies/goblin/goblin_idle_spritesheet.png");
		slimeTexture = new Texture("enemies/slime/slime_run_spritesheeet.png");

		serverListener = new ServerListener(dataInput, dataOutput, this);
		serverListener.start();
	}

	public void connectSocket() {
		System.out.println("Connecting to: " + HOST + ", port: " + PORT + "...");
		try {
			clientSocket = new Socket(HOST, PORT);
			dataInput = new DataInputStream(clientSocket.getInputStream());
			dataOutput = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("Connected to Server.");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	@Override
	public void dispose () {
		super.dispose();
		playerHero.dispose();
		anotherHero.dispose();
		upDoorTexture.dispose();
		downDoorTexture.dispose();
		leftDoorTexture.dispose();
		rightDoorTexture.dispose();
		upDoorTextureOpen.dispose();
		downDoorTextureOpen.dispose();
		leftDoorTextureOpen.dispose();
		rightDoorTextureOpen.dispose();
		attackTexture.dispose();
		hitHeroTexture.dispose();
		flyTexture.dispose();
	}
}


class ServerListener extends Thread {

	private DataInputStream dataInput;
	private DataOutputStream dataOutput;
	private DungeonClient client;


	public ServerListener(DataInputStream dataInput, DataOutputStream dataOutput, DungeonClient client) {
		this.dataInput = dataInput;
		this.dataOutput = dataOutput;
		this.client = client;
	}

	@Override
	public void run() {
		try {
			synchronized (dataOutput) {
				dataOutput.writeUTF("CONNECT");
			}
			while (true) {
				String command = dataInput.readUTF();
				//System.out.println(command);
				if (command.startsWith("PLAYER_UPDATE")) {
					String id = command.split(" ")[1];
					String x = command.split(" ")[2];
					String y = command.split(" ")[3];
					if (client.anotherPlayers.get(id) != null) {
						if (client.anotherPlayers.get(id).didHeroMove(Float.parseFloat(x), Float.parseFloat(y)))
							client.anotherPlayers.get(id).update(Gdx.graphics.getDeltaTime());
						client.anotherPlayers.get(id).setPosition(Float.parseFloat(x), Float.parseFloat(y));
					} else if (client.id != null && id.equals(client.id) && client.player != null) {
						if (client.player.didHeroMove(Float.parseFloat(x), Float.parseFloat(y)))
							client.player.update(Gdx.graphics.getDeltaTime());
						client.player.setPosition(Float.parseFloat(x), Float.parseFloat(y));
						client.posX = Float.parseFloat(x);
						client.posY = Float.parseFloat(y);
					} else if (client.id != null && !id.equals(client.id)) {
						String playerId = id;
						Hero hero = new Hero(client.anotherHero, client.attackTexture, client.hitHeroTexture);
						client.anotherPlayers.put(playerId, hero);
						client.anotherPlayers.get(playerId).setPosition(Float.parseFloat(x), Float.parseFloat(y));
					}
				} else if (command.startsWith("CREATED")) {
					client.id = command.split(" ")[1];
					String x = command.split(" ")[2];
					String y = command.split(" ")[3];
					client.foundOpponent = true;
					client.isGameFinished = false;
					if (client.player == null)
						client.player = new Hero(client.playerHero, client.attackTexture, client.hitHeroTexture);
					client.player.setPosition(Float.parseFloat(x), Float.parseFloat(y));
					client.posX = Float.parseFloat(x);
					client.posY = Float.parseFloat(y);
				} else if (command.startsWith("REMOVEPLAYER")) {
					String id = command.split(" ")[1];
					client.anotherPlayers.remove(id);
				} else if (command.startsWith("ROOM_UPDATE_CLOSED")) {
					int left = Integer.parseInt(command.split(" ")[1]);
					int right = Integer.parseInt(command.split(" ")[2]);
					int up = Integer.parseInt(command.split(" ")[3]);
					int down = Integer.parseInt(command.split(" ")[4]);
					client.play.rooms[0] = left;
					client.play.rooms[1] = right;
					client.play.rooms[2] = up;
					client.play.rooms[3] = down;
					if (client.play.rooms[0] != -1) {
						Door leftDoor = new Door(client.leftDoorTexture);
						leftDoor.setPosition(0, 150);
						client.play.doorSprites[0] = leftDoor;
					}
					if (client.play.rooms[1] != -1) {
						Door rightDoor = new Door(client.rightDoorTexture);
						rightDoor.setPosition(600, 150);
						client.play.doorSprites[1] = rightDoor;
					}
					if (client.play.rooms[2] != -1) {
						Door upDoor = new Door(client.upDoorTexture);
						upDoor.setPosition(300, 307);
						client.play.doorSprites[2] = upDoor;
					}
					if (client.play.rooms[3] != -1) {
						Door downDoor = new Door(client.downDoorTexture);
						downDoor.setPosition(300, 2);
						client.play.doorSprites[3] = downDoor;
					}
				} else if (command.startsWith("ROOM_UPDATE_OPENED")) {
					int left = Integer.parseInt(command.split(" ")[1]);
					int right = Integer.parseInt(command.split(" ")[2]);
					int up = Integer.parseInt(command.split(" ")[3]);
					int down = Integer.parseInt(command.split(" ")[4]);
					client.play.rooms[0] = left;
					client.play.rooms[1] = right;
					client.play.rooms[2] = up;
					client.play.rooms[3] = down;
					if (client.play.rooms[0] != -1) {
						Door leftDoor = new Door(client.leftDoorTextureOpen);
						leftDoor.setPosition(0, 150);
						client.play.doorSprites[0] = leftDoor;
					}
					if (client.play.rooms[1] != -1) {
						Door rightDoor = new Door(client.rightDoorTextureOpen);
						rightDoor.setPosition(600, 150);
						client.play.doorSprites[1] = rightDoor;
					}
					if (client.play.rooms[2] != -1) {
						Door upDoor = new Door(client.upDoorTextureOpen);
						upDoor.setPosition(300, 307);
						client.play.doorSprites[2] = upDoor;
					}
					if (client.play.rooms[3] != -1) {
						Door downDoor = new Door(client.downDoorTextureOpen);
						downDoor.setPosition(300, 2);
						client.play.doorSprites[3] = downDoor;
					}
				} else if (command.startsWith("RESET_PLAYERS")) {
					client.anotherPlayers.clear();
				} else if (command.startsWith("DRAW_ATTACK")) {
					String attackX = command.split(" ")[1];
					String attackY = command.split(" ")[2];
					synchronized (client.play.attackList) {
						client.play.attackList.add(new Attack(Float.parseFloat(attackX), Float.parseFloat(attackY), client.attackTexture));
					}
				} else if (command.startsWith("HEALTH_UPDATE")) {
					double health = Double.parseDouble(command.split(" ")[1]);
					double maxHealth = Double.parseDouble(command.split(" ")[2]);
					client.player.health = health;
					client.player.maxHealth = maxHealth;
				} else if (command.startsWith("CHANGE_SPRITE")) {
					String clientID = command.split(" ")[1];
					if (client.id.equals(clientID)) {
						client.player.changeTexture(client.player.hitHeroTexture);
					} else if (client.anotherPlayers.get(clientID) != null)
						client.anotherPlayers.get(clientID).changeTexture(client.anotherPlayers.get(clientID).hitHeroTexture);
				} else if (command.startsWith("RESET_SPRITE")) {
					String clientID = command.split(" ")[1];
					if (client.id.equals(clientID))
						client.player.changeTexture(client.player.texture);
					else if (client.anotherPlayers.get(clientID) != null)
						client.anotherPlayers.get(clientID).changeTexture(client.anotherPlayers.get(clientID).texture);
				} else if (command.startsWith("LEVEL_UP")) {
					if (client.player != null) {
						String level = command.split(" ")[1];
						client.player.level = Integer.parseInt(level);
						synchronized (client.play.attackList) {
							client.play.attackList.clear();
						}
						synchronized (client.play.monsterList) {
							client.play.monsterList.clear();
						}
						client.foundOpponent = false;
						synchronized (dataOutput) {
							dataOutput.writeUTF("CONNECT");
						}
					}
				} else if (command.startsWith("LOST")) {
					client.isGameFinished = true;
					Thread.sleep(5000);
					System.exit(0);
				} else if (command.startsWith("ECHO")) {
					client.statusMessage = command.split(" ", 2)[1];
					client.statusLifetime = System.currentTimeMillis();
				} else if (command.startsWith("MONSTER_UPDATE")) {
					int id = Integer.parseInt(command.split(" ")[1]);
					String monsterType = command.split(" ")[2];
					String x = command.split(" ")[3];
					String y = command.split(" ")[4];
					Monster monster = null;
					for (Monster enemy : client.play.monsterList) {
						if (enemy.id == id)
							monster = enemy;
					}
					if (monster != null) {
						monster.setX(Float.parseFloat(x));
						monster.setY(Float.parseFloat(y));
					} else {
						if (monsterType.equals("FLY"))
							client.play.monsterList.add(new Monster(client.flyTexture, 4, id, Float.parseFloat(x), Float.parseFloat(y)));
						else if (monsterType.equals("GOBLIN"))
							client.play.monsterList.add(new Monster(client.goblinTexture, 6, id, Float.parseFloat(x), Float.parseFloat(y)));
						else if (monsterType.equals("SLIME"))
							client.play.monsterList.add(new Monster(client.slimeTexture, 6, id, Float.parseFloat(x), Float.parseFloat(y)));
					}
				} else if (command.startsWith("RESET_MONSTERS")) {
					synchronized (client.play.monsterList) {
						client.play.monsterList.clear();
					}
				} else if (command.startsWith("DELETE_MONSTER")) {
					int id = Integer.parseInt(command.split(" ")[1]);
					Monster removed = null;
					for (Monster enemy : client.play.monsterList) {
						if (enemy.id == id) {
							removed = enemy;
							break;
						}
					}
					synchronized (client.play.monsterList) {
						if (removed != null)
							client.play.monsterList.remove(removed);
					}
				} else if (command.equals("CONFIG_TEST")) {
				} else if (command.startsWith("NEW_ITEM")) {
					String item = command.split("\t")[1];
					String effect = command.split("\t")[2];
					client.itemMessage = "Found item: " + item;
					client.itemEffect = "Effect: " + effect;
					client.itemMessageLifeTime = System.currentTimeMillis();
				}
			}
		} catch (java.io.EOFException e) {
			System.out.println("Connection has been lost");
			System.exit(3);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}