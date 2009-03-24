package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.navigation.Constants.*;

import java.util.*;

public class Cannon extends AbstractRobot{
	protected MapLocation target;
	protected MapLocation archon;
	protected MapLocation nearest;
	protected boolean nearestIsAir;
	protected int lastSeen;
	protected int countdown;

	public Cannon(RobotController _rc)
	{
		super(_rc);
		target = null;
		archon = null;
		nearest = null;
		nearestIsAir = false;
		lastSeen = 0;
		countdown = 0;
	}

	public void run()
	{
		/*
		 * Algorytm Cannona (tak ja soldier:P)
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
			catch (Exception e) { 
				System.out.println("EXCEPTION : " + e); 
				e.printStackTrace();
			}
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

			radio.receive();

			while (radio.isIncoming()) {
				Message m = radio.get();
				if (null == m) continue;
				if (m.ints[Radio.TYPE] == Radio.HELLO &&
						RobotType.valueOf(m.strings[Radio.HELLO_SENDER_TYPE]).equals(RobotType.ARCHON)) {
					archon = m.locations[Radio.HELLO_SENDER];
					info("Ustawiam se pozycje Archona...");
				} else if (m.ints[Radio.TYPE] == Radio.ENEMIES) {
					info("Otrzymuje info o wrogu...");
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

		if (rc.getLocation().distanceSquaredTo(nearest) <= 25 && 
				rc.getLocation().distanceSquaredTo(nearest) >= 4) { // Spr zasieg...
			if (!rc.canAttackSquare(nearest)) {
				if (!rc.isMovementActive()) {
					rc.setDirection(rc.getLocation().directionTo(nearest));
					rc.yield();
				} // TODO TO MOZE JEST W CO INNEGO STRZELIC ?
			} else {
				if (!rc.isAttackActive()) {
					if (nearestIsAir) 
						rc.attackAir(nearest);
					else
						rc.attackGround(nearest);
					rc.yield();						
				} else {
					info("Nie moge jeszcze atakowac, idle: " + rc.getRoundsUntilAttackIdle());
					rc.yield();
				}
			}
		} else {
			if (!rc.isMovementActive()) 
				stepTo(nearest);
			else 
				rc.yield();
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
		//checkSoldiers(); // czy by tu nie podladowac jakies soldziera z frontu

		if (nearest == null) {
			mission = Mission.FOLLOW;
			return;
		}
	}

	public void chooseTarget(List<RInfo> e1, List<RInfoShort> e2)
	{
		MapLocation p_nearest = nearest;
		boolean p_nearestIsAir = nearestIsAir;
		nearest = null;
		
		if (e1.isEmpty() && e2.isEmpty()) return;

		int min = INFINITY;
		double energon = INFINITY;
		MapLocation ch_loc = null, ca_loc = null, ar_loc = null;
		int ch_min = INFINITY, ca_min = INFINITY, ar_min = INFINITY;
		
		if (!e1.isEmpty()) {
			for (RInfo r : e1) {
				if (r.inf.location.isAdjacentTo(rc.getLocation())) continue;
				int len = rc.getLocation().distanceSquaredTo(r.inf.location);
				if (null == nearest || (len > 1 && len < min) || 
						(len == min && r.inf.energonLevel < energon)) { 
					min = len; nearest = r.inf.location;
					energon = r.inf.energonLevel;
					nearestIsAir = r.inf.type.isAirborne();
				}
				switch (r.inf.type) {
					case CHANNELER:
						if ((ch_loc == null || (len > 1 && len < ch_min)) && 
								rc.canAttackSquare(r.inf.location)) {
							ch_min = len; ch_loc = r.inf.location;
						} break;
					case CANNON:
						if ((ca_loc == null || (len > 1 && len < ca_min)) && 
								rc.canAttackSquare(r.inf.location)) {
							ca_min = len; ca_loc = r.inf.location;
						} break;
					case ARCHON:
						if ((ar_loc == null || (len > 1 && len < ar_min)) && 
								rc.canAttackSquare(r.inf.location)) {
							ar_min = len; ar_loc = r.inf.location;
						} break;
				}
			}
		}

		if (!e2.isEmpty()) {
			for (RInfoShort r : e2) {
				if (r.location.isAdjacentTo(rc.getLocation())) continue;
				int len = rc.getLocation().distanceSquaredTo(r.location);
				if (null == nearest || (len > 1 && len < min) || 
						(len == min && r.energon < energon)) { 
					min = len; nearest = r.location; 
					energon = r.energon;
					nearestIsAir = r.type.isAirborne();
				}
				switch (r.type) {
					case CHANNELER:
						if ((ch_loc == null || (len > 1 && len < ch_min)) && 
								rc.canAttackSquare(r.location)) {
							ch_min = len; ch_loc = r.location;
						} break;
					case CANNON:
						if ((ca_loc == null || (len > 1 && len < ca_min)) && 
								rc.canAttackSquare(r.location)) {
							ca_min = len; ca_loc = r.location;
						} break;
					case ARCHON:
						if ((ar_loc == null || (len > 1 && len < ar_min)) && 
								rc.canAttackSquare(r.location)) {
							ar_min = len; ar_loc = r.location;
						} break;
				}
			}
		}

		if (ch_loc != null)      { nearest = ch_loc; min = ch_min; nearestIsAir = false; } 
		else if (ca_loc != null) { nearest = ca_loc; min = ca_min; nearestIsAir = false; }
		else if (ar_loc != null) { nearest = ar_loc; min = ar_min; nearestIsAir = true; }

		/*if (nearest != null) countdown = 0;
		else {
			if (countdown < 1) { 
				++countdown;
				nearest = p_nearest;
				nearestIsAir = p_nearestIsAir;
			}
		}*/

		// TODO : Uwaga na channelera
	}





}
