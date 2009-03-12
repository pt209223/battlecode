package pt209223.ai;

import battlecode.common.*;
import java.util.*;

import pt209223.navigation.*;
import static pt209223.communication.Radio.*;

public class Archon extends AbstractRobot {
	private LinkedList<MapLocation> fluxKnown;
	private LinkedList<MapLocation> fluxTaken;
	private LinkedList<MapLocation> fluxEnemy;
	private LinkedList<MapLocation> stairs;

	private int lastBroadcast;
	private int lastEnemiesNoticed;
	private boolean isChannelerConnected;
	private boolean isWorkerConnected;
	private MapLocation target;

	public Archon(RobotController _rc) 
	{
		super(_rc);
		fluxKnown = new LinkedList<MapLocation>();
		fluxTaken = new LinkedList<MapLocation>();
		fluxEnemy = new LinkedList<MapLocation>();
		stairs = new LinkedList<MapLocation>();
		lastBroadcast = 0;
		lastEnemiesNoticed = 0;
		isChannelerConnected = false;
		isWorkerConnected = false;
		target = null;
	}

	public void run() throws GameActionException 
	{
		info("Szukam fluxa...");
		mission = Mission.GOTO_FLUX;
		
		do_mission();
	}

	public void do_escape() throws GameActionException
	{
		try {
			fastScan();
			
			if (enemies.isEmpty()) mission = Mission.GOTO_FLUX;
			else {
				ListIterator<RInfo> it = enemies.listIterator();
				MapLocation loc = it.next().inf.location;
				while (it.hasNext()) {
					MapLocation next = it.next().inf.location;
					loc = new MapLocation(loc.getX()+next.getX(), loc.getY()+next.getY());
				}
				Direction d = rc.getLocation().directionTo(loc).opposite();

				if (!rc.canMove(d)) {
					if (rc.canMove(d.rotateRight())) d = d.rotateRight();
					else if (rc.canMove(d.rotateLeft())) d = d.rotateLeft();
					else if (rc.canMove(d.rotateRight().rotateRight())) d = d.rotateRight().rotateRight();
					else {
						for (int i = 0; i < 5; ++i) {
							d = d.rotateRight();
							if (rc.canMove(d)) break;
						}
					}
				}
					
				info("Uciekam: " + d);

				waitForMove();
						
				if (rc.getDirection() == d) { 
					rc.moveForward(); 
				} else if (rc.getDirection() == d.opposite()) {
					rc.moveBackward(); rc.yield();
				} else {
					rc.setDirection(d);
					waitForMove();
					rc.moveForward();
				}

				rc.yield();
			}
		}
		catch (Exception e) { }
	}

	public void do_attack() 
	{
		try { 
			fastScan();

			if (rc.getEnergonLevel() < 0.39*rc.getMaxEnergonLevel()) {
				mission = Mission.ESCAPE;
				return;
			}

			if (soldiers.size() <  3 * (1 + archons.size()/2)) {
				spawn(RobotType.SOLDIER);
				info("Tworze Soldiera...");
			}

			if (cannons.size() < 1 * (1 + archons.size()/2)) {
				spawn(RobotType.CANNON);
				info("Tworze Cannona...");
			}

			transferEnergonTo(soldiers);
			transferEnergonTo(cannons);
			radio.sayAttack(target);

			talk();
		}
		catch (Exception e) { }
	}

	public void do_stop() throws GameActionException
	{
		fastScan();

		transferEnergonTo(workers);
		transferEnergonTo(cannons);
		transferEnergonTo(soldiers);
		transferEnergonTo(channelers);
		transferEnergonTo(scouts);

		if (channelers.isEmpty()) isChannelerConnected = false;
		else if (!isChannelerConnected) {
			try {
				info("Pojawil sie Channeler, przywitam sie...");
				radio.sayHello();
				radio.broadcast();
				isChannelerConnected = true;
			}
			catch (Exception e) { }
		}

		if (workers.isEmpty()) isWorkerConnected = false;
		else if (!isWorkerConnected) {
			try {
				info("Pojawil sie Worker, przywitam sie...");
				radio.sayStairs(stairs);
				radio.broadcast();
				isWorkerConnected = true;
			}
			catch (Exception e) { }
		}

		radio.sayHello();

		listen();
		talk();
	
		if (enemies.isEmpty()) {
			if (workers.isEmpty()) {
				spawn(RobotType.WORKER);
				info("Tworze Workera...");
			}

			if (channelers.isEmpty()) { 
				spawn(RobotType.CHANNELER);
				info("Tworze Channlera...");
			}

		} else {
			if (rc.getEnergonLevel() < 0.5*rc.getMaxEnergonLevel() &&
					channelers.isEmpty()) {
				info("Atakuja... ratunku...");
				mission = Mission.ESCAPE; return; 
			}

			if (soldiers.size() < 3*(1 + archons.size()/2)) {
				spawn(RobotType.SOLDIER);
				info("Tworze Soldiera...");
			}

		}

		//...
	}

