package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Dummy extends AbstractRobot {

	public Dummy(RobotController _rc) 
	{
		super(_rc);
	}

	public void run() throws GameActionException
	{
		while (true) {
			info("Jestem dummy");

			while (rc.isMovementActive()) rc.yield();

			if (rc.canMove(rc.getDirection())) rc.moveForward();
			else rc.setDirection(rc.getDirection().rotateRight());

			rc.yield();
		}
	}

}
