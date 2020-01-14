package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Hero {

	public double health = 3.0;
	public int level = 0;
	int frameCount = 6;
	float cycleTime = 0.5f;
	float maxFrameTime;
	float currentFrameTime;
	public int frame;
	public Array<TextureRegion> frames = new Array<TextureRegion>();
	float scaleX = 1;
	float scaleY = 1;
	float x = 0;
	float y = 0;
	int frameWidth = 1;
	int frameHeight = 1;


	public Hero(Texture texture) {
		TextureRegion region = new TextureRegion(texture);
		frameWidth = region.getRegionWidth() / frameCount;
		TextureRegion temp;
		frameHeight = region.getRegionHeight();
		for(int i = 0; i < frameCount; ++i) {
			temp = new TextureRegion(region, i * frameWidth, 0, frameWidth, frameHeight);
			frames.add(temp);
		}
		maxFrameTime = cycleTime / frameCount;
		frame = 0;
		this.setScale(2.35f);
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public boolean didHeroMove(float newX, float newY) {
		if (newX == this.getX() && newY == this.getY())
			return false;
		return true;
	}

	public void setScale(float scale){
		scaleX = scale;
		scaleY = scale;
	}

	public void draw(Batch batch) {
		batch.draw(frames.get(frame), x, y, (frameWidth * scaleX), (frameHeight * scaleY));
	}


	public void update(float dt){
		currentFrameTime += dt;
		if(currentFrameTime > maxFrameTime){
			frame++;
			currentFrameTime = 0;
		}
		if(frame >= frameCount)
			frame = 0;
	}


}
