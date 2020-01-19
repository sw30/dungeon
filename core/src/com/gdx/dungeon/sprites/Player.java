package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;


public class Player extends Hero {

	Vector2 previousPosition;

	public Player(Texture texture, Texture attackTexture, Texture hitHeroTexture) {
		super(texture, attackTexture, hitHeroTexture);
		previousPosition = new Vector2(getX(), getY());
	}



}
