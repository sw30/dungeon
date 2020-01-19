package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Door extends Sprite {

	boolean areOpen;

	/**
	 * Creates door with a texture, which are closed on default
	 * @param texture - doors texture
	 */
	public Door(Texture texture) {
		super(texture);
		areOpen = false;
	}
}
