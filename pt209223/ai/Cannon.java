package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;
import static pt209223.communication.Radio.*;


public class Cannon extends AbstractRobot{
	private MapLocation archon;

	public Cannon(RobotController _rc)
	{
		super(_rc);
		archon = null;
	}

	public void run() throws GameActionException
	{
		radio.receive();

		while (radio.isIncoming()) {
			Message msg = radio.get();
			switch (msg.ints[MSG_IDX_TYPE]) {
				case MSG_TYPE_HELLO:
					if (RobotType.valueOf(msg.strings[MSG_HELLO_S_SENDER]) == RobotType.ARCHON)
						archon = msg.locations[MSG_HELLO_L_SENDER];
					break;
				case MSG_TYPE_ENEMIES:
					info("Cel do zniszczenia!!");
					int i_s = 0, i_l = 1, i_i = 1, idx = -1, min = 1000;
					int ch_i = -1, ca_i = -1, ar_i = -1;
					int ch_min = 1000, ca_min = 1000, ar_min = 1000;

					for (int i = 0; i < msg.ints[MSG_ENEMIES_I_SIZE]; ++i) {
						int len = rc.getLocation().distanceSquaredTo(msg.locations[i_l]);
						if (idx == -1 || len < min) { min = len; idx = i; }
						switch (RobotType.valueOf(msg.strings[i_s])) {
							case CHANNELER:
								if ((ch_i == -1 || len < ch_min) && rc.canAttackSquare(msg.locations[i_l])) {
									ch_min = len; ch_i = i;
								} break;
							case CANNON:
								if ((ca_i == -1 || len < ca_min) && rc.canAttackSquare(msg.locations[i_l])) {
									ca_min = len; ch_i = i;
								} break;
							case ARCHON:
								if ((ar_i == -1 || len < ar_min) && rc.canAttackSquare(msg.locations[i_l])) {
									ar_min = len; ar_i = i;
								} break;
						}
						++i_s; ++i_l; ++i_i;
					}

					if (ch_i != -1) idx = ch_i;
					else if (ca_i != -1) idx = ca_i;
					else if (ar_i != -1) idx = ar_i;
					else if (idx == -1) continue;

					if (!rc.canAttackSquare(msg.locations[idx+1])) {
						waitForMove();
						rc.setDirection(rc.getLocation().directionTo(msg.locations[idx+1]));
					}

					waitForAttack(); 
					if (RobotType.valueOf(msg.strings[idx]).isAirborne()) 
						rc.attackAir(msg.locations[idx+1]);
					else
						rc.attackGround(msg.locations[idx+1]);
					rc.yield();

					break;
				default:
					/* inne ignoruje */
			}
		}

		if (null != archon) stepTo(archon);

	}

}
