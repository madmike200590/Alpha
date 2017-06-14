package at.ac.tuwien.kr.alpha.solver;

import java.util.ArrayList;

/**
 * Copyright (c) 2017, the Alpha Team.
 */
public class GlueForgetting {
	private int conflictCounterSinceLastForget;
	private int forgetRunCounter;
	private static final int INITIAL_CONFLICTS_UNTIL_FORGET = 20000;
	private static final int INCREASE_FACTOR_UNTIL_FORGET = 500;

	private final ArrayList<WatchedNoGood> reducibleNoGoods = new ArrayList<>();

	public void incrementConflictCounter() {
		conflictCounterSinceLastForget++;
	}

	public boolean timeForForgetting() {
		return conflictCounterSinceLastForget > INITIAL_CONFLICTS_UNTIL_FORGET + INCREASE_FACTOR_UNTIL_FORGET * forgetRunCounter;
	}

	public void recordWatchedNoGood(WatchedNoGood watchedNoGood) {
		reducibleNoGoods.add(watchedNoGood);
	}

	public void reduceLearntNoGoods() {
		forgetRunCounter++;

		// TODO: iterate over all reducibleNoGoods, keep all binary ones, keep ones with lbd <= 2, keep locked ones (that currently propagate),
		// TODO: sort the remaining ones by lbd, then remove the half with higher lbd.
	}
}
