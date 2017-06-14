package at.ac.tuwien.kr.alpha.solver;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class ReducibleWatchedNoGood {

	private final WatchedNoGood watchedNoGood;
	private float activity;
	private int literalBlockDistance;

	public ReducibleWatchedNoGood(WatchedNoGood watchedNoGood) {
		this.watchedNoGood = watchedNoGood;
	}
}
