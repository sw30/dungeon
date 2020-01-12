package com.gdx.dungeon.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Player extends Hero {

	Vector2 previousPosition;

	public Player(Texture texture) {
		super(texture);
		previousPosition = new Vector2(getX(), getY());
	}



}
