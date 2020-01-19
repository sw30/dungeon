package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Door extends Sprite {

	boolean areOpen;

	public Door(Texture texture) {
		super(texture);
		areOpen = false;
	}
}
