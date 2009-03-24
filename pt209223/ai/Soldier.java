package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.navigation.Constants.*;

import java.util.*;

public class Soldier extends AbstractRobot {
	protected MapLocation target;
	protected MapLocation archon;
	protected MapLocation nearest;
	protected boolean nearestIsAir;
	protected int lastSeen;

	public Soldier(RobotController _rc)
	{
		super(_rc);
		target = null;
		archon = null;
		nearest = null;
		nearestIsAir = false;
		lastSeen = 0;
	}

	public void run()
	{
		/*
		 * Algorytm Soldiera
		 * 
		 * Albo czeka az bedzie mogl wykonac pierwszy ruch (50 rund), moze wtedy 
		 * sobie poobserwac okolice, pozbierac wiadomosci...
		 *
		 * Albo wykonuje Mission.RANDOM, tj nie wie co ma robic sie kreci po 
		 * okolicy, gdy glodny idzie do Najblizszego Archona.
		 *
		 * Albo wykonuje Mission.ATTACK, tj dostaje od Archona sygnal do ataku
		 * we wskazanym kierunku(ku lokalizacji target).
		 * 
		 */
		mission = Mission.WAIT;

		while (true) {
			try {
				switch (mission) {
					case WAIT:    do_wait();   break; // - oczekiwanie na misje
					case FOLLOW:  do_follow(); break; // - podarzaj za archonem
					case ATTACK:  do_attack(); break; // - atakuj
					default:
						System.out.println("Nieobslugiwana misja: "+mission);
				}
			}
			catch (Exception e) { /*System.out.println("EXCEPTION : " + e);*/ }
		}
	}

	public void do_wait() throws GameActionException
	{
		expensiveScan();

		if (!rc.isMovementActive() && !rc.isAttackActive()) 
			mission = Mission.FOLLOW;
		else {
			radio.receive();

			while (radio.isIncoming()) {
				Message m = radio.get();
				if (null == m) continue;
				if (m.ints[Radio.TYPE] == Radio.HELLO &&
						RobotType.valueOf(m.strings[Radio.HELLO_SENDER_TYPE]).equals(RobotType.ARCHON)) {
					archon = m.locations[Radio.HELLO_SENDER];
					info("Ustawiam se pozycje Archona...");
				}
			}
			
			transferEnergonTo(soldiers);
			transferEnergonTo(cannons);

			rc.yield();
		}
	}

	public void do_follow() throws GameActionException 
	{
		if (rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel() && null != archon) {
			info("Ide do Archona...");
			stepTo(archon);
		} else {
			info("Pochodze se...");
			fastScan();
		
			if (!enemies.isEmpty()) {
				List<RInfoShort> fake = new LinkedList<RInfoShort>();
				chooseTarget(enemies, fake);
				mission = Mission.ATTACK;
				return;
			}

			checkSoldiers();
			checkCannons();

			radio.receive();

			while (radio.isIncoming()) {
				Message m = radio.get();
				if (null == m) continue;
				if (m.ints[Radio.TYPE] == Radio.HELLO &&
						RobotType.valueOf(m.strings[Radio.HELLO_SENDER_TYPE]).equals(RobotType.ARCHON)) {
					archon = m.locations[Radio.HELLO_SENDER];
					info("Ustawiam se pozycje Archona...");
				} else if (m.ints[Radio.TYPE] == Radio.ENEMIES) {
					info("Dostaje pozycje wroga...");
					List<RInfoShort> e2 = new LinkedList<RInfoShort>();
					for (int i = 0; i < m.ints[Radio.ENEMIES_SIZE]; ++i)
						e2.add(new RInfoShort(
									m.locations[Radio.ENEMIES_L_START+i],
									m.ints[Radio.ENEMIES_I_START+i],
									m.strings[Radio.ENEMIES_S_START+i]));
					chooseTarget(enemies, e2);
					mission = Mission.ATTACK;
					return;
				}
			}

			if (null != archon && rc.getLocation().distanceSquaredTo(archon) > 9) {
				stepTo(archon);
			} else {
				Direction d = rc.getDirection();

				switch (rand.nextInt(4)) {
					case 0: d = d.rotateRight(); break;
					case 1: d = d.rotateLeft();  break;
				}
		
				for (RInfo r : soldiers) {
					if (null != archon) {
						if (rc.getLocation().distanceSquaredTo(archon) < 
								r.inf.location.distanceSquaredTo(archon))
							transferEnergonTo(r);
					}
				}

				stepTo(rc.getLocation().add(d));
			}
		}
	}

