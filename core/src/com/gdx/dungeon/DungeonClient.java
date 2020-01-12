package com.gdx.dungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.gdx.dungeon.screens.Play;
import com.gdx.dungeon.sprites.Hero;
import com.gdx.dungeon.sprites.Player;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class DungeonClient extends Game {
	private final float UPDATE_TIME = 1/60f;
	float timer = 0;
	private final String URI = "http://localhost:8080";
	public Socket socket;
	String id;
	public Hero player;
	Texture playerHero;
	Texture anotherHero;
	public HashMap<String, Hero> anotherPlayers = new HashMap<String, Hero>();

	@Override
	public void create() {
		setScreen(new Play(this));
		playerHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		anotherHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		connectSocket();
		configSocketEvents();
	}

	public void connectSocket() {
		try {
			socket = IO.socket(URI);
			socket.connect();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void configSocketEvents() {
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
					anotherPlayers.put(playerId, new Hero(anotherHero));
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
	}

	/*
	@Override
	public void dispose () {
		super.dispose();
		playerHero.dispose();
		anotherHero.dispose();
	} */
}
