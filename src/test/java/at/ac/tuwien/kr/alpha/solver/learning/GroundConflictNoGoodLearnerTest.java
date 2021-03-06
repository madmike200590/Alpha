package at.ac.tuwien.kr.alpha.solver.learning;

import at.ac.tuwien.kr.alpha.common.AtomStore;
import at.ac.tuwien.kr.alpha.common.AtomStoreImpl;
import at.ac.tuwien.kr.alpha.common.AtomStoreTest;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.solver.*;
import org.junit.Ignore;
import org.junit.Test;

import static at.ac.tuwien.kr.alpha.common.NoGoodTest.fromOldLiterals;
import static at.ac.tuwien.kr.alpha.solver.AntecedentTest.antecedentsEquals;
import static org.junit.Assert.*;

/**
 * Copyright (c) 2016-2019, the Alpha Team.
 */
public class GroundConflictNoGoodLearnerTest {

	private final WritableAssignment assignment;
	private final NoGoodStore store;
	private AtomStore atomStore;

	public GroundConflictNoGoodLearnerTest() {
		atomStore = new AtomStoreImpl();
		AtomStoreTest.fillAtomStore(atomStore, 20);
		this.assignment = new TrailAssignment(atomStore);
		this.assignment.growForMaxAtomId();
		this.store = new NoGoodStoreAlphaRoaming(assignment);
		this.store.growForMaxAtomId(20);
	}

	@Test
	public void smallConflictNonTrivial1UIP() {
		GroundConflictNoGoodLearner learner = new GroundConflictNoGoodLearner(assignment, atomStore);

		NoGood n1 = new NoGood(fromOldLiterals(2, -8, 1));
		NoGood n2 = new NoGood(fromOldLiterals(-1, -7));
		NoGood n3 = new NoGood(fromOldLiterals(-3, 1));
		NoGood n4 = new NoGood(fromOldLiterals(5, 3));
		NoGood n5 = new NoGood(fromOldLiterals(6, -5));
		NoGood n6 = new NoGood(fromOldLiterals(4, -2));
		NoGood n7 = new NoGood(fromOldLiterals(-6, -4));
		store.add(10, n1);
		store.add(11, n2);
		store.add(12, n3);
		store.add(13, n4);
		store.add(14, n5);
		store.add(15, n6);
		store.add(16, n7);

		assignment.choose(9, ThriceTruth.TRUE);
		assignment.choose(8, ThriceTruth.FALSE);
		assertNull(store.propagate());
		assertFalse(store.didPropagate());
		assignment.choose(7, ThriceTruth.FALSE);
		ConflictCause conflictCause = store.propagate();
		assertTrue(store.didPropagate());

		assertNotNull(conflictCause);
		Antecedent violatedNoGood = conflictCause.getAntecedent();
		assertNotNull(violatedNoGood);
		assertTrue(antecedentsEquals(violatedNoGood, n5.asAntecedent()) || antecedentsEquals(violatedNoGood, n7.asAntecedent()));
		GroundConflictNoGoodLearner.ConflictAnalysisResult analysisResult = learner.analyzeConflictingNoGood(conflictCause.getAntecedent());
		NoGood learnedNoGood = analysisResult.learnedNoGood;
		assertEquals(new NoGood(fromOldLiterals(1, -8)), learnedNoGood);
		int backjumpingDecisionLevel = analysisResult.backjumpLevel;
		assertEquals(backjumpingDecisionLevel, 2);
	}

	@Ignore // TrailAssignment no longer propagates at lower decision level.
	@Test
	public void subCurrentDLPropagationWithChoiceCauseOfConflict() {
		GroundConflictNoGoodLearner learner = new GroundConflictNoGoodLearner(assignment, atomStore);
		NoGood n1 = new NoGood(fromOldLiterals(1, -2));
		NoGood n2 = new NoGood(fromOldLiterals(2, 3));
		store.add(10, n1);
		assignment.choose(1, ThriceTruth.TRUE);
		assignment.choose(3, ThriceTruth.TRUE);
		store.propagate();
		assertEquals(ThriceTruth.MBT, assignment.get(2).getTruth());
		assertEquals(1, assignment.get(2).getDecisionLevel());
		ConflictCause conflictCause = store.add(11, n2);
		assertNotNull(conflictCause);
		assertNotNull(conflictCause.getAntecedent());
		GroundConflictNoGoodLearner.ConflictAnalysisResult conflictAnalysisResult = learner.analyzeConflictingNoGood(conflictCause.getAntecedent());
		assertNull(conflictAnalysisResult.learnedNoGood);
		assertEquals(2, conflictAnalysisResult.backjumpLevel);

	}
}