package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class Hero extends Sprite {

	public Vector2 previousPosition;

	public Hero(Texture texture) {
		super(texture);
		previousPosition = new Vector2(getX(), getY());
		this.setScale(2.35f);
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
