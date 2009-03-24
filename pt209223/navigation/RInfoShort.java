package pt209223.navigation;

import battlecode.common.*;
import java.util.*;

public class RInfoShort {
	public MapLocation location;
	public double energon;
	public RobotType type;

	public RInfoShort(MapLocation l, double e, RobotType t)
	{
		location = l;
		energon = e;
		type = t;
	}

	public RInfoShort(MapLocation l, int e, String t)
	{
		location = l;
		energon = (double)e;
		type = RobotType.valueOf(t);
	}

};

