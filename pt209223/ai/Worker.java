package pt209223.ai;

import java.util.*;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.communication.Radio.*;

public class Worker extends AbstractRobot {
	// - Na stairs trzymamy sciezke ukladania blokow.
	// - Od 'First' do 'Last' gdzie 'Last' to pozycja
	// - fluxa. Przy starcie Worker czeka az, Archon 
	// - powie jakie sa schody(moze byc tak, ze juz 
	// - jakies sa. Za kazdym dolozeniem bloku na 
	// - sciezke Worker powiadamia, ze dolozyl blok.
	// - Archon, odbierze i zapamieta.
	private MapLocation nearest, next;
	private MapLocation[] blocks;
	private LinkedList<MapLocation> stairs;
	private HashSet<MapLocation> lastSeen;

	public Worker(RobotController _rc) 
	{
		super(_rc);
		blocks = null;
		nearest = null;
		next = null;
		lastSeen = new HashSet<MapLocation> ();
		stairs = new LinkedList<MapLocation> ();
	}

	public void run() throws GameActionException 
	{
		mission = Mission.NONE;
		do_mission();
	}

	public void do_none() throws GameActionException
	{
		// Czekamy co powie archon o schodkach :]

		while (true) {
			radio.receive();
			Message msg = radio.get();
			if (null == msg) continue;
			if (MSG_TYPE_STAIRS != msg.ints[MSG_IDX_TYPE]) continue;

			FluxDeposit[] fds = rc.senseNearbyFluxDeposits();
			MapLocation flux = null;
			for (FluxDeposit fd : fds) {
				FluxDepositInfo fdi = rc.senseFluxDepositInfo(fd);
				if (null == flux) flux = fdi.location;
				else if (rc.getLocation().distanceSquaredTo(flux) < 
					rc.getLocation().distanceSquaredTo(fdi.location)) {
					flux = fdi.location;
				}
			}
	
			stairs.clear();
			for (int i = MSG_STAIRS_L_START, cnt = 0; cnt < msg.ints[MSG_STAIRS_I_SIZE]; ++i, ++cnt) 
				stairs.addFirst(msg.locations[i]);
			if (!rc.getLocation().isAdjacentTo(stairs.getLast()) || 
					stairs.getLast() != flux) {
				info("Nieprawidlowe dane od Archona...");
				stairs.clear();
				stairs.addFirst(flux);
			}
	
			mission = Mission.GOTO_BLOCK;
			return;
		}
	}

	public void do_goto_block()
	{
		try {
			if (rc.getNumBlocks() >= 1) {
				mission = Mission.GOTO_FLUX;
				return;
			}

			if (rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel()) {
				mission = Mission.GOTO_ARCHON;
				return;
			}
			
			expensiveScan(); // Drogie sprawdzenie otoczenia
			checkLastSeen(); // Aktualizacja wiedzy o widzianych blokach
			
			if (!scanForBlocks()) { // Nie widzimy zadnego bloku
				info("Oj, ajaja");
				// Ale... moze pamietamy jakies pozycje...
				if (!lastSeen.isEmpty()) {
					blocks = new MapLocation[lastSeen.size()];
					int i = 0;
					for (MapLocation l : lastSeen) { blocks[i] = l; ++i; }
				} else {
					info("Nie widze zadnych sensownych blockow:(");
					mission = Mission.RANDOM;
					return;
				}
				// jesli jednak nie, to Worker sobie pochodzi, tu i tam...
				if (!findNearest()) {
					lastSeen.clear();
					info("Nadal nie widze zadnych sensownych blockow:(");
					mission = Mission.RANDOM;
					return;
				}
			}
			
			info("Ide... (" + rc.getLocation().distanceSquaredTo(next) + ")");
			goTo(next);
			info("Biore bloka...");

			// Jesli nie da sie pobrac bloku, to Worker sobie troche pochodzi, popatrzy...
			if (!rc.canLoadBlockFromLocation(nearest)) { mission = Mission.RANDOM; return; }

			waitForMove();
		
			while (true) {
				try { rc.loadBlockFromLocation(nearest); info("Mam!!"); break; }
				catch (Exception e) { }
			}

			// Odczekajmy...
			int wt = rc.getRobotType().loadDelay();
			while (wt > 0) { rc.yield(); --wt; }

			// Do domciu...
			mission = Mission.GOTO_FLUX;	
		}
		catch (Exception e) { }
	}

