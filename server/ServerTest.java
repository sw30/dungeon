import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DummyServer extends Thread {

	int howManyAccept;
	int port;
	public List <Socket> sockets = new CopyOnWriteArrayList<Socket>();
	public ServerSocket serverSocket;

	public DummyServer(int howManyAccept, int port) {
		this.howManyAccept = howManyAccept;
		this.port = port;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			for (int i = 0; i < howManyAccept; ++i) {
				Socket socket = serverSocket.accept();
				sockets.add(socket);
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
}

class ServerTest {

	static int port = 8081;
	DummyServer server = null;

	@BeforeEach
	void setUp() {
		port++;
	}

	@AfterEach
	void tearDown() {
		try {
			if (server != null && server.serverSocket.isClosed())
				server.serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	void PlayerDataNullSocket() {
		try {
			new PlayerData(5, 5, "test", null);
			fail("PlayerData can't have null socket");
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataConnectedSocket() {
		try {
			Socket socket = new Socket();
			new PlayerData(5, 5, "test", socket);
			fail("Socket must be connected");
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataConnectionRefused() {
		try {
			Socket socket = new Socket("localhost", port);
			new PlayerData(5, 5, "test", socket);
			fail("Connection has not been refused");
		} catch (Exception e) { }
	}

	@Test
	void PlayerDataTestAcceptingSocket() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			new PlayerData(5, 5, "test", server.sockets.get(0));
		} catch (Exception e) {
			fail("Socket was not accepted");
		}
	}

	@Test
	void PlayerDataTestAttack() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			player.attack(0);
			if (player.lastAttack < 0)
				fail("Couldn't attack");
		} catch (Exception e) {}
	}


	@Test
	void PlayerDataTestMovingAttack() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			player.attack(0);
			player.x = 6;
			player.y = 6;
			player.updateAttackXY();
			if (player.attackX != player.x - player.attackRange)
				fail("AttackX equals " + player.attackX + ", but should equal " + (player.x - player.attackRange));
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataTestAttackDirection() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			if (player.checkIfAttacked(5, 5))
				fail("The player was attacked, but there was no direction of attack");
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataTestShield() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			player.shield = 1;
			double health = player.currentHealth;
			player.beAttacked(player);
			if (player.shield != 0)
				fail("Player didn't lose his shield");
			else if (player.currentHealth != health)
				fail("Player lost their health while holding shield");
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataTestMultipleShields() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			player.shield = 5;
			double health = player.currentHealth;
			player.beAttacked(player);
			for (int i = 0; i < 6; ++i) {
				player.coolDown = System.currentTimeMillis() - 10000;
				player.beAttacked(player);
			}
			if (player.shield != 0)
				fail("Player has negative amount of shields: " + player.shield);
			else if (player.currentHealth == health)
				fail("Player haven't lost his health");
		} catch (Exception e) {}
	}

	@Test
	void PlayerDataTestCooldown() {
		try {
			server = new DummyServer(1, port);
			server.start();
			Socket socket = new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player = new PlayerData(5, 5, "test", server.sockets.get(0));
			player.shield = 5;
			double health = player.currentHealth;
			player.beAttacked(player);
			for (int i = 0; i < 6; ++i)
				player.beAttacked(player);
			if (player.shield != 4)
				fail("Player has been attacked during cooldown time");
		} catch (Exception e) {}
	}

	@Test
	void RoomTestInit() {
		try {
			Random r = new Random();
			port += r.nextInt(100);
			server = new DummyServer(2, port);
			server.start();
			new Socket("localhost", port);
			new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player1 = new PlayerData(100, 100, "0", server.sockets.get(0));
			PlayerData player2 = new PlayerData(200, 200, "1", server.sockets.get(1));
			new Room(player1, player2, 0);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	void RoomTestNullDungeon() {
		try {
			server = new DummyServer(2, port);
			server.start();
			new Socket("localhost", port);
			new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player1 = new PlayerData(100, 100, "0", server.sockets.get(0));
			PlayerData player2 = new PlayerData(200, 200, "1", server.sockets.get(1));
			Room room = new Room(player1, player2, 0);
			if (room.areBothPlayersAliveInDungeon())
				fail("Both players are alive in null dungeon");
		} catch (Exception e) { }
	}

	@Test
	void RoomAddDungeon() {
		try {
			server = new DummyServer(2, port);
			server.start();
			new Socket("localhost", port);
			new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player1 = new PlayerData(100, 100, "0", server.sockets.get(0));
			PlayerData player2 = new PlayerData(200, 200, "1", server.sockets.get(1));
			Room room = new Room(player1, player2, 0);
			int dungeonAmount = room.dungeon.size();
			room.addDungeon();
			if (room.dungeon.size() != dungeonAmount + 1)
				fail("Failed to add dungeon to room. Dungeons' array size is " + room.dungeon.size());
		} catch (Exception e) { }
	}


	@Test
	void RoomTestBothPlayersInDungeon() {
		try {
			server = new DummyServer(2, port);
			server.start();
			new Socket("localhost", port);
			new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player1 = new PlayerData(100, 100, "0", server.sockets.get(0));
			PlayerData player2 = new PlayerData(200, 200, "1", server.sockets.get(1));
			Room room = new Room(player1, player2, 0);
			player1.currentDungeon = room.dungeon.get(room.dungeon.size() - 1);
			player2.currentDungeon = room.dungeon.get(room.dungeon.size() - 1);
			if (!room.areBothPlayersAliveInDungeon())
				fail("Both players should be alive in one dungeon");
		} catch (Exception e) { }
	}

	@Test
	void RoomTestBothPlayersDead() {
		try {
			server = new DummyServer(2, port);
			server.start();
			new Socket("localhost", port);
			new Socket("localhost", port);
			Thread.sleep(100);
			PlayerData player1 = new PlayerData(100, 100, "0", server.sockets.get(0));
			PlayerData player2 = new PlayerData(200, 200, "1", server.sockets.get(1));
			Room room = new Room(player1, player2, 0);
			player1.currentDungeon = room.dungeon.get(room.dungeon.size() - 1);
			player2.currentDungeon = room.dungeon.get(room.dungeon.size() - 1);
			player1.currentHealth = 0;
			if (room.areBothPlayersAliveInDungeon())
				fail("Both players are in one dungeon, but one is dead");
		} catch (Exception e) { }
	}

	@Test
	void MonsterIllegalType() {
		try {
			Monster monster = new Monster(0, 50, 50, "TEST");
			fail("Illegal monster type");
		} catch (Exception e) { }
	}

	@Test
	void MonsterType() {
		try {
			Monster monster = new Monster(0, 50, 50, "FLY");
		} catch (Exception e) {
			fail("Monster type was legal");
		}
	}

	@Test
	void DungeonMonsters() {
		Dungeon dungeon = new Dungeon(0, -1, -1, -1, -1);
		dungeon.spawnRandomMonsters();
		if (dungeon.areMonstersKilled())
			fail("Monsters aren't supposed to be killed since no one is in the dungeon");
	}

	@Test
	void DungeonAvailableRooms() {
		Dungeon dungeon = new Dungeon(0, 1, -1, -1, -1);
		if (!dungeon.areAvailableDoors())
			fail("Left doors are available");
	}

	@Test
	void DungeonNonAvailableRooms() {
		Dungeon dungeon = new Dungeon(0, -1, -1, -1, -1);
		if (!dungeon.areAvailableDoors())
			fail("No doors are available");
	}


}