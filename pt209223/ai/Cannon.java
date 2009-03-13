package pt209223.ai;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import pt209223.navigation.*;
import pt209223.communication.*;

public class Cannon extends AbstractRobot{
	private MapLocation archon;

	public Cannon(RobotController _rc)
	{
		super(_rc);
		archon = null;
	}

	public void run()
	{
		radio.receive();

		while (true) { //radio.isIncoming()) {
			try {
				Message msg = radio.get();
				if (null == msg) { 
					if (null != archon) stepTo(archon);
					continue; // Moze byc przestarzale
				}

				switch (msg.ints[Radio.TYPE]) {
					case Radio.HELLO:
						if (RobotType.valueOf(msg.strings[Radio.HELLO_SENDER_TYPE]).equals(RobotType.ARCHON))
							archon = msg.locations[Radio.HELLO_SENDER];
						break;
					case Radio.ENEMIES:
						// TODO: troche syf z indeksami...
						info("Cel do zniszczenia!!");
						int i_s = 0, i_l = 1, i_i = 1, idx = -1, min = 1000;
						int ch_i = -1, ca_i = -1, ar_i = -1;
						int ch_min = 1000, ca_min = 1000, ar_min = 1000;

						for (int i = 0; i < msg.ints[Radio.ENEMIES_SIZE]; ++i) {
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
			catch (Exception e) {
				System.out.println("run(): Exception: " + e);
			}
		}


	}

}
