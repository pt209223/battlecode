package pt209223.navigation;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class AItem implements Comparable<AItem> {
	public MapLocation location;
	public int         heurestic;
	public int         cost;
	public int         height;

	public AItem(MapLocation _l, int _h, int _c)
	{
		location = _l;
		heurestic = _h;
		cost = _c;
	}

	public int costTo(AItem trg, int delayOrtho)
	{
		int c = (int)Math.round(delayOrtho * Math.sqrt(location.distanceSquaredTo(trg.location)));

		if (height > trg.height) 
			return c + CLIMBING_PENALTY_RATE * (height-trg.height) * (height-trg.height);
		else if (height < trg.height)
			return c + FALLING_PENALTY_RATE * (trg.height-height) * (trg.height-height);
		else
			return c;
	}

	public int estimateTo(AItem trg, int delayOrtho)
	{
		return (int)Math.round(delayOrtho * Math.sqrt(location.distanceSquaredTo(trg.location)));
	}

	public int compareTo(AItem o) //Object other)
	{
		//if (!(other instanceof AItem)) return -1;
		//AItem o = (AItem) other;
		return (o.heurestic + o.cost) - (heurestic + cost);
	}
	
	@Override public boolean equals(Object other)
	{
		if (!(other instanceof AItem)) return false;
	
		AItem o = (AItem) other;
		if (null == o) return false;
		else if (null == location) return (null == o.location);
		else if (null == o.location) return false;

		return location.equals(o.location);
	}

	@Override public int hashCode()
	{
		return (null == location) ? 0 : location.hashCode();
	}

}
