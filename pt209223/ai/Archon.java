package pt209223.ai;

import battlecode.common.*;
import java.util.*;

import pt209223.navigation.*;
import pt209223.communication.*;

public class Archon extends AbstractRobot {
	/*
	 * Archon o najwyzszym ID jest liderem.
	 * 
	 * ...
	 */

	enum Strategy { ECONOMY, FIGTH };

	private LinkedList<MapLocation> fluxKnown;  // lista znanych fluxow
	private LinkedList<MapLocation> fluxTaken;  // lista moich fluxow
	private LinkedList<MapLocation> fluxEnemy;  // lista przeciwnika fluxow
	private LinkedList<MapLocation> fluxSpent;  // lista zuzytych fluxow
	private LinkedList<MapLocation> stairs;     // lista schodkow(dla Workera)

	private boolean fluxDetected;
	private int lastBroadcast;                  // kiedy byl poprzedni broadcast
	private int lastEnemiesNoticed;             // kiedy ostatnio widziano wroga
	private MapLocation target;                 // obrany cel
	private boolean immediatelySend;            // wyslij broadcast natychmiast
	private boolean amILeader;                  // czy jestem liderem?
	private Strategy strategy;                  // obrana strategia
	private int soldierArmy;                    // licznosc armi soldierow
	private int cannonArmy;                     // licznosc armi cannonow
	private int countdown;
	private int lastSpawn;

	public Archon(RobotController _rc) 
	{
		super(_rc);
		fluxKnown = new LinkedList<MapLocation>();
		fluxTaken = new LinkedList<MapLocation>();
		fluxEnemy = new LinkedList<MapLocation>();
		fluxSpent = new LinkedList<MapLocation>();
		fluxDetected = false;
		stairs = new LinkedList<MapLocation>();
		lastBroadcast = 0;
		lastEnemiesNoticed = 0;
		target = null;
		immediatelySend = false;
		amILeader = false;
		strategy = Strategy.ECONOMY;
		mission = Mission.FIND_FLUX;
		soldierArmy = 5;
		cannonArmy = 1;
		countdown = 0;
		lastSpawn = 0;
	}

	// Dodawanie informacji co robot robi.
	public void info(String descr)
	{
		for (int i = 1; i < infos.length; ++i)
			infos[i-1] = infos[i];

		infos[infos.length - 1] = Clock.getRoundNum() + "@" + mission + ": " + descr;

		for (int i = 0; i < infos.length; ++i)
			rc.setIndicatorString(i, infos[i]);
	}

	public void run() 
	{
		while (true) {
			try {
				switch (mission) {
					case FIND_FLUX:   do_find_flux();  break; // Szukaj fluxa
					case USE_FLUX:    do_use_flux();   break; // Wydobywaj fluxa
					case ESCAPE:      do_escape();     break; // Ucieczka :P
					case FIND_ENEMY:  do_find_enemy(); break; // Znajdz wroga
					case PREPARE:     do_prepare();    break; // Przygotuj sie 
					case ATTACK:      do_attack();     break; // Attaakckkk!
					default:
						System.out.println("Nieoczekiwana misja: "+mission);
				}
			}
			catch (Exception e) { }
		}
	}

	public void do_escape() throws GameActionException
	{
		fastScan(); // tylko szybki scan.

		if (enemies.isEmpty()) {
			// TODO: Moze warto byloby wracac do ktorego wlasnego fluxa
			mission = Mission.PREPARE;
		} else {
			// Wybieramy droge ucieczki.
			// TODO: Czasem uciekajacy Archon grzeznie w rogu planszy.
				
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
				
			if (rc.getDirection().equals(d)) rc.moveForward(); 
			else if (rc.getDirection().equals(d.opposite())) rc.moveBackward();
			else {
				rc.setDirection(d);
				waitForMove();
				rc.moveForward();
			}

			rc.yield();
		}

	}

