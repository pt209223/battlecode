package pt209223.navigation;

import java.util.*;
import battlecode.common.*;

public class TInfo {
	public final MapLocation loc;
	public boolean land;
	public int height;
	public int blocks;
	public RInfo groundRobot;
	public RInfo airRobot;

	public TInfo(MapLocation _loc)
	{
		loc = _loc;
	}

	public void scan(RobotController rc) throws GameActionException
	{
		blocks = rc.senseNumBlocksAtLocation(loc);
		TerrainTile tt = rc.senseTerrainTile(loc);
		height = tt.getHeight();
		land = (tt.getType() == TerrainTile.TerrainType.LAND);
		Robot ar = rc.senseAirRobotAtLocation(loc);
		Robot gr = rc.senseGroundRobotAtLocation(loc);
		groundRobot = (null == gr) ? null : (new RInfo(rc.senseGroundRobotAtLocation(loc), rc));
		airRobot = (null == ar) ? null : (new RInfo(rc.senseAirRobotAtLocation(loc), rc));
	}

	public void rescan(RobotController rc) throws GameActionException
	{
		blocks = rc.senseNumBlocksAtLocation(loc);
		Robot ar = rc.senseAirRobotAtLocation(loc);
		Robot gr = rc.senseGroundRobotAtLocation(loc);
		groundRobot = (null == gr) ? null : (new RInfo(rc.senseGroundRobotAtLocation(loc), rc));
		airRobot = (null == ar) ? null : (new RInfo(rc.senseAirRobotAtLocation(loc), rc));
	}

	//public String toString()
};

