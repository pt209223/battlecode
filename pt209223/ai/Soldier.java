package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.communication.Radio.*;

public class Soldier extends AbstractRobot {
	protected MapLocation target;
	protected MapLocation archon;

	public Soldier(RobotController _rc)
	{
		super(_rc);
		target = null;
		archon = null;
	}

	public void run() throws GameActionException
	{
		mission = Mission.ATTACK;
		do_mission();
	}

	public void do_attack() throws GameActionException
	{
		listen();
	
		fastScan();

		if (enemies.isEmpty()) {
			if (!soldiers.isEmpty())
				for (RInfo r : soldiers) {
					if (rc.getLocation().isAdjacentTo(r.inf.location) && 
							rc.getLocation().distanceSquaredTo(target) >
							r.inf.location.distanceSquaredTo(target)) {
						transferEnergonTo(r); break;
					}
				}

			if (null != target) {
				if (rc.getLocation() != target) stepTo(target);
				else {
					rc.moveForward();
					rc.yield();
				}
			}
		} else {
			int min = 1000;
			RInfo nearest = null;

			for (RInfo r : enemies) {
				int len = rc.getLocation().distanceSquaredTo(r.inf.location);
				if (null == nearest || (len > 0 && len < min)) {
					min = len; nearest = r;
				}
			}

			if (nearest.inf.location.isAdjacentTo(rc.getLocation())) {
				if (!rc.canAttackSquare(nearest.inf.location)) {
					waitForMove();
					rc.setDirection(rc.getLocation().directionTo(nearest.inf.location));
				}
				waitForAttack();
				if (nearest.inf.type.isAirborne()) rc.attackAir(nearest.inf.location);
				else rc.attackGround(nearest.inf.location);
				rc.yield();
			} else {
				if (!soldiers.isEmpty())
					for (RInfo r : soldiers) {
						if (rc.getLocation().isAdjacentTo(r.inf.location) && 
								rc.getLocation().distanceSquaredTo(nearest.inf.location) >
								r.inf.location.distanceSquaredTo(nearest.inf.location)) {
							transferEnergonTo(r); break;
						}
					}
				
				stepTo(nearest.inf.location);
			}
		}
	}

	public void listen()
	{
		radio.receive();

		while (radio.isIncoming()) {
			Message msg = radio.get();
			switch (msg.ints[MSG_IDX_TYPE]) {
				//case MSG_TYPE_HELLO:
				//	if (RobotType.valueOf(msg.strings[MSG_HELLO_S_SENDER]) == RobotType.ARCHON)
				//		archon = msg.locations[MSG_HELLO_L_SENDER];
				//	break;
				case MSG_TYPE_ATTACK:
					info("Cel do zniszczenia!!");
					target = msg.locations[MSG_ATTACK_L_TARGET];
					mission = Mission.ATTACK;
					break;
				default:
					/* inne ignoruje */
			}
		}
	}

}
