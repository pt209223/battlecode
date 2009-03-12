package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static pt209223.communication.Radio.*;

public class Channeler extends AbstractRobot {
	private MapLocation archon;
	private int drainRounds;
	private int drainDelay;

	public Channeler(RobotController _rc)
	{
		super(_rc);
		archon = null;
		drainRounds = 0;
		drainDelay = 0;
	}

	public void run() throws GameActionException
	{
		info("Czekam na 'HELLO' od Archona...");

		while (true) {
			radio.receive();
			Message msg = radio.get();
			if (null == msg) continue;
			if (MSG_TYPE_HELLO != msg.ints[MSG_IDX_TYPE]) continue;
			
			if (!rc.getLocation().isAdjacentTo(msg.locations[MSG_HELLO_L_SENDER])) 
				continue;
			
			archon = msg.locations[MSG_HELLO_L_SENDER];
			break;
		}

		info("Ok, pokrece sie wokol Archona...");

		while (true) {
			radio.receive();

			int min = 100;

			while (true) {
				Message msg = radio.get();
				if (null == msg) break;
				if (MSG_TYPE_ENEMIES != msg.ints[MSG_IDX_TYPE]) continue;
				
				int it_i = MSG_ENEMIES_I_ENEMIES_START;
				int it_l = MSG_ENEMIES_L_ENEMIES_START;
				int it_s = MSG_ENEMIES_S_ENEMIES_START;
				int i = 0, n = msg.ints[MSG_ENEMIES_I_SIZE];

				while (i < n) {
					int len = archon.distanceSquaredTo(msg.locations[it_l]);
					if (len < min) min = len;
					++i; ++it_i; ++it_l; ++it_s;
				}
			}

			if (9 >= min) {
				info("Wrog blisko, wysysam okoliczna energie...");
				drainRounds = 10;
				drainDelay = 0;
			} else if (25 >= min) {
				info("Wrog sie zbliza, bede wysysal okoliczna energie...");
				drainRounds = 10;
				drainDelay = 1;
			}

			if (drainDelay > 0) { --drainDelay; }
			else if (drainRounds > 0) { 
				try { 
					rc.drain(); --drainRounds; 
					info("Wysysam energie...");
				} 
				catch (Exception e) { }
			}

			Direction dir = rc.getLocation().directionTo(archon);

			if (Direction.NONE == dir) { 
				info("Nie wiem gdzie jest Archon :(");
			} else if (Direction.OMNI == dir) { 
				info("Jestem pod Archonem, odejde na bok...");
				try { rc.moveForward(); }
				catch (Exception e) { 
					info("Nie moge sie ruszyc... :("); 
				}
			} else if (rc.getLocation().isAdjacentTo(archon)) {
				try {
					dir = dir.rotateRight();
					if (rc.getDirection() != dir) rc.setDirection(dir);
					waitForMove(); 
					rc.moveForward();
					rc.yield();
				} 
				catch (Exception e) { }
			}	else {
				info("U!...");
			}

		}
	}

}

