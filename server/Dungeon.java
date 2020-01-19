import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dungeon represents a room of a dungeon in a server's room
 */
public class Dungeon {
	int ID;
	List<Monster> monsters = new ArrayList<Monster>();
	public int direction[] = new int[4];
	//LEFT, RIGHT, UP, DOWN
	public boolean wasEmpty = true;
	public boolean wasRewarded = false;

	public Dungeon(int ID, int LEFT, int RIGHT, int UP, int DOWN) {
		this.ID = ID;
		direction[0] = LEFT;
		direction[1] = RIGHT;
		direction[2] = UP;
		direction[3] = DOWN;
	}


	/**
	 * Spawns randomly chosen set of available monsters: a fly, a goblin or a slime
	 */
	public void spawnRandomMonsters() {
		Random r = new Random();
		int dice = r.nextInt(5);
		try {
			if (dice == 0) {
				monsters.add(new Monster(0, 100, 220, "FLY"));
				monsters.add(new Monster(1, 200, 180, "FLY"));
				monsters.add(new Monster(2, 300, 200, "FLY"));
				monsters.add(new Monster(3, 400, 240, "FLY"));
			} else if (dice == 1) {
				monsters.add(new Monster(0, 400, 200, "GOBLIN"));
				monsters.add(new Monster(1, 250, 100, "SLIME"));
			} else if (dice == 2) {
				monsters.add(new Monster(0, 100, 100, "FLY"));
				monsters.add(new Monster(1, 200, 100, "GOBLIN"));
				monsters.add(new Monster(2, 300, 100, "FLY"));
			} else if (dice == 3) {
				monsters.add(new Monster(0, 250, 100, "SLIME"));
			} else if (dice == 4) {
				monsters.add(new Monster(0, 300, 200, "SLIME"));
				monsters.add(new Monster(1, 100, 220, "FLY"));
				monsters.add(new Monster(2, 100, 300, "FLY"));
			}
		} catch (Exception e) {}
		wasEmpty = false;
	}

	/**
	 *
	 * @return true if monsters are killed and false if there are still alive monsters
	 */
	public boolean areMonstersKilled() {
		if (monsters.size() == 0)
			return true;
		return false;
	}

	/**
	 *
	 * @return true if there are any spots where doors could be placed
	 */
	public boolean areAvailableDoors() {
		for (int i = 0; i < 4; ++i)
			if (direction[i] == -1)
				return true;
		return false;
	}
}