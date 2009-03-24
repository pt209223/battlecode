package pt209223.navigation;

import java.util.*;

public class APath {
	public PriorityQueue<AItem> queue;
	public HashSet<AItem> visited;
	public LinkedList<AItem> path;

	public APath()
	{
		queue = new PriorityQueue<AItem>();
		visited = new HashSet<AItem>();
		path = new LinkedList<AItem>();
	}

	public boolean find()
	{
		return false;
	}
}
