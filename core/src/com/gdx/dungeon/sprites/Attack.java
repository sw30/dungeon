package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Attack extends Sprite {
	public float attackX;
	public float attackY;

	public Attack(float x, float y, Texture texture) {
		super(texture);
		attackX = x;
		attackY = y;
	}
}