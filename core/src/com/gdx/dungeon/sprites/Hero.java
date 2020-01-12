package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Hero extends Sprite {

	Vector2 previousPosition;

	public Hero(Texture texture) {
		super(texture);
		previousPosition = new Vector2(getX(), getY());
	}

	public boolean hasMoved() {
		if (previousPosition.x != getX() || previousPosition.y != getY()) {
			previousPosition.x = getX();
			previousPosition.y = getY();
			return true;
		}
		return false;
	}
}
