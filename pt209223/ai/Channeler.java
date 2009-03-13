package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import pt209223.communication.*;

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

	public void run()
	{
		info("Czekam na 'HELLO' od Archona...");

		while (true) {
			radio.receive();
			Message msg = radio.get();
			if (null == msg) continue; // Moze byc przestarzala
			if (Radio.HELLO != msg.ints[Radio.TYPE]) continue;
			
			if (!rc.getLocation().isAdjacentTo(msg.locations[Radio.HELLO_SENDER])) 
				continue;
			if (!RobotType.valueOf(msg.strings[Radio.HELLO_SENDER_TYPE]).equals(RobotType.ARCHON))
				continue;
			
			archon = msg.locations[Radio.HELLO_SENDER];
			break;
		}

		info("Ok, pokrece sie wokol Archona...");

		while (true) {
			radio.receive();

			int min = 100;

			while (true) {
				Message msg = radio.get();
				if (null == msg) break;
				if (Radio.ENEMIES != msg.ints[Radio.TYPE]) continue;
				
				int it_i = Radio.ENEMIES_I_START;
				int it_l = Radio.ENEMIES_L_START;
				int it_s = Radio.ENEMIES_S_START;
				int i = 0, n = msg.ints[Radio.ENEMIES_SIZE];

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

			if (Direction.NONE.equals(dir)) { 
				info("Nie wiem gdzie jest Archon :(");
			} else if (Direction.OMNI.equals(dir)) { 
				info("Jestem pod Archonem, odejde na bok...");
				try { rc.moveForward(); }
				catch (Exception e) { 
					info("Nie moge sie ruszyc... :("); 
				}
			} else if (rc.getLocation().isAdjacentTo(archon)) {
				try {
					dir = dir.rotateRight();
					if (!rc.getDirection().equals(dir)) rc.setDirection(dir);
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

