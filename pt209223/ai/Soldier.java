package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.navigation.Constants.*;

public class Soldier extends AbstractRobot {
	protected MapLocation target;
	protected MapLocation archon;
	protected MapLocation nearest;
	protected boolean nearestIsAir;

	public Soldier(RobotController _rc)
	{
		super(_rc);
		target = null;
		archon = null;
		nearest = null;
		nearestIsAir = false;
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
		mission = Mission.RANDOM;
		do_mission();
	}

	public void do_none() throws GameActionException
	{
		while (rc.isMovementActive() || rc.isAttackActive()) {
			///...
		}
	}

	public void do_random() throws GameActionException
	{
		info("Pochodze sobie, popatrze...");

		listen(); // - slucha rozkazow.

		if (null != target) return;

		if (mission != Mission.RANDOM) return;

		if (rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel()) {
			if (null != archon) { 
				if (rc.getLocation().equals(archon)) archon = null;
				else { stepTo2(archon); return; }
			}

			fastScan(); // - scanuje tylko czy sa jakies roboty
		} else 
			expensiveScan(); // - dokladnie bada teren

		checkArchons();
		checkEnemies();
		checkSoldiers();

		// - Jak jest jest wrog to wychodzimy z RANDOM
		if (!enemies.isEmpty()) { 
			mission = Mission.ATTACK;
			target = nearest;
			return; 
		}

		int dnum = rand.nextInt(4);
		Direction dir = rc.getDirection();

		switch (dnum) {
			case 0: dir = dir.rotateRight(); break;
			case 1: dir = dir.rotateLeft();  break;
		}

		stepTo2(rc.getLocation().add(dir));
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

	public void checkEnemies()
	{
		if (!enemies.isEmpty()) {
			int min = INFINITY;
			double energon = INFINITY;
			nearest = null;
			for (RInfo r : enemies) {
				int len = rc.getLocation().distanceSquaredTo(r.inf.location);
				if (null == nearest || len < min || r.inf.energonLevel < energon) { 
					min = len; nearest = r.inf.location; 
					energon = r.inf.energonLevel;
					nearestIsAir = r.inf.type.isAirborne();
				}
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

			if (null != who) transferEnergonTo(who);
		}
	}

	public void transferEnergonTo(RInfo r)
	{
		try {
			rc.transferEnergon(
					Math.min(r.inf.maxEnergon - r.inf.energonLevel, 5),
					r.inf.location, r.robot.getRobotLevel());
		}
		catch (Exception e) { }
	}


	public void do_attack() throws GameActionException
	{
		fastScan(); // - Szybki skan co i jak
		checkEnemies(); // - gdzie wrog

		if (null != nearest && rc.getLocation().isAdjacentTo(nearest)) {
			if (!rc.canAttackSquare(nearest)) {
				waitForMove();
				rc.setDirection(rc.getLocation().directionTo(nearest));
			}
			waitForAttack();
			if (nearestIsAir) rc.attackAir(nearest);
			else rc.attackGround(nearest);
			rc.yield();
			return;
		}

		checkSoldiers(); // czy by tu nie podladowac jakies soldziera z frontu

		// Zmieniamy target (uwaga, teraz moze to byc null!!)
		if (rc.getLocation().distanceSquaredTo(target) <= 4 ||
				(null != nearest && nearest.distanceSquaredTo(target) > 25)) target = nearest;

		if (null != nearest) stepTo2(nearest);
		else {
			if (null == target) { mission = Mission.RANDOM; return; }
			stepTo2(target); // Do celu...
		}
	}

	public void listen()
	{
		if (null != target) return; // JEsli mam gdzie strzelac to ignoruje listen()

		int rds = Clock.getRoundNum(), bcs = Clock.getBytecodeNum();

		radio.receive();

		while (radio.isIncoming()) {
			Message msg = radio.get();
			if (null == msg) continue; // Moze byc przestarzala

			switch (msg.ints[Radio.TYPE]) {
				case Radio.HELLO:
					if (RobotType.valueOf(msg.strings[Radio.HELLO_SENDER_TYPE]) == RobotType.ARCHON)
						archon = msg.locations[Radio.HELLO_SENDER];
					break;
				case Radio.ATTACK:
					target = msg.locations[Radio.ATTACK_TARGET];
					break;
				default:
					/* inne ignoruje */
			}
		}

		rds = Clock.getRoundNum() - rds;
		bcs = 6000*rds + Clock.getBytecodeNum() - bcs;

		info("Przetworzylem wiadomosci (rn:"+rds+" bc:"+bcs+")");
	}

}
