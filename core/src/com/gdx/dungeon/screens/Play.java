package com.gdx.dungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.gdx.dungeon.DungeonClient;
import com.gdx.dungeon.sprites.Attack;
import com.gdx.dungeon.sprites.Door;
import com.gdx.dungeon.sprites.Hero;
import com.gdx.dungeon.sprites.Monster;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	public int rooms[] = new int[4];
	public Door doorSprites[] = new Door[4];
	public List<Attack> attackList = new CopyOnWriteArrayList<Attack>();
	public List<Monster> monsterList = new CopyOnWriteArrayList<Monster>();

	public Play(DungeonClient info) {
		client = info;
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.getData().setScale(1);
		font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		for (int i = 0; i < 4; ++i) {
			rooms[i] = -1;
			doorSprites[i] = null;
		}
	}

	@Override
	public void show() {
		loadMap();
		renderer = new OrthogonalTiledMapRenderer(map, 0);	//hidden
		camera = new OrthographicCamera();
		float x = 305, y = 230;
		camera.position.set(new Vector2(x, y), 0);
		camera.viewportWidth = 640;
		camera.viewportHeight = 480;
		renderer.setView(camera);
	}


	public void hideMap() {
		if (map != null) {
			renderer = new OrthogonalTiledMapRenderer(map, 0);
		}
	}

	public void unhideMap() {
		if (map != null) {
			renderer = new OrthogonalTiledMapRenderer(map, 2.35f);
		}
	}

	@Override
	public void resize(int width, int height) {
		camera.viewportWidth = 640;
		camera.viewportHeight = 480;
		renderer = new OrthogonalTiledMapRenderer(map, 2.35f);
		camera.update();
	}

	public void loadMap() {
		if (map == null) {
			TmxMapLoader loader = new TmxMapLoader();
			map = loader.load("maps/map1.tmx");
		}
	}

	public void displayCenterText(String text) {
		font.draw(batch, text, 230, 250);
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
		if (System.currentTimeMillis() < client.statusLifetime + 5000)
			font.draw(batch, client.statusMessage, 30, 470);
		if (client.foundOpponent && !client.isGameFinished) {
			unhideMap();
			for (int i = 0; i < 4; ++i) {
				if (rooms[i] == -1)
					doorSprites[i] = null;
				if (doorSprites[i] != null)
					doorSprites[i].draw(batch);
			}
			for (HashMap.Entry<String, Hero> entry : client.anotherPlayers.entrySet()) {
				entry.getValue().draw(batch);
			}
			if (client.player != null) {
				client.player.draw(batch);
				font.draw(batch, "Health: " + client.player.health + " / " + client.player.maxHealth, 50, 375);
				font.draw(batch, "Level: " + client.player.level, 50, 400);
				if (System.currentTimeMillis() < client.itemMessageLifeTime + 7000) {
					font.draw(batch, client.itemMessage, 300, 400);
					font.draw(batch, client.itemEffect, 300, 375);
				}
			}
			synchronized (monsterList) {
				for (Monster monster : monsterList) {
					monster.update(Gdx.graphics.getDeltaTime());
					monster.draw(batch);
				}
			}
			if (client.player != null) {
				synchronized (attackList) {
					for (Attack attack : attackList) {
						attack.setPosition(attack.attackX, attack.attackY);
						attack.draw(batch);
					}
					attackList.clear();
				}
			}
		} else if (client.isGameFinished) {
			hideMap();
			displayCenterText("Game over.\nYou have lost at level " + client.player.level);
		} else if (client.player != null && client.player.level > 0){
			hideMap();
			if (System.currentTimeMillis() < client.itemMessageLifeTime + 5000) {
				font.draw(batch, client.itemMessage, 30, 445);
				font.draw(batch, client.itemEffect, 30, 420);
			}
			displayCenterText("Moving to " + client.player.level + " level.\nWaiting for opponent...");
		} else {
			hideMap();
			displayCenterText("Waiting for another player...");
		}
		batch.end();
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
			if (Gdx.input.isKeyPressed(Input.Keys.A)) {
				client.posX = client.posX + (-200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.D)) {
				client.posX = client.posX + (200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.W)) {
				client.posY = client.posY + (200 * dt);
			}
			if (Gdx.input.isKeyPressed(Input.Keys.S)) {
				client.posY = client.posY + (-200 * dt);
			}

			try {
				if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
					synchronized (client.dataOutput) {
						client.dataOutput.writeUTF("ATTACK 2");
					}
					client.player.lastAttack = System.currentTimeMillis();
				} else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
					synchronized (client.dataOutput) {
						client.dataOutput.writeUTF("ATTACK 3");
					}
					client.player.lastAttack = System.currentTimeMillis();
				} else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
					synchronized (client.dataOutput) {
						client.dataOutput.writeUTF("ATTACK 0");
					}
					client.player.lastAttack = System.currentTimeMillis();
				} else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
					synchronized (client.dataOutput) {
						client.dataOutput.writeUTF("ATTACK 1");
					}
					client.player.lastAttack = System.currentTimeMillis();
				}
			} catch (Exception e) {
				e.printStackTrace();
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
