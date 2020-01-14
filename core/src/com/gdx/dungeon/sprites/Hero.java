package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Hero extends Sprite {

	public double health = 3.0;
	public int level = 0;
	int frameCount = 6;
	float cycleTime = 0.5f;
	float maxFrameTime;
	float currentFrameTime;
	public int frame;
	public Array<TextureRegion> frames = new Array<TextureRegion>();


	public Hero(Texture texture) {
		super(texture);
		TextureRegion region = new TextureRegion(texture);
		int frameWidth = region.getRegionWidth() / frameCount;
		TextureRegion temp;
		for(int i = 0; i < frameCount; ++i) {
			temp = new TextureRegion(region, i * frameWidth, 0, frameWidth, region.getRegionHeight());
			frames.add(temp);
		}
		maxFrameTime = cycleTime / frameCount;
		frame = 0;
		this.setScale(2.35f);
	}

	public boolean didHeroMove(float newX, float newY) {
		if (newX == this.getX() && newY == this.getY())
			return false;
		return true;
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
