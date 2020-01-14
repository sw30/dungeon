package com.gdx.dungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.gdx.dungeon.DungeonClient;
import com.gdx.dungeon.sprites.Hero;

import java.util.HashMap;

public class Play implements Screen {

	public TiledMap map = null;
	private OrthogonalTiledMapRenderer renderer;
	OrthographicCamera camera;
	ExtendViewport viewport;
	DungeonClient client;
	SpriteBatch batch;
	BitmapFont font;
	private final float UPDATE_TIME = 1/30f;
	float timer = 0;

	public Play(DungeonClient info) {
		client = info;
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().setScale(1);
		font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

	}

	@Override
	public void show() {
		loadMap();
		renderer = new OrthogonalTiledMapRenderer(map, 0);	//hidden
		camera = new OrthographicCamera();
		float x = 305, y = 230;
		camera.position.set(new Vector2(x, y), 0);
	}


	public void hideMap() {
		if (map != null)
			renderer = new OrthogonalTiledMapRenderer(map, 0);
	}

	public void unhideMap() {
		if (map != null)
			renderer = new OrthogonalTiledMapRenderer(map, 2.35f);
	}

	public void loadMap() {
		if (map == null) {
			TmxMapLoader loader = new TmxMapLoader();
			map = loader.load("maps/map1.tmx");
		}
	}

	public void displayCenterText(String text) {
		font.draw(batch, text, 200, 200);
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
		if (client.foundOpponent) {
			unhideMap();
			if (client.player != null) {
				//client.player.draw(batch);
				//batch.draw(client.player.frames.get(client.player.frame), client.player.getX(), client.player.getY());
				//client.player.update(Gdx.graphics.getDeltaTime());
				client.player.draw(batch);
			}
			font.draw(batch, "Health: " + client.player.health, 50, 375);
			font.draw(batch, "Level: " + client.player.level, 50, 400);
			for (HashMap.Entry<String, Hero> entry : client.anotherPlayers.entrySet()) {
				//entry.getValue().draw(batch);
				//batch.draw(entry.getValue().frames.get(entry.getValue().frame), entry.getValue().getX(), entry.getValue().getY());
				//entry.getValue().update(Gdx.graphics.getDeltaTime());
				entry.getValue().draw(batch);
			}
		} else {
			hideMap();
			displayCenterText("Wyszukiwanie drugiego gracza...");
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
		if (map != null) {
			map.dispose();
			map = null;
		}
		renderer.dispose();
	}

	public void handleInput(float dt) {
		if (client.player != null) {
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				//client.player.setPosition(client.player.getX() + (-200 * dt), client.player.getY());
				client.posX = client.posX + (-200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				//client.player.setPosition(client.player.getX() + (200 * dt), client.player.getY());
				client.posX = client.posX + (200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
				//client.player.setPosition(client.player.getX(), client.player.getY() + (200 * dt));
				client.posY = client.posY + (200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
				//client.player.setPosition(client.player.getX(), client.player.getY() + (-200 * dt));
				client.posY = client.posY + (-200 * dt);
			}
		}
	}


	public void updateServer(float dt) {
		timer += dt;
		if (timer >= UPDATE_TIME && client.player != null) {
			try {
				client.dataOutput.writeUTF("PLAYERMOVED " + client.posX + " " + client.posY);
				client.dataOutput.writeUTF("UPDATE");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}



}
