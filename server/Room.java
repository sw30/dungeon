import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Server's room
 */
public class Room {	//room means room on the server, which contains many dungeons; each dungeon is just a room inside a room

	List<PlayerData> players = new ArrayList<PlayerData>();
	int roomID;
	List<Dungeon> dungeon = new ArrayList<Dungeon>();
	PlayerData loser = null;
	PlayerData winner = null;

	/**
	 * Adds randomly generated dungeon's room inside a server's room
	 */
	public void addDungeon() {
		if (dungeon.size() == 0)
			dungeon.add(new Dungeon(0, -1, -1, -1, -1));
		else {
			Random r = new Random();
			int newID = dungeon.size();
			int id = r.nextInt(newID);
			while (!dungeon.get(id).areAvailableDoors()) {
				id++;
				id = id % newID;
			}
			int direction = r.nextInt(4);
			while (dungeon.get(id).direction[direction] != -1) {
				direction++;
				direction = direction % 4;
			}
			dungeon.get(id).direction[direction] = newID;
			int LEFT = -1, RIGHT = -1, UP = -1, DOWN = -1;
			if (direction == 0)
				RIGHT = id;
			else if (direction == 1)
				LEFT = id;
			else if (direction == 2)
				DOWN = id;
			else if (direction == 3)
				UP = id;
			Dungeon newDungeon = new Dungeon(newID, LEFT, RIGHT, UP, DOWN);
			dungeon.add(newDungeon);
		}
	}

	/**
	 * Creates Room, generated 10 dungeon's rooms inside it
	 * @param player1
	 * @param player2
	 * @param roomID - should be unique for a whole server
	 */
	public Room(PlayerData player1, PlayerData player2, int roomID) {
		for (int i = 0; i < 10; ++i) {
			addDungeon();
			if (i != 0 && i != 9)
				dungeon.get(dungeon.size() - 1).spawnRandomMonsters();
		}
		players.add(player1);
		players.get(0).currentRoom = this;
		players.get(0).currentDungeon = dungeon.get(0);
		players.add(player2);
		players.get(1).currentRoom = this;
		players.get(1).currentDungeon = dungeon.get(dungeon.size() - 1);
		this.roomID = roomID;
		players.get(0).procedure = 1;
		players.get(1).procedure = 1;
	}

	/**
	 * Checks if both players are alive and in the same room.
	 * Useful method for trying to check if doors in the dungeon's room should be opened
	 * @return true if both players are alive and in one dungeon's room or false if not
	 */
	public boolean areBothPlayersAliveInDungeon() {
		if (players.size() != 2 || players.get(0).currentDungeon == null)
			return false;
		if (players.get(0).currentDungeon == players.get(1).currentDungeon && players.get(0).currentHealth > 0 && players.get(1).currentHealth > 0)
			return true;
		return false;
	}

}