package com.gdx.dungeon.sprites;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * A class containing every local information about hero and his animations
 * See Animation documentation for details
 */
public class Hero {

	public double health = 6.0;
	public double maxHealth = 6.0;
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
	int frameWidth;
	int frameHeight;
	public Sprite attackSprite;
	public long lastAttack = -100;
	Texture attackTexture;
	public Texture hitHeroTexture;
	public Texture texture;

	/**
	 * @param texture - hero's default spritesheet
	 * @param attackTexture - attack's texture
	 * @param hitHeroTexture - hero's spritesheet shown while hero is being hit
	 */
	public Hero(Texture texture, Texture attackTexture, Texture hitHeroTexture) {
		this.attackTexture = attackTexture;
		this.hitHeroTexture = hitHeroTexture;
		this.texture = texture;
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
		attackSprite = new Sprite(this.attackTexture);
	}

	/**
	 * changes current texture to another
	 * @param otherTexture - texture to be changed to
	 */
	public void changeTexture(Texture otherTexture) {
		frames.clear();
		TextureRegion region = new TextureRegion(otherTexture);
		TextureRegion temp;
		for(int i = 0; i < frameCount; ++i) {
			temp = new TextureRegion(region, i * frameWidth, 0, frameWidth, frameHeight);
			frames.add(temp);
		}
	}

	/**
	 * Sets hero position
	 */
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

	/**
	 * Checks if hero has moved
	 * @param newX - place hero wants to move on X-axis
	 * @param newY - place hero wants to move on Y-axis
	 * @return
	 */
	public boolean didHeroMove(float newX, float newY) {
		if (newX == this.getX() && newY == this.getY())
			return false;
		return true;
	}


	public void setScale(float scale){
		scaleX = scale;
		scaleY = scale;
	}

	/**
	 * draws current frame
	 * @param batch
	 */
	public void draw(Batch batch) {
		if (frames.size != 0) {
			frame = frame % frames.size;
			batch.draw(frames.get(frame), x, y, (frameWidth * scaleX), (frameHeight * scaleY));
		}
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