	public void do_attack() throws GameActionException
	{
		fastScan();

		if (rc.getEnergonLevel() < 0.2*rc.getMaxEnergonLevel()) {
			mission = Mission.ESCAPE;
			return;
		}

		if (rc.getEnergonLevel() < 0.4*rc.getMaxEnergonLevel() &&
				soldiers.isEmpty() && cannons.isEmpty()) {
			mission = Mission.ESCAPE;
			return;
		}

		boolean hasChanneler = false;
		if (!enemies.isEmpty())
			for (RInfo r : enemies) 
				if (r.inf.type.equals(RobotType.CHANNELER)) { hasChanneler = true; break; }

		if (hasChanneler) {
			info("Ma Channelera, wybuduje Cannona");
			if ( cannons.size()  <  1*cannonArmy  ) spawn(RobotType.CANNON);
		} else {
			if ( soldiers.size() <  3             ) spawn(RobotType.SOLDIER);
			if ( cannons.size()  <  1*cannonArmy  ) spawn(RobotType.CANNON);
			if ( soldiers.size() <  1*soldierArmy ) spawn(RobotType.SOLDIER);
		}

		transferEnergonTo(soldiers);
		transferEnergonTo(cannons);

		talk();

		if (enemies.isEmpty()) {
			Direction fluxDir = getDirectionToFlux();
			if (Direction.OMNI.equals(fluxDir)) {
					mission = Mission.USE_FLUX; 
				radio.sayFluxTaken();
				findStairs();
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
		} else 
			rc.yield();
	}

	public void findStairs()
	{
		expensiveScan();
		stairs.clear();
		stairs.addFirst(rc.getLocation());
		TInfo curr = getFromMap(rc.getLocation());
		
		String is = "Schody :";

		while (null != curr &&  stairs.size() < 3) {
			Direction d = Direction.WEST;
			TInfo best = null;
			int h = curr.height + curr.blocks;
			
			for (int i = 0; i < 8; ++i) {
				if (stairs.contains(curr.loc.add(d))) continue;
				TInfo next = getFromMap(curr.loc.add(d));
				if (null == next) continue;
				int nh = next.height + next.blocks;
				if (null == best || nh > h) { h = nh; best = next; }
				d = d.rotateRight();
			}

			if (null == best || h == 0) break;

			is += (" " + curr.loc.directionTo(best.loc));
			stairs.addFirst(best.loc);
			curr = best;
		}

		info(is);
	}

	public void do_prepare() throws GameActionException
	{
		countdown = 50;

		fastScan();
	
		if (!enemies.isEmpty()) {
			boolean toEscape = false;
			for (RInfo r : enemies) {
				if (r.inf.type.equals(RobotType.CANNON)) { 
					if ((soldiers.isEmpty() || rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel()) &&
							rc.getLocation().distanceSquaredTo(r.inf.location) <= 25) { toEscape = true; break; }
				} else if (r.inf.type.equals(RobotType.SOLDIER)) {
					if ((soldiers.isEmpty() && rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel()) &&
							rc.getLocation().distanceSquaredTo(r.inf.location) <= 9) {	toEscape = true; break; }
				}
			}
			if (toEscape) { mission = Mission.ESCAPE; return; }
		}

		transferEnergonTo(soldiers);
		transferEnergonTo(cannons);

		if      ( cannons.size()  <  cannonArmy  ) spawn(RobotType.CANNON);
		else if ( soldiers.size() <  soldierArmy ) spawn(RobotType.SOLDIER);
	
		// listen ();

		if (cannons.size() >= cannonArmy && soldiers.size() >= soldierArmy) {
			info("A teraz czas na atak");
			/*if (countdown > 0) --countdown;
			else*/ mission = Mission.ATTACK;
		}

		if (lastSpawn > 0) {
			radio.sayHello();
			radio.broadcast();
			lastSpawn = 0;
		}

		rc.yield();
	}

	public void do_use_flux() throws GameActionException
	{
		/*
		 * TODO: Postarac sie aby wysylac jedynie mozliwie 
		 * najaktualniejsze dane. Glownie chodzi o pozycje
		 * wroga. Szczegolnie wazne jest to dla Cannona.
		 */
		fastScan();

		transferEnergonTo(workers);
		transferEnergonTo(cannons);
		transferEnergonTo(soldiers);
		transferEnergonTo(channelers);
		transferEnergonTo(scouts);

		listen ();
	
		if (enemies.isEmpty()) {
			if (workers.size() < 1) {
				if (spawn(RobotType.WORKER))
					radio.sayStairs(stairs);
			}

			if (channelers.isEmpty()) { 
				spawn(RobotType.CHANNELER);
			} else if (cannons.size() < cannonArmy) {
				spawn(RobotType.CANNON);
			} else if (soldiers.size() < soldierArmy) {
				spawn(RobotType.SOLDIER);
			}

			if (lastSpawn > 0) 
				radio.sayHello();

		} else {
			if (rc.getEnergonLevel() < 0.5*rc.getMaxEnergonLevel() &&
					channelers.isEmpty() && enemies.size() > soldiers.size()) {
				info("Atakuja... ratunku..."); // TODO: trzeba by tu dac jakis rozsadniejszy warunek
				mission = Mission.ESCAPE; return; 
			} 

			if (cannons.size() < 1) {
				if (spawn(RobotType.CANNON))
					radio.sayHello();
			}

			target = chooseTarget();
			radio.sayEnemies(enemies);
			radio.sayAttack(target);
		}

		if (radio.isOutgoing()) { radio.broadcast(); lastSpawn = 0; }

		waitForMove();
	}

	public void do_find_enemy()
	{
		// Do zmiany
		mission = Mission.FIND_FLUX;
	}

	public void do_find_flux() throws GameActionException
	{
		/*
		 * TODO: Rozpoznawanie istniejacych schodow. 
		 * Nie bedzie to trudne.
		 */

		fastScan();
		listen();
	
		if (!enemies.isEmpty()) { 
			mission = Mission.ESCAPE; 
			radio.sayEnemies(enemies);
			radio.broadcast();
			return; 
		}

		// Trzeba zmienic :
		Direction fluxDir = getDirectionToFlux();

		if (Direction.OMNI.equals(fluxDir)) {
			findStairs();
			mission = Mission.USE_FLUX; 
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
					//if (rc.getRobot().getID() < );
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
		lastSpawn = 0;
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

			// TODO : to mozna troche efektywniej napisac
			
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
				lastSpawn = Clock.getRoundNum();
				return true;
			}
			catch (Exception e) { 
				info("Nie wyprodukowalem bo: " + e);
			}
		}
		
		return false;
	}
	
	public Direction getDirectionToFlux()
	{
		Direction toNearest = rc.senseDirectionToUnownedFluxDeposit();
		return toNearest;
/*		FluxDeposit[] fds = rc.senseNearbyFluxDeposits();
		
		if (null != fds && fds.length > 0) {
			// Milczaco zakladam z
			if (!fluxDetected) {
				radio.sayFluxFound(fds
			}

			fluxDetected = true;
		} else {
			fluxDetected = false;
	}*/
		
	}


}