	public void do_goto_flux() throws GameActionException
	{
		fastScan();
		listen();
		talk();
	
		if (!enemies.isEmpty()) { 
			info("ATTAACKCKK!!");
			target = chooseTarget();
			mission = Mission.ATTACK; 
			return; 
		}

		Direction fluxDir = rc.senseDirectionToUnownedFluxDeposit();

		if (Direction.OMNI == fluxDir) {
			stairs.clear();
			stairs.add(rc.getLocation());
			mission = Mission.STOP;
			radio.sayFluxTaken();
		} else {
			if (Direction.NONE == fluxDir) 
				fluxDir = rc.senseDirectionToOwnedFluxDeposit();
			
			if (!rc.isMovementActive() && !rc.hasActionSet()) {
				if (rc.canMove(fluxDir) && rc.getDirection() != fluxDir) rc.setDirection(fluxDir);
				else if (rc.canMove(rc.getDirection())) rc.moveForward();
				else rc.setDirection(rc.getDirection().rotateRight());
				rc.yield();
			}
		}
	}

	public void listen()
	{
		radio.receive();

		while (radio.isIncoming()) {
			Message msg = radio.get();
			switch (msg.ints[MSG_IDX_TYPE]) {
				case MSG_TYPE_HELLO:
					info(
							"Wita sie z nami : " + msg.strings[MSG_HELLO_S_SENDER] + 
							" (id: " + msg.locations[MSG_HELLO_L_SENDER].toString() + ")");
					break;
				case MSG_TYPE_FLUX_FOUND:
					info(
							"Archon z " + msg.locations[MSG_FLUX_FOUND_L_SENDER] + 
							" znalazl Flux na " + msg.locations[MSG_FLUX_FOUND_L_FOUND]);
					break;
				case MSG_TYPE_FLUX_TAKEN:
					info(
							"Archon z " + msg.locations[MSG_FLUX_TAKEN_L_SENDER] + 
							" przejal Flux");
					break;
				case MSG_TYPE_ENEMIES:
					info(
							"Sa wrogowie, w liczbie: " + msg.ints[MSG_ENEMIES_I_SIZE] + 
							" informacja z " + msg.locations[MSG_ENEMIES_L_SENDER]);
					break;
				case MSG_TYPE_GOTO:
					info("Ktos chce abym gdzies poszedl... Nie ma mowy...");
					break;
				case MSG_TYPE_STAIRS:
					if (stairs.isEmpty() || msg.ints[MSG_STAIRS_I_SIZE] == 0 ||
							msg.locations[MSG_STAIRS_L_START+msg.ints[MSG_STAIRS_I_SIZE]-1].getX() != stairs.getLast().getX() ||
							msg.locations[MSG_STAIRS_L_START+msg.ints[MSG_STAIRS_I_SIZE]-1].getY() != stairs.getLast().getY()) {
						info("Bledne dane o schodach, ignoruje... ("+msg.ints[MSG_STAIRS_I_SIZE]+")");
					} else {
						info("Ustawiam sciezke(schody) do fluxa: " + msg.ints[MSG_STAIRS_I_SIZE]);
						stairs.clear();
						for (int i = MSG_STAIRS_L_START, cnt = 0; cnt < msg.ints[MSG_STAIRS_I_SIZE]; ++i, ++cnt)
							stairs.addFirst(msg.locations[i]);
					}
					break;
				default:
					info("Nie rozpoznalem typu wiadomosci : " + msg.ints[MSG_IDX_TYPE]);
			}
		}

	}

	public void talk()
	{
		int now = Clock.getRoundNum();

		int dist = (mission == Mission.ATTACK) ? 1 : 10;

		if ((!enemies.isEmpty() && lastEnemiesNoticed + dist < now) ||
				(lastBroadcast + dist < now)) {
			if (!enemies.isEmpty()) {
				radio.sayEnemies(enemies);
				target = chooseTarget();
				radio.sayAttack(target);
			}
			if (radio.broadcast()) {
				lastBroadcast = now;
				if (!enemies.isEmpty()) 
					lastEnemiesNoticed = now;
			}
		}
	}

	public MapLocation chooseTarget()
	{
		if (enemies.isEmpty()) return null;

		int min = 1000; // a fe
		MapLocation nearest = null;
		
		for (RInfo r : enemies) {
			int len = rc.getLocation().distanceSquaredTo(r.inf.location);
			if (null == nearest || len < min) { 
				min = len; nearest = r.inf.location;
			}
		}

		return nearest;
	}

	public boolean canSpawnThat(RobotType t)
	{
		return (t.spawnCost() < rc.getEnergonLevel() &&
				rc.getEnergonLevel() - t.spawnCost() > 0.3*rc.getMaxEnergonLevel());
	}

	public boolean spawn(RobotType t)
	{
		if (canSpawnThat(t)) {
			try { 
				Direction d = rc.getDirection();
				if (RobotType.SCOUT == t) {
					for (int i = 0; i < 8; ++i) 
						if (rc.canMove(d)) { d = d.rotateRight(); break; }
						else d = d.rotateRight();
				} else {
					for (int i = 0; i < 8; ++i) 
						if (rc.senseTerrainTile(rc.getLocation().add(d)).getType() == TerrainTile.TerrainType.LAND && 
								rc.senseGroundRobotAtLocation(rc.getLocation().add(d)) == null) break;
						else d = d.rotateRight();
				}
							
				if (rc.getDirection() != d) {
					waitForMove(); 
					rc.setDirection(d);
				}

				waitForMove();
				rc.spawn(t);
				return true;
			}
			catch (Exception e) { 
				info("Nie wyprodukowalem bo: " + e);
			}
		}
		
		return false;
	}

}