	public void do_goto_flux() throws GameActionException
	{
		// Nie mam blokow? To co ja tu jeszcze robie?
		if (rc.getNumBlocks() == 0) {
			mission = Mission.GOTO_BLOCK;
			return;
		}

		assert(!stairs.isEmpty());

		// Kieruje sie do pierwszego schodka
		while (!rc.getLocation().isAdjacentTo(stairs.getFirst())) {
			if (rc.getLocation() == stairs.getFirst()) // Jak na, to rusz sie gdziekolwiek...
				stepTo(rc.getLocation().add(rc.getDirection().rotateRight()));
			else // wpp idz w kierunku schodka
				stepTo(stairs.getFirst());
			info("Ide, ide... (" + rc.getLocation().distanceSquaredTo(stairs.getFirst())+")");
		}
		info("Hah! jestem!");

		// Teraz wchodzimy schodek po schodku...
		TInfo curr = updateOnMap(rc.getLocation());
		TInfo prev = updateOnMap(rc.getLocation().subtract(rc.getDirection()));
		boolean added = true;

		assert(null != curr && null != prev);

		for (MapLocation l : stairs) {
			TInfo t = updateOnMap(l);
			assert(null != t);
			
			if (Math.abs(curr.height+curr.blocks-t.height-t.blocks) >= WORKER_MAX_HEIGHT_DELTA) {
				// Za wysokie progi... ;(
				goExactlyThere(prev.loc);
				unloadExactlyThere(curr.loc);
				break;
			} else {
				added = false;
				if (stairs.getLast() == l) {
					// Nastepny jest flux!
					unloadExactlyThere(l);
					break;
				} else {
					// Idziemy dalej po schodach :]
					goExactlyThere(l);
				}
			}
			prev = curr;
			curr = t;
		}

		if (added) {
			// Na curr nowy blok.
			stairs.addFirst(curr.loc);
			radio.sayStairs(stairs);
			radio.broadcast();
		}

		mission = Mission.GOTO_BLOCK;
	}

	public void do_goto_archon()
	{
		// Archon jest tam gdzie flux, jesli nawet zdarzy sie, ze
		// tak nie bedzie to mozna to uznac sytuacje tak awaryjna
		// - np atak wroga - ze zupelnie nie istotne bedzie co zrobi
		// nasz Worker.

		while (!rc.getLocation().isAdjacentTo(stairs.getLast())) 
			stepTo(stairs.getLast());
		
		mission = Mission.RANDOM;
	}

	public void do_random()
	{
		// Pochodzimy sobie...
		info("Pochodze sobie...");

		if (rc.getNumBlocks() >= 1) {
			mission = Mission.GOTO_FLUX;
			return;
		}

		if (rc.getEnergonLevel() < 0.3*rc.getMaxEnergonLevel()) {
			mission = Mission.GOTO_ARCHON;
			return;
		}

		if (scanForBlocks()) {
			mission = Mission.GOTO_BLOCK;
			return;
		}

		Direction[] ds = Direction.values();
		int r = rand.nextInt(ds.length);
		Direction d = ds[r];

		if (Direction.OMNI == d || Direction.NONE == d)
			d = rc.getDirection();

		stepTo(rc.getLocation().add(d));
	}

	public boolean scanForBlocks()
	{
		blocks = rc.senseNearbyBlocks();
		return findNearest();
	}

	public boolean findNearest()
	{
		int min = 1000;
		nearest = null;
		next = null;

		if (null != blocks && 0 < blocks.length) {
			for (MapLocation b : blocks) {
				if (!stairs.contains(b)) {
					MapLocation ok = null;
					TInfo tb = getFromMap(b);
					if (null == tb) continue;

					for (Direction d : Direction.values()) {
						if (Direction.OMNI == d || Direction.NONE == d) continue;
						TInfo t = getFromMap(b.add(d));
						if (null != t && checkHeight(t, tb)) { ok = b.add(d); break; }
					}

					int len = rc.getLocation().distanceSquaredTo(b);
					if (null != ok && min > len) { min = len; nearest = b; next = ok; }

					lastSeen.add(b);
				}
			}
		}

		if (null != blocks && null == nearest) { blocks = null; }

		return (null != nearest);
	}
			
	public void checkLastSeen()
	{
		for (MapLocation l : lastSeen) {
			TInfo t = getFromMap(l);
			if (null != t && 0 == t.blocks)
				lastSeen.remove(l);
		}
	}

	public boolean checkHeight(TInfo src, TInfo trg)
	{
		return // Cel nie moze byc wyzej niz WORKER_MAX_HEIGHT_DELTA
			src.height+src.blocks + WORKER_MAX_HEIGHT_DELTA > trg.height+trg.blocks;
	}

	public void goExactlyThere(MapLocation l)
	{
		Direction d = rc.getLocation().directionTo(l);
		while (true) {
			try { 
				if (rc.getDirection() != d) {
					waitForMove();
					rc.setDirection(d);
				}
				waitForMove();
				rc.moveForward();
				rc.yield();
				return;
			}
			catch (Exception e) { }
		}
	}

	public void unloadExactlyThere(MapLocation l)
	{
		while (true) {
			try {
				waitForMove();
				rc.unloadBlockToLocation(l);
				rc.yield();
				return;
			}
			catch (Exception e) { }
		}
	}


}
