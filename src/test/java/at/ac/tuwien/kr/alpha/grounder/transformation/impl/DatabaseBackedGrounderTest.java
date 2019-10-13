package at.ac.tuwien.kr.alpha.grounder.transformation.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.Alpha;
import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.impl.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.common.program.impl.InputProgram;
import at.ac.tuwien.kr.alpha.common.program.impl.InternalProgram;
import at.ac.tuwien.kr.alpha.common.program.impl.NormalProgram;
import at.ac.tuwien.kr.alpha.grounder.sql.DatabaseBackedGrounder;
import at.ac.tuwien.kr.alpha.test.util.TestUtils;

public class DatabaseBackedGrounderTest {

	@Test
	public void testSimpleProgram1() throws IOException {
		String aspStr = "p(a). p(b). q(b). q(X) :- p(X).";
		Alpha system = new Alpha();
		InputProgram prg = system.readProgramString(aspStr);
		NormalProgram normal = system.normalizeProgram(prg);
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(normal);
		DatabaseBackedGrounder grounder = new DatabaseBackedGrounder();
		InternalProgram evaluated = grounder.apply(analyzed);
		grounder.close();
		BasicAtom qOfB = BasicAtom.newInstance("q", "b");
		List<Atom> facts = evaluated.getFacts();
		int numQOfB = 0;
		for (Atom at : facts) {
			if (at.equals(qOfB)) {
				numQOfB++;
			}
		}
		Assert.assertEquals(1, numQOfB);
		Set<AnswerSet> answerSets = system.solve(evaluated).collect(Collectors.toSet());
		TestUtils.assertAnswerSetsEqual("p(a), p(b), q(a), q(b)", answerSets);
	}

}
