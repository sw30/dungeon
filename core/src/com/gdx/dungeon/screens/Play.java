package com.gdx.dungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.gdx.dungeon.DungeonClient;
import com.gdx.dungeon.sprites.Hero;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Play implements Screen {

	private TiledMap map;
	private OrthogonalTiledMapRenderer renderer;
	OrthographicCamera camera;
	ExtendViewport viewport;
	DungeonClient client;
	SpriteBatch batch;
	private final float UPDATE_TIME = 1/60f;
	float timer = 0;

	public Play(DungeonClient info) {
		client = info;
		batch = new SpriteBatch();
	}

	@Override
	public void show() {
		TmxMapLoader loader = new TmxMapLoader();
		map = loader.load("maps/map1.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 2.35f);
		camera = new OrthographicCamera();
		float x = 305, y = 230;
		camera.position.set(new Vector2(x, y), 0);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		handleInput(Gdx.graphics.getDeltaTime());
		updateServer(Gdx.graphics.getDeltaTime());
		renderer.setView(camera);
		renderer.render();
		batch.begin();
		if (client.player != null) {
			client.player.draw(batch);
		}
		for (HashMap.Entry<String, Hero> entry : client.anotherPlayers.entrySet()) {
			entry.getValue().draw(batch);
		}
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		camera.viewportWidth = width;
		camera.viewportHeight = height;
		camera.update();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void dispose() {
		map.dispose();
		renderer.dispose();
	}

	public void handleInput(float dt) {
		if (client.player != null) {
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				client.player.setPosition(client.player.getX() + (-200 * dt), client.player.getY());
			}
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				client.player.setPosition(client.player.getX() + (200 * dt), client.player.getY());
			}
			if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
				client.player.setPosition(client.player.getX(), client.player.getY() + (200 * dt));
			}
			if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
				client.player.setPosition(client.player.getX(), client.player.getY() + (-200 * dt));
			}
		}
	}


	public void updateServer(float dt) {
		timer += dt;
		if (timer >= UPDATE_TIME && client.player != null) {
			try {
				client.dataOutput.writeUTF("PLAYERMOVED " + client.player.getX() + " " + client.player.getY());
				client.dataOutput.writeUTF("UPDATE");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}



}
