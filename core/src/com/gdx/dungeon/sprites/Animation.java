package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;


public class Animation {
	Array<TextureRegion> frames = new Array<TextureRegion>();
	float maxFrameTime;
	int frameCount;
	public int frame = 0;
	float scaleX = 1;
	float scaleY = 1;
	int frameWidth;
	int frameHeight;
	float currentFrameTime = 0;

	/**
	 * @param texture
	 * @param frameCount - amount of frames in the texture
	 * @param cycleTime - how long frame shall remain on screen
	 */
	public Animation(Texture texture, int frameCount, float cycleTime){
		this.frameCount = frameCount;
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

	}

	/**
	 * Changes animation frame if frame cycle is over
	 * @param dt - deltatime
	 */
	public void update(float dt){
		currentFrameTime += dt;
		if (currentFrameTime > maxFrameTime){
			frame++;
			currentFrameTime = 0;
		}
		if(frame >= frameCount)
			frame = 0;

	}

	/**
	 * Sets scale for animation
	 * @param scale
	 */
	public void setScale(float scale){
		scaleX = scale;
		scaleY = scale;
	}

	/**
	 * returns current frame of animation
	 * @return - current frame of animation
	 */
	public int getFrame() {
		return frame;
	}

}