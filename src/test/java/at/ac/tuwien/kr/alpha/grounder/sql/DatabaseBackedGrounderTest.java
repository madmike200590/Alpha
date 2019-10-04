package at.ac.tuwien.kr.alpha.grounder.sql;

import java.sql.SQLException;

import org.junit.Test;

import at.ac.tuwien.kr.alpha.Alpha;
import at.ac.tuwien.kr.alpha.common.program.impl.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.common.rule.impl.InternalRule;

public class DatabaseBackedGrounderTest {

	private static final String SIMPLE1_ASP = "p(a). q(a). q(b). r(X) :- p(X), q(X).";

	@Test
	public void groundingSimpleTest1() throws SQLException {
		Alpha system = new Alpha();
		AnalyzedProgram analyzed = AnalyzedProgram.analyzeNormalProgram(system.normalizeProgram(system.readProgramString(SIMPLE1_ASP)));
		DatabaseBackedGrounder grounder = new DatabaseBackedGrounder(analyzed);
		InternalRule rule = analyzed.getRules().get(0);
		// PreparedStatement groundingStmt = grounder.getRuleGroundingSelects().get(rule.getRuleId());
		// System.out.println("Grounding sql = " + groundingStmt.toString());

	}

}
