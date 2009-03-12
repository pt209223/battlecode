package pt209223.navigation;

import battlecode.common.*;
import java.util.*;

public class RInfo {
	public Robot robot;
	public RobotInfo inf;

	public RInfo(Robot _robot, RobotInfo _inf)
	{
		robot = _robot;
		inf = _inf;
	}

	public RInfo(Robot _robot, RobotController _rc) throws GameActionException
	{
		robot = _robot;
		inf = _rc.senseRobotInfo(_robot);
	}

};

