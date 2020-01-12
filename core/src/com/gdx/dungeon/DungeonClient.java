package com.gdx.dungeon;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.gdx.dungeon.sprites.Hero;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class DungeonClient extends ApplicationAdapter {
	SpriteBatch batch;
	private Socket socket;
	private String URI = "http://localhost:8080";
	String id;
	Hero player;
	Texture playerHero;
	Texture anotherHero;
	HashMap<String, Hero> anotherPlayers;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		playerHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		anotherHero = new Texture("heroes/knight/knight_idle_anim_f0.png");
		anotherPlayers = new HashMap<String, Hero>();
		connectSocket();
		configSocketEvents();
	}

	public void handleInput(float dt) {
		if (player != null) {
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				player.setPosition(player.getX() + (-200 * dt), player.getY());
			} else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				player.setPosition(player.getX() + ( 200 * dt), player.getY());
			}
		}
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		handleInput(Gdx.graphics.getDeltaTime());
		batch.begin();
		if (player != null) {
			player.draw(batch);
		}
		for (HashMap.Entry<String, Hero> entry : anotherPlayers.entrySet()) {
			 entry.getValue().draw(batch);
		}
		batch.end();
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
					id = data.getString("id");
					Gdx.app.log("SocketIO", "New Player connected: " + id);
					anotherPlayers.put(id, new Hero(anotherHero));
				} catch (JSONException e) {
					Gdx.app.log("SocketIO", "Error getting new player ID");
				}
			}
		});
	}
	
	@Override
	public void dispose () {
		super .dispose();
		playerHero.dispose();
		anotherHero.dispose();
	}
}
