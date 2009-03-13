package pt209223.ai;

import battlecode.common.*;
import java.util.*;

import pt209223.navigation.*;
import pt209223.communication.*;

public class Archon extends AbstractRobot {
	private LinkedList<MapLocation> fluxKnown;
	private LinkedList<MapLocation> fluxTaken;
	private LinkedList<MapLocation> fluxEnemy;
	private LinkedList<MapLocation> stairs;

	private int lastBroadcast;
	private int lastEnemiesNoticed;
	private MapLocation target;
	private boolean immediatelySend;

	public Archon(RobotController _rc) 
	{
		super(_rc);
		fluxKnown = new LinkedList<MapLocation>();
		fluxTaken = new LinkedList<MapLocation>();
		fluxEnemy = new LinkedList<MapLocation>();
		stairs = new LinkedList<MapLocation>();
		lastBroadcast = 0;
		lastEnemiesNoticed = 0;
		target = null;
		immediatelySend = false;
	}

	public void run() 
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
				
				if (rc.getDirection().equals(d)) { 
					rc.moveForward(); 
				} else if (rc.getDirection().equals(d.opposite())) {
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

		listen ();
		talk   ();
	
		if (enemies.isEmpty()) {
			if (workers.isEmpty()) {
				spawn(RobotType.WORKER);
				info("Tworze Workera...");
				radio.sayStairs(stairs);
				immediatelySend = true;
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
			mission = Mission.ATTACK; 
			return; 
		}

		Direction fluxDir = rc.senseDirectionToUnownedFluxDeposit();

		if (Direction.OMNI.equals(fluxDir)) {
			stairs.clear();
			stairs.add(rc.getLocation());
			mission = Mission.STOP;
			radio.sayFluxTaken();
			immediatelySend = true;
		} else {
			if (Direction.NONE.equals(fluxDir)) 
				fluxDir = rc.senseDirectionToOwnedFluxDeposit();
			
			if (!rc.isMovementActive() && !rc.hasActionSet()) {
				if (rc.canMove(fluxDir) && !rc.getDirection().equals(fluxDir)) rc.setDirection(fluxDir);
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
			if (null == msg) continue; // Moze byc przestarzala

			switch (msg.ints[Radio.TYPE]) {
				case Radio.HELLO:
					info(
							"Wita sie z nami : " + msg.strings[Radio.HELLO_SENDER_TYPE] + 
							" (id: " + msg.locations[Radio.HELLO_SENDER].toString() + ")");
					break;
				case Radio.FLUX_FOUND:
					info(
							"Archon z " + msg.locations[Radio.FLUX_FOUND_SENDER] + 
							" znalazl Flux na " + msg.locations[Radio.FLUX_FOUND_FOUND]);
					break;
				case Radio.FLUX_TAKEN:
					info(
							"Archon z " + msg.locations[Radio.FLUX_TAKEN_SENDER] + 
							" przejal Flux");
					break;
				case Radio.ENEMIES:
					info(
							"Sa wrogowie, w liczbie: " + msg.ints[Radio.ENEMIES_SIZE] + 
							" informacja z " + msg.locations[Radio.ENEMIES_SENDER]);
					break;
				case Radio.GOTO:
					info("Ktos chce abym gdzies poszedl... Nie ma mowy...");
					break;
				case Radio.STAIRS:
					if (!stairs.isEmpty() && msg.ints[Radio.STAIRS_SIZE] > 0 &&
							msg.locations[Radio.STAIRS_START+msg.ints[Radio.STAIRS_SIZE]-1].equals(stairs.getLast())) {
						info("Ustawiam sciezke(schody) do fluxa: " + msg.ints[Radio.STAIRS_SIZE]);
						stairs.clear();
						for (int i = Radio.STAIRS_START, cnt = 0; cnt < msg.ints[Radio.STAIRS_SIZE]; ++i, ++cnt)
							stairs.addLast(msg.locations[i]);
					} else if (stairs.isEmpty()) {
						info("Worker podsyla pozycje schodkow, ignoruje...");
					} else if (msg.ints[Radio.STAIRS_SIZE] > 0) {
						info("Worker podeslal bledne pozycje schodkow...");
						info("= " + msg.locations[Radio.STAIRS_START+msg.ints[Radio.STAIRS_SIZE]-1] + " != " + stairs.getLast());
					} else
						info("Worker podeslal pusta liste schodkow...");
					break;
				default:
					info("Nie rozpoznalem typu wiadomosci : " + msg.ints[Radio.TYPE]);
			}
		}
	}

	public void talk()
	{
		int now = Clock.getRoundNum();
		int dist = mission.equals(Mission.ATTACK) ? 1 : 10;

		if (immediatelySend && !rc.hasBroadcastMessage()) immediatelySend = false;
		else {
			// Nie ma wroga i nie czas na broadcast...
			if (enemies.isEmpty() && lastBroadcast + dist >= now) return;

			// Jest wrog ale nie czas na broadcast...
			if (!enemies.isEmpty() && lastEnemiesNoticed + dist >= now) return;
		}

		// Wysylamy info o jednostkach wroga...
		if (!enemies.isEmpty()) {
			target = chooseTarget();
			radio.sayEnemies(enemies);
			radio.sayAttack(target);
			lastEnemiesNoticed = Clock.getRoundNum();
		}

		radio.sayHello();
		radio.broadcast();
		lastBroadcast = Clock.getRoundNum();
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
				if (RobotType.SCOUT.equals(t)) {
					for (int i = 0; i < 8; ++i) 
						if (rc.canMove(d)) { d = d.rotateRight(); break; }
						else d = d.rotateRight();
				} else {
					for (int i = 0; i < 8; ++i) 
						if (rc.senseTerrainTile(rc.getLocation().add(d)).getType().equals(TerrainTile.TerrainType.LAND) && 
								rc.senseGroundRobotAtLocation(rc.getLocation().add(d)) == null) break;
						else d = d.rotateRight();
				}
							
				if (!rc.getDirection().equals(d)) {
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
