package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class Hero extends Sprite {

	public double health = 3.0;
	public int level = 0;


	public Hero(Texture texture) {
		super(texture);
		this.setScale(2.35f);
	}



}
