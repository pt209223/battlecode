package pt209223;

import pt209223.ai.*;
import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {

	private final RobotController rc;
	private AbstractRobot player;

	public RobotPlayer(RobotController _rc) 
	{
		rc = _rc;

		switch (rc.getRobotType()) {
			case ARCHON:
				player = new Archon(rc);
				break;
			case CHANNELER:
				player = new Channeler(rc);
				break;
			case SOLDIER:
				player = new Soldier(rc);
				break;
			case SCOUT:
				player = new Scout(rc);
				break;
			case CANNON:
				player = new Cannon(rc);
				break;
			case WORKER:
				player = new Worker(rc);
				break;
			default:
				player = new Dummy(rc);
		}
	}

	public void run() 
	{
		while (true) {
			try { player.run(); }
			catch (Exception e) {
				System.out.println("Exception: " + e.toString());
				e.printStackTrace();
			}
		}
	}

}

