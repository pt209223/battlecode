package pt209223.ai;

// Import java utils
import java.util.*;

// Import battlecode
import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.communication.Radio.*;

public abstract class AbstractRobot {
	public enum Mission {
		NONE, RANDOM, ESCAPE, ATTACK, GOTO, GOTO_FLUX, GOTO_ENEMY, GOTO_ARCHON, GOTO_BLOCK, STOP;
	};

	protected final RobotController rc;  // Kontroler     //
	protected final Random rand;         // Losowosc      //
	protected String[] infos;            // Informacje    //
	protected Radio radio;               // Komunikacja   //
	protected Mission mission;           // Misja         //
	
	protected Map<MapLocation, TInfo> map; // Mapa        //

	protected List<RInfo> enemies;     // pozycje wroga   //
	protected List<RInfo> archons;     // moje archony    //
	protected List<RInfo> workers;     // moje workery    //
	protected List<RInfo> cannons;     // moje cannony    //
	protected List<RInfo> scouts;      // moje scouty     //
	protected List<RInfo> soldiers;    // moje soldiery   //
	protected List<RInfo> channelers;  // moje channelery //

	protected int lastScan;
	protected int lastExpensiveScan;

	// Ogolna konstrukcja robota.
	public AbstractRobot(RobotController _rc) 
	{
		rc = _rc;
		rand = new Random();
		rand.setSeed(rc.getRobot().getID());
		infos = new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];
		radio = new Radio(_rc);
		mission = Mission.NONE;
		map = new HashMap<MapLocation, TInfo>();
		enemies = new ArrayList<RInfo>();
		archons = new ArrayList<RInfo>();
		workers = new ArrayList<RInfo>();
		cannons = new ArrayList<RInfo>();
		scouts = new ArrayList<RInfo>();
		soldiers = new ArrayList<RInfo>();
		channelers = new ArrayList<RInfo>();
		lastScan = 0;
		lastExpensiveScan = 0;
	}

	// Kazdy robot ma miec zdefiniowane co ma robic.
	abstract public void run() throws GameActionException;

	public void do_mission() throws GameActionException
	{
		while (true) {
			switch (mission) {
				case NONE:
					do_none(); break;
				case RANDOM:
					do_random(); break;
				case ESCAPE:
					do_escape(); break;
				case ATTACK:
					do_attack(); break;
				case STOP:
					do_stop(); break;
				case GOTO:
					do_goto(); break;
				case GOTO_FLUX:
					do_goto_flux(); break;
				case GOTO_ENEMY: 
					do_goto_enemy(); break;
				case GOTO_ARCHON:
					do_goto_archon(); break;
				case GOTO_BLOCK:
					do_goto_block(); break;
			}
		}
	}

	public void do_none() throws GameActionException 
	{ info("DO_NONE()"); }
	
	public void do_random() throws GameActionException 
	{ info("DO_RANDOM()"); }
	
	public void do_escape() throws GameActionException 
	{ info("DO_ESCAPE()"); }
	
	public void do_attack() throws GameActionException
	{ info("DO_ATTACK()"); }

	public void do_stop() throws GameActionException 
	{ info("DO_STOP()"); }
	
	public void do_goto() throws GameActionException 
	{ info("DO_GOTO()"); }
	
	public void do_goto_flux() throws GameActionException 
	{ info("DO_GOTO_FLUX()"); }
	
	public void do_goto_enemy() throws GameActionException 
	{ info("DO_GOTO_ENEMY()"); } 
	
	public void do_goto_archon() throws GameActionException 
	{ info("DO_GOTO_ARCHON()"); }
	
	public void do_goto_block() throws GameActionException 
	{ info("DO_GOTO_BLOCK()"); }

	// Dodawanie informacji co robot robi.
	public void info(String descr)
	{
		for (int i = 1; i < infos.length; ++i)
			infos[i-1] = infos[i];

		infos[infos.length - 1] = Clock.getRoundNum() + ": " + descr;

		for (int i = 0; i < infos.length; ++i)
			rc.setIndicatorString(i, infos[i]);
	}

	public void waitForMove()
	{
		while (rc.isMovementActive() ||
				rc.getRoundsUntilMovementIdle() > 0) 
			rc.yield();
	}

	public void waitForAttack()
	{
		while (rc.isAttackActive() ||
				rc.getRoundsUntilAttackIdle() > 0) 
			rc.yield();
	}

	public boolean canMove(Direction d)
	{
		return 
			rc.canMove(d) && !rc.isMovementActive() && rc.getRoundsUntilMovementIdle() == 0;
	}

	public boolean canAttack(MapLocation l)
	{
		return
			rc.canAttackSquare(l) && !rc.isAttackActive() && rc.getRoundsUntilAttackIdle() == 0;
	}

	public void expensiveScan()
	{
		enemies.clear();
		archons.clear();
		workers.clear();
		cannons.clear();
		scouts.clear();
		soldiers.clear();
		channelers.clear();

		int d = rc.getRobotType().sensorRadius();
		int bc = Clock.getBytecodeNum();
		int rd = Clock.getRoundNum();
		
		for (int x = rc.getLocation().getX() - d; x <= rc.getLocation().getX() + d; ++x) {
			for (int y = rc.getLocation().getY() - d; y <= rc.getLocation().getY() + d; ++y) {				
				// Sprawdzamy kazde pole z pola widocznosci...
				MapLocation loc = new MapLocation(x, y); 
				TInfo t = null;

				if (!rc.canSenseSquare(loc)) continue;
				try { 
					// Moga byc rzucane wyjatki, trudno. 
					// Raczej nie da sie tego jakos przerwac...
					if (map.containsKey(loc)) {
						t = map.get(loc);
						t.rescan(rc);
						map.remove(loc);
					} else {
						t = new TInfo(loc);
						t.scan(rc);
					}
					map.put(loc, t);
				}
				catch (Exception e) { }
				
				// Teraz patrzymy jakie roboty sa w zasiegu...
				if (null == t) continue; // dla pewnosci...
				
				if (null != t.groundRobot && 
						t.groundRobot.robot.getID() != rc.getRobot().getID()) {
					if (rc.getTeam() != t.groundRobot.inf.team) 
						enemies.add(t.groundRobot);
					else
						switch (t.groundRobot.inf.type) {
							case WORKER:
								workers.add(t.groundRobot); break;
							case CANNON:
								cannons.add(t.groundRobot); break;
							case SOLDIER:
								soldiers.add(t.groundRobot); break;
							case CHANNELER:
								channelers.add(t.groundRobot); break;
						}
				}

				if (null != t.airRobot &&
						t.airRobot.robot.getID() != rc.getRobot().getID()) {
					if (rc.getTeam() != t.airRobot.inf.team) 
						enemies.add(t.airRobot);
					else
						switch (t.airRobot.inf.type) {
							case ARCHON:
								archons.add(t.airRobot); break;
							case SCOUT:
								scouts.add(t.airRobot); break;
						}
				}

			}
		}	

		int bc_e = Clock.getBytecodeNum();
		int rd_e = Clock.getRoundNum();
	
		info("expensiveScan: bytecode=" + (bc_e - bc) + " rounds=" + (rd_e - rd));

		lastScan = lastExpensiveScan = Clock.getRoundNum();
	}


	public void fastScan()
	{
		enemies.clear();
		archons.clear();
		workers.clear();
		cannons.clear();
		scouts.clear();
		soldiers.clear();
		channelers.clear();

		int bc = Clock.getBytecodeNum();
		int rd = Clock.getRoundNum();

		Robot[] gr = rc.senseNearbyGroundRobots();

		if (null != gr)
			for (Robot r : gr) {
				try {
					RobotInfo inf = rc.senseRobotInfo(r);
					if (inf.team != rc.getTeam()) { enemies.add(new RInfo(r, inf)); continue; }
					if (inf.type == RobotType.WORKER)       workers.add(new RInfo(r, inf));
					if (inf.type == RobotType.CHANNELER) channelers.add(new RInfo(r, inf));
					if (inf.type == RobotType.CANNON)       cannons.add(new RInfo(r, inf));
					if (inf.type == RobotType.SOLDIER)     soldiers.add(new RInfo(r, inf));
				}
				catch (Exception e) { }
			}

		Robot[] ar = rc.senseNearbyAirRobots();
		
		if (null != ar)
			for (Robot r : ar) {
				try {
					RobotInfo inf = rc.senseRobotInfo(r);
					if (inf.team != rc.getTeam()) { enemies.add(new RInfo(r, inf)); continue; }
					if (inf.type == RobotType.ARCHON) { archons.add(new RInfo(r, inf)); continue; }
					if (inf.type == RobotType.SCOUT)     scouts.add(new RInfo(r, inf));
				}
				catch (Exception e) { }
			}

		int bc_e = Clock.getBytecodeNum();
		int rd_e = Clock.getRoundNum();
	
		//info("Czas scanu: bytecode=" + (bc_e - bc) + " rounds=" + (rd_e - rd));

		lastScan = Clock.getRoundNum();
	}

	public void transferEnergonTo(RInfo r)
	{
		if ((!rc.getLocation().isAdjacentTo(r.inf.location) &&
					rc.getLocation() != r.inf.location) ||
				rc.getTeam() != r.inf.team || 
				r.inf.energonLevel > 0.7*r.inf.maxEnergon ||
				rc.getEnergonLevel() < 0.4*rc.getMaxEnergonLevel())
			return;

		try {
			rc.transferEnergon(
					Math.min(r.inf.maxEnergon - r.inf.energonLevel, 5),
					r.inf.location, r.robot.getRobotLevel());
		}
		catch (Exception e) { }
	}

	public void transferEnergonTo(List<RInfo> lr)
	{
		for (RInfo r : lr) transferEnergonTo(r);
	}

	public void stepTo(MapLocation trg)
	{
		Direction d = rc.getLocation().directionTo(trg);

		if ((rc.getLocation().getX() == trg.getX() && 
					rc.getLocation().getY() == trg.getY()) ||
				Direction.OMNI == d || Direction.NONE == d) return;

		boolean repeat = true;
		while (repeat) {
			try {
				for (int i = 0; i < 8 && !rc.canMove(d); ++i) d = d.rotateRight();
				
				if (rc.getDirection() != d) {
					waitForMove();
					rc.setDirection(d);
				}
					
				waitForMove();
				rc.moveForward();
				rc.yield();

				repeat = false;
			}
			catch (Exception e) { }
		}
	}

	public void goTo(MapLocation trg)
	{
		while (
				rc.getLocation().getX() != trg.getX() ||
				rc.getLocation().getY() != trg.getY())
			stepTo(trg);
	}

	public TInfo getFromMap(MapLocation loc)
	{
		TInfo t = map.get(loc);
		if (null != t) return t;
		if (!rc.canSenseSquare(loc)) return null;

		try {
			t = new TInfo(loc);
			t.scan(rc);
			map.put(loc, t);
			return t;
		}
		catch (Exception e) { }

		return null;
	}

	public TInfo updateOnMap(MapLocation loc)
	{
		TInfo t = map.get(loc);

		try {
			if (null == t) {
				t = new TInfo(loc);
				t.scan(rc);
			} else { 
				t.rescan(rc);
				map.remove(loc);
			}

			map.put(loc, t);
		}
		catch (Exception e) { return null; }

		return t;
	}


}
