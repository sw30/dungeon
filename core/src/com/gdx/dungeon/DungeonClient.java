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
	String id;
	public Hero player;
	Texture playerHero;
	Texture anotherHero;
	public HashMap<String, Hero> anotherPlayers = new HashMap<String, Hero>();
	Socket clientSocket;
	public DataInputStream dataInput;
	public DataOutputStream dataOutput;
	ServerListener serverListener;


	@Override
	public void create() {
		connectSocket();
		setScreen(new Play(this));
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



	/*public void configSocketEvents() {
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				Gdx.app.log("SocketIO", "Connected");
				player = new Hero(playerHero);
			}
		}).on("socketID", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String id = data.getString("id");
					Gdx.app.log("SocketIO", "My ID: " + id);
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting ID");
				}
			}
		}).on("newPlayer", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String playerId = data.getString("id");
					Gdx.app.log("SocketIO", "New Player connected: " + playerId);
					Hero hero = new Hero(anotherHero);
					anotherPlayers.put(playerId, hero);
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting new player ID");
				}
			}
		}).on("playerDisconnected", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					id = data.getString("id");
					anotherPlayers.remove(id);
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting new player ID");
				}
			}
		}).on("getPlayers", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONArray objects = (JSONArray) args[0];
				try {
					for (int i = 0; i < objects.length(); ++i) {
						Hero differentPlayer = new Hero(anotherHero);
						Vector2 position = new Vector2();
						position.x = ((Double) objects.getJSONObject(i).getDouble("x")).floatValue();
						position.y = ((Double) objects.getJSONObject(i).getDouble("y")).floatValue();
						differentPlayer.setPosition(position.x, position.y);
						anotherPlayers.put(objects.getJSONObject(i).getString("id"), differentPlayer);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}).on("playerMoved", new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject data = (JSONObject) args[0];
				try {
					String playerId = data.getString("id");
					Double x = data.getDouble("x");
					Double y = data.getDouble("y");
					if (anotherPlayers.get(playerId) != null)
						anotherPlayers.get(playerId).setPosition(x.floatValue(), y.floatValue());
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting new player ID");
				}
			}
		});
	}*/


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
					} else if (client.id != null && id == client.id) {
						client.player.setPosition(Float.parseFloat(x), Float.parseFloat(y));
					} else if (client.id != null){
						String playerId = id;
						Hero hero = new Hero(client.anotherHero);
						client.anotherPlayers.put(playerId, hero);
					}
				} else if (command.startsWith("CREATED")) {
					client.id = command.split(" ")[1];
					String x = command.split(" ")[2];
					String y = command.split(" ")[3];
					client.player = new Hero(client.playerHero);
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