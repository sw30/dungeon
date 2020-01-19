package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

/**
 * Has information about monster, inherits animation
 */
public class Monster extends Animation {

	float x;
	float y;
	public int id;

	public Monster(Texture region, int frameCount, int id, float x, float y) {
		super(region, frameCount, 0.5f);
		this.x = x;
		this.y = y;
		this.id = id;
		this.setScale(2.35f);
	}

	/**
	 * Draws current frame
	 */
	public void draw(Batch batch) {
		//System.out.println(frames.size);
		if (frames.size != 0) {
			frame = frame % frames.size;
			batch.draw(frames.get(frame), x, y, (frameWidth * scaleX), (frameHeight * scaleY));
		}
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}
}
