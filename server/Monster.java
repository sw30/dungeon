import java.io.IOException;


/**
 * A class that represents monsters: bot which player is supposed to fight with
 */
public class Monster {
	public double x;
	public double y;
	public String type;
	public double health;
	public double velocity;
	public int id;
	public long coolDown = -1001;
	double m = 20.0;


	/**
	 *
	 * @param id - ID for a monster, should be unique for each monster in a Dungeon
	 * @param x - monster's x on map
	 * @param y - monster's y on map
	 * @param type - Monster's type: FLY, GOBLIN or SLIME
	 * @throws IOException - thrown if passed wrong monster's type
	 */
	public Monster(int id, int x, int y, String type) throws IOException {
		this.x = x;
		this.y = y;
		this.type = type;
		this.id = id;
		if (type == "FLY") {
			health = 0.5;
			velocity = 2.5;
		} else if (type == "GOBLIN") {
			health = 2.0;
			velocity = 1;
		} else if (type == "SLIME") {
			health = 5.0;
			velocity = 0.5;
		} else throw new IOException("Illegal monster type");
	}


	/**
	 * If player can be attacked - attacks them
	 * @param player - player to be attacked
	 * @return true if attacked successfully or false if not
	 */
	public boolean beAttacked(PlayerData player) {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			if (player != null && player.sharpness)
				health -= 1.0;
			else
				health -= 0.5;
			return true;
		}
		return false;
	}


	/**
	 * Moves monster in player's direction
	 * @param player - player to be followed
	 */
	void move(PlayerData player) throws IOException {
		if (player.x + m > x && player.x - m > x)
			x += velocity;
		if (player.x - m < x && player.x + m < x)
			x -= velocity;
		if (player.y + m + 5 > y && player.y + 5 > y)
			y += velocity;
		if (player.y - m - 5 < y && player.y - 5 < y)
			y -= velocity;
		attack(player);
	}

	/**
	 * Checks if player is can be attacked. If so, attacks him
	 * @param player - player to be attacked
	 */
	void attack(PlayerData player) throws IOException {
		if (player.x - 25 < x && x < player.x + 30) {
			if (player.y - 10 < y && y < player.y + 20) {
				player.beAttacked(null);
				synchronized (player.clientOutput) {
					player.clientOutput.writeUTF("HEALTH_UPDATE " + player.currentHealth + " " + player.maxHealth);
					player.clientOutput.writeUTF("CHANGE_SPRITE " + player.socketID);
				}
			}
		}
	}

}