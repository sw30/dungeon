package com.gdx.dungeon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.gdx.dungeon.screens.Play;
import com.gdx.dungeon.sprites.Hero;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

public class DungeonClient extends Game {
	private static final int PORT = 8080;
	private static final String HOST = "localhost";
	public Socket socket;
	String id = null;
	public Hero player;
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
	public boolean foundOpponent = true;


	@Override
	public void create() {
		connectSocket();
		play = new Play(this);
		setScreen(play);
		playerHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		anotherHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		//configSocketEvents();
		serverListener = new ServerListener(dataInput, dataOutput, this);
		serverListener.start();
	}

	public void connectSocket() {
		System.out.println("Connecting to: " + HOST + ", port: " + PORT + "...");
		try {
			clientSocket = new Socket(HOST, PORT);
			dataInput = new DataInputStream(clientSocket.getInputStream());
			dataOutput = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("Connected.");
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
			dataOutput.writeUTF("CONNECT");
			while (true) {
				String command = dataInput.readUTF();
				System.out.println(command);
				if (command.startsWith("PLAYER_UPDATE")) {
					String id = command.split(" ")[1];
					String x = command.split(" ")[2];
					String y = command.split(" ")[3];
					if (client.anotherPlayers.get(id) != null) {
						client.anotherPlayers.get(id).setPosition(Float.parseFloat(x), Float.parseFloat(y));
					} else if (client.id != null && id.equals(client.id) && client.player != null) {
						client.player.setPosition(Float.parseFloat(x), Float.parseFloat(y));
						client.posX = Float.parseFloat(x);
						client.posY = Float.parseFloat(y);
					} else if (client.id != null && !id.equals(client.id)){
						String playerId = id;
						Hero hero = new Hero(client.anotherHero);
						client.anotherPlayers.put(playerId, hero);
						client.anotherPlayers.get(playerId).setPosition(Float.parseFloat(x), Float.parseFloat(y));
					}
				} else if (command.startsWith("CREATED")) {
					client.id = command.split(" ")[1];
					String x = command.split(" ")[2];
					String y = command.split(" ")[3];
					client.player = new Hero(client.playerHero);
					client.player.setPosition(Float.parseFloat(x), Float.parseFloat(y));
					client.posX = Float.parseFloat(x);
					client.posY = Float.parseFloat(y);
				} else if (command.startsWith("REMOVEPLAYER")) {
					String id = command.split(" ")[1];
					client.anotherPlayers.remove(id);
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