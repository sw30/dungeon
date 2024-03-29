import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class PlayerData {
	public double x;
	public double y;
	public String socketID;
	public Socket socket;
	public DataOutputStream clientOutput;
	public DataInputStream clientInput;
	public Room currentRoom = null;
	public Dungeon currentDungeon = null;

	public double attackRange = 20;
	public double maxHealth = 6.0;
	public double currentHealth = 6.0;
	public int level = 0;

	public long lastAttack = -100;
	public double attackX;
	public double attackY;
	public int lastDirection;
	public long coolDown = -1001;
	public int shield = 0;
	public boolean sharpness = false;
	public long sharpnessLifeTime = -30001;

	public boolean hasLost = false;
	public int procedure = 0;


	/**
	 * Collects information about player in the server
	 * @param x
	 * @param y
	 * @param socketID - should be unique in the whole server
	 * @param socket
	 * @throws IOException
	 */
	public PlayerData(double x, double y, String socketID, Socket socket) throws IOException {
		this.x = x;
		this.y = y;
		this.socketID = socketID;
		this.socket = socket;
		clientOutput = new DataOutputStream(socket.getOutputStream());
		clientInput = new DataInputStream(socket.getInputStream());
	}

	/**
	 * Collects information to let client draw attack
	 * @param direction: 0 - LEFT, 1 - RIGHT, 2 - UP, 3 - DOWN
	 */
	public void attack(int direction) {
		lastDirection = direction;
		lastAttack = System.currentTimeMillis();
		attackX = x;
		attackY = y;
		if (direction == 0)
			attackX -= attackRange;
		else if (direction == 1)
			attackX += attackRange;
		else if (direction == 2)
			attackY += attackRange;
		else if (direction == 3)
			attackY -= attackRange;
	}

	/**
	 * Method should be called anytime player moves to let his attack be drawn everywhere he moves
	 */
	public void updateAttackXY() {
		attackX = x;
		attackY = y;
		if (lastDirection == 0)
			attackX -= attackRange;
		else if (lastDirection == 1)
			attackX += attackRange;
		else if (lastDirection == 2)
			attackY += attackRange;
		else if (lastDirection == 3)
			attackY -= attackRange;
	}

	/**
	 * Literally checks if player on (x, y) is attacked
	 * @param x - player's x who is supposed to be attacked
	 * @param y - player's y
	 * @return true if player is attacked and false if not
	 */
	public boolean checkIfAttacked(double x, double y) {
		if (!(lastAttack > System.currentTimeMillis() - 100))
			return false;
		updateAttackXY();
		double up = 20;
		double down = 20;
		double left = 15;
		double right = 15;
		if (lastDirection == 0) {
			if (x + left >= attackX - attackRange && x <= attackX && y <= attackY + down && y >= attackY - up)
				return true;
		} else if (lastDirection == 1) {
			if (x >= attackX && x <= attackX + attackRange && y <= attackY + down && y >= attackY - up)
				return true;
		} else if (lastDirection == 2) {
			if (y >= attackY && y <= attackY + attackRange && x <= attackX + left && x >= attackX - right)
				return true;
		} else if (lastDirection == 3) {
			if (y >= attackY - attackRange && y <= attackY && x <= attackX + left && x >= attackX - right)
				return true;
		}
		return false;
	}

	/**
	 * Makes player in the parameter attacked
	 * @param player - player to be attacked
	 * @return true on success or false
	 */
	public boolean beAttacked(PlayerData player) {
		if (coolDown + 1000 < System.currentTimeMillis()) {
			coolDown = System.currentTimeMillis();
			if (shield <= 0) {
				if (player != null && player.sharpness)
					currentHealth -= 1.0;
				else
					currentHealth -= 0.5;
			} else {
				shield--;
				if (shield < 0)	shield = 0;
			}
			return true;
		}
		return false;
	}
}