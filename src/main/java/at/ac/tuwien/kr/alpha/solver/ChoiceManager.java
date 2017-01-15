package at.ac.tuwien.kr.alpha.solver;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.MBT;

/**
 * This class provides functionality for choice point management, detection of active choice points, etc.
 * Copyright (c) 2017, the Alpha Team.
 */
public class ChoiceManager {
	private final Assignment assignment;

	// Active choice points and all atoms that influence a choice point (enabler, disabler, choice atom itself).
	private final Set<ChoicePoint> activeChoicePoints = new HashSet<>();
	private final Map<Integer, ChoicePoint> influencers = new HashMap<>();

	// Backtracking information.
	private final Map<Integer, ArrayList<Integer>> modifiedInDecisionLevel = new HashMap<>();
	private int highestDecisionLevel;

	// The total number of modifications this ChoiceManager received (avoids re-computation in ChoicePoints).
	private long modCount;

	public ChoiceManager(Assignment assignment) {
		this.assignment = assignment;
		modifiedInDecisionLevel.put(0, new ArrayList<>());
		highestDecisionLevel = 0;
		modCount = 0;
	}

	private class ChoicePoint {
		final Integer atom;
		final int enabler;
		final int disabler;
		boolean isActive;
		long lastModCount;

		private ChoicePoint(Integer atom, Integer enabler, int disabler) {
			this.atom = atom;
			this.enabler = enabler;
			this.disabler = disabler;
			this.lastModCount = modCount;
		}

		private boolean isActiveChoicePoint() {
			Assignment.Entry enablerEntry = assignment.get(enabler);
			Assignment.Entry disablerEntry = assignment.get(disabler);
			return  enablerEntry != null && enablerEntry.getTruth().toBoolean()
				&& (disablerEntry == null || !disablerEntry.getTruth().toBoolean());
		}

		private boolean isNotChosen() {
			Assignment.Entry entry = assignment.get(atom);
			return entry == null || MBT.equals(entry.getTruth());
		}

		void recomputeActive() {
			if (lastModCount == modCount) {
				return;
			}
			final boolean wasActive = isActive;
			isActive = isNotChosen() & isActiveChoicePoint();
			lastModCount = modCount;
			if (isActive) {
				activeChoicePoints.add(this);
			} else {
				if (wasActive) {
					activeChoicePoints.remove(this);
				}
			}
		}
	}

	public void updateAssignment(int atom) {
		ChoicePoint choicePoint = influencers.get(atom);
		if (choicePoint != null) {
			modCount++;
			modifiedInDecisionLevel.get(highestDecisionLevel).add(atom);
			choicePoint.recomputeActive();
		}
	}

	public void nextDecisionLevel() {
		highestDecisionLevel++;
		modifiedInDecisionLevel.put(highestDecisionLevel, new ArrayList<>());
	}

	public void backtrack() {
		// Assumption: assignment was backtracked first!
		modCount++;
		ArrayList<Integer> changedAtoms = modifiedInDecisionLevel.get(highestDecisionLevel);
		for (Integer atom : changedAtoms) {
			ChoicePoint choicePoint = influencers.get(atom);
			choicePoint.recomputeActive();
		}
		highestDecisionLevel--;
	}

	void addChoiceInformation(Pair<Map<Integer, Integer>, Map<Integer, Integer>> choiceAtoms) {
		// Assumption: we get all enabler/disabler pairs in one call.
		Map<Integer, Integer> enablers = choiceAtoms.getLeft();
		Map<Integer, Integer> disablers = choiceAtoms.getRight();
		for (Map.Entry<Integer, Integer> atomToEnabler : enablers.entrySet()) {
			// Construct and record ChoicePoint.
			Integer atom = atomToEnabler.getKey();
			if (atom == null) {
				throw new RuntimeException("Incomplete choice point description found (no atom). Should not happen.");
			}
			if (influencers.get(atom) != null) {
				throw new RuntimeException("Received choice information repeatedly. Should not happen.");
			}
			Integer enabler = atomToEnabler.getValue();
			Integer disabler = disablers.get(atom);
			if (enabler == null || disabler == null) {
				throw new RuntimeException("Incomplete choice point description found (no enabler or disabler). Should not happen.");
			}
			ChoicePoint choicePoint = new ChoicePoint(atom, enabler, disabler);
			influencers.put(atom, choicePoint);
			influencers.put(enabler, choicePoint);
			influencers.put(disabler, choicePoint);
		}
	}

	boolean isActiveChoiceAtom(int atom) {
		ChoicePoint choicePoint = influencers.get(atom);
		return choicePoint != null && choicePoint.isActive && choicePoint.atom == atom;
	}

	public int getNextActiveChoiceAtom() {
		return activeChoicePoints.size() > 0 ? activeChoicePoints.iterator().next().atom : 0;
	}

	boolean isAtomChoice(int atom) {
		ChoicePoint choicePoint = influencers.get(atom);
		return choicePoint != null && choicePoint.atom == atom;
	}
}