	public void do_attack() throws GameActionException
	{
		if (null == nearest) {
			info("Atakuje! nearest = null");
			mission = Mission.FOLLOW;
			return;
		}
		
		info("Atakuje! nearest = "+rc.getLocation().distanceSquaredTo(nearest)+" enemies.size(): "+enemies.size());

		if (!rc.isAttackActive()) { // Mozna strzelac
			if (rc.getLocation().isAdjacentTo(nearest)) { // W zasiegu
				if (rc.canAttackSquare(nearest)) {
					if (nearestIsAir) rc.attackAir(nearest);
					else rc.attackGround(nearest);
					info("ATAK!");
				} else { // Trzeba sie obrocic
					// TODO SPRAWDZIC CZY WROG JEST GDZIES NA WPROST !!!
					if (!rc.isMovementActive() && !rc.getLocation().equals(nearest)) {
						rc.setDirection(rc.getLocation().directionTo(nearest));
						rc.yield();
						info("O-OBROT!");
					} else {
						info("CZEKAM :(");
					}

				}
			} else { // Daleko jest wrog.
				stepWarily(nearest);
			}
		} else if (!rc.getLocation().isAdjacentTo(nearest)) {
			// Jak jestesmy daleko...
			stepWarily(nearest);
		}

		int rd = -1;
		LinkedList<RInfoShort> enemies2 = new LinkedList<RInfoShort>();

		while (radio.isIncoming()) {
			Message m = radio.get();
			if (null == m) continue;
			if (m.ints[Radio.TYPE] != Radio.ENEMIES) continue;
			if (m.ints[Radio.ROUND] < rd) continue;
			if (m.ints[Radio.ROUND] != rd) enemies2.clear();
			
			for (int i = 0; i < m.ints[Radio.ENEMIES_SIZE]; ++i)
				enemies2.add(
						new RInfoShort( 
							m.locations[Radio.ENEMIES_L_START+i],
							m.ints[Radio.ENEMIES_I_START+i],
							m.strings[Radio.ENEMIES_S_START+i]));
		}

		fastScan();
		chooseTarget(enemies, enemies2);
		checkSoldiers(); // czy by tu nie podladowac jakies soldziera z frontu
		checkCannons();

		if (nearest == null) {
			info("Nie mam w co strzelac ;((...");
			mission = Mission.FOLLOW;
			return;
		}
	}

	public void checkArchons()
	{
		if (!archons.isEmpty()) {
			int min = INFINITY;
			archon = null;
			for (RInfo r : archons) {
				int len = rc.getLocation().distanceSquaredTo(r.inf.location);
				if (null == archon || len < min) { min = len; archon = r.inf.location; }
			}
		}
	}

	public void checkSoldiers()
	{
		if (!soldiers.isEmpty() && rc.getEnergonLevel() > 0.4*rc.getMaxEnergonLevel()) {
			RInfo who = null;
			if (null != target) {
				int min = INFINITY;
				for (RInfo r : soldiers) {
					if (!r.inf.location.isAdjacentTo(rc.getLocation())) continue;
					if (r.inf.energonLevel > 0.75 * r.inf.maxEnergon) continue;
					int len = r.inf.location.distanceSquaredTo(target);
					if (null == who || len < min) { who = r; min = len; }
				}
			} else if (null != archon) {
				int max = -1;
				for (RInfo r : soldiers) {
					if (!r.inf.location.isAdjacentTo(rc.getLocation())) continue;
					if (r.inf.energonLevel > 0.75 * r.inf.maxEnergon) continue;
					int len = r.inf.location.distanceSquaredTo(archon);
					if (null == who || len > max) { who = r; max = len; }
				}
			}

			if (null != who) {
				try {
					rc.transferEnergon(
							Math.min(who.inf.maxEnergon - who.inf.energonLevel, 5),
							who.inf.location, who.robot.getRobotLevel());
				}
				catch (Exception e) { }
			}
		}
	}

	public void checkCannons()
	{
		if (!cannons.isEmpty() && rc.getEnergonLevel() > 0.4*rc.getMaxEnergonLevel()) {
			RInfo who = null;
			if (null != target) {
				int min = INFINITY;
				for (RInfo r : cannons) {
					if (!r.inf.location.isAdjacentTo(rc.getLocation())) continue;
					if (r.inf.energonLevel > 0.75 * r.inf.maxEnergon) continue;
					int len = r.inf.location.distanceSquaredTo(target);
					if (null == who || len < min) { who = r; min = len; }
				}
			} else if (null != archon) {
				int max = -1;
				for (RInfo r : cannons) {
					if (!r.inf.location.isAdjacentTo(rc.getLocation())) continue;
					if (r.inf.energonLevel > 0.75 * r.inf.maxEnergon) continue;
					int len = r.inf.location.distanceSquaredTo(archon);
					if (null == who || len > max) { who = r; max = len; }
				}
			}

			if (null != who) {
				try {
					rc.transferEnergon(
							Math.min(who.inf.maxEnergon - who.inf.energonLevel, 5),
							who.inf.location, who.robot.getRobotLevel());
				}
				catch (Exception e) { }
			}
		}
	}


	public void chooseTarget(List<RInfo> e1, List<RInfoShort> e2)
	{
		MapLocation p_nearest = nearest;
		boolean p_nearestIsAir = nearestIsAir;
		int p_lastSeen = lastSeen;
		lastSeen = Clock.getRoundNum();
		nearest = null;
		
		int min = INFINITY;
		double energon = INFINITY;
		
		if (!e1.isEmpty()) {
			for (RInfo r : e1) {
				int len = rc.getLocation().distanceSquaredTo(r.inf.location);
				if (null == nearest || (len > 0 && len < min) || 
						(len == min && r.inf.energonLevel < energon)) { 
					min = len; nearest = r.inf.location; 
					energon = r.inf.energonLevel;
					nearestIsAir = r.inf.type.isAirborne();
				}
			}
		}

		if (!e2.isEmpty()) {
			for (RInfoShort r : e2) {
				int len = rc.getLocation().distanceSquaredTo(r.location);
				if (null == nearest || (len > 0 && len < min) || 
						(len == min && r.energon < energon)) { 
					min = len; nearest = r.location; 
					energon = r.energon;
					nearestIsAir = r.type.isAirborne();
				}
			}
		}

		// TODO : Uwaga na channelera
		
		if (null == nearest && p_lastSeen + 5 > lastSeen) {
			nearest = p_nearest;
			nearestIsAir = p_nearestIsAir;
			lastSeen = p_lastSeen;
		}
	}

}
