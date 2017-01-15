/**
 * Copyright (c) 2016, the Alpha Team.
 * All rights reserved.
 * 
 * Additional changes made by Siemens.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.BasicAnswerSet;
import at.ac.tuwien.kr.alpha.grounder.ChoiceGrounder;
import at.ac.tuwien.kr.alpha.grounder.DummyGrounder;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.grounder.NaiveGrounder;
import at.ac.tuwien.kr.alpha.grounder.parser.ParsedProgram;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static at.ac.tuwien.kr.alpha.Main.parseVisit;
import static at.ac.tuwien.kr.alpha.MainTest.stream;
import static org.junit.Assert.assertEquals;

public class SolverTests extends AbstractSolverTests {
	/**
	 * Sets the logging level to TRACE. Useful for debugging; call at beginning of test case.
	 */
	private static void enableTracing() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(ch.qos.logback.classic.Level.TRACE);
	}

	private static void enableDebugLog() {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.DEBUG);
	}

	@Test
	public void testFactsOnlyProgram() throws IOException {
		String testProgram = "p(a). p(b). foo(13). foo(16). q(a). q(c).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		Grounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		List<AnswerSet> answerSets = solver.collectList();

		assertEquals(1, answerSets.size());

		AnswerSet expected = new BasicAnswerSet.Builder()
			.predicate("q").instance("a").instance("c")
			.predicate("p").instance("a").instance("b")
			.predicate("foo").instance("13").instance("16")
			.build();

		assertEquals(expected, answerSets.get(0));
	}

	@Test
	public void testSimpleRule() throws Exception {
		String testProgram = "p(a). p(b). r(X) :- p(X).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		Grounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		List<AnswerSet> answerSets = solver.collectList();

		AnswerSet expected = new BasicAnswerSet.Builder()
			.predicate("p").instance("a").instance("b")
			.predicate("r").instance("a").instance("b")
			.build();

		assertEquals(1, answerSets.size());
		assertEquals(expected, answerSets.get(0));
	}

	@Test
	public void testSimpleRuleWithGroundPart() throws Exception {
		String testProgram =
			"p(1)." +
				"p(2)." +
				"q(X) :-  p(X), p(1).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		Grounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		List<AnswerSet> answerSets = solver.collectList();

		assertEquals(1, answerSets.size());
		AnswerSet expected = new BasicAnswerSet.Builder()
			.predicate("q").instance("1").instance("2")
			.predicate("p").instance("1").instance("2")
			.build();

		assertEquals(expected, answerSets.get(0));
	}

	@Test
	public void testProgramZeroArityPredicates() throws Exception {
		String testProgram = "a. p(X) :- b, r(X).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		Grounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		List<AnswerSet> answerSets = solver.collectList();

		assertEquals(1, answerSets.size());

		AnswerSet expected = new BasicAnswerSet.Builder()
			.predicate("a")
			.build();

		assertEquals(expected, answerSets.get(0));
	}

	@Test
	public void testGuessingGroundProgram() throws Exception {
		Solver solver = getInstance(new NaiveGrounder(parseVisit(stream("a :- not b. b :- not a."))));

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder().predicate("a").build(),
			new BasicAnswerSet.Builder().predicate("b").build()
		));

		assertEquals(expected, solver.collectSet());
	}

	@Test
	public void testGuessingProgramNonGround() throws Exception {
		String testProgram = "dom(1). dom(2). dom(3)." +
			"p(X) :- dom(X), not q(X)." +
			"q(X) :- dom(X), not p(X).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		final BasicAnswerSet.Builder base = new BasicAnswerSet.Builder()
			.predicate("dom").instance("1").instance("2").instance("3");

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("1").instance("2")
				.predicate("p").instance("3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("1")
				.predicate("p").instance("2").instance("3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("2")
				.predicate("p").instance("1").instance("3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("p").instance("1").instance("2").instance("3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("1").instance("2").instance("3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("1").instance("3")
				.predicate("p").instance("2")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("2").instance("3")
				.predicate("p").instance("1")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("q").instance("3")
				.predicate("p").instance("1").instance("2")
				.build()
		));

		assertEquals(expected, solver.collectSet());
	}

	@Test
	public void dummyGrounder() {
		assertEquals(DummyGrounder.EXPECTED, getInstance(new DummyGrounder()).collectSet());
	}

	@Test
	public void choiceGrounder() {
		assertEquals(ChoiceGrounder.EXPECTED, getInstance(new ChoiceGrounder()).collectSet());
	}

	@Test
	public void guessingProgram3Way() throws IOException {
		String testProgram = "a :- not b, not c." +
			"b :- not a, not c." +
			"c :- not a, not b.";

		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);

		Solver solver = getInstance(grounder);

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder()
				.predicate("a")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("b")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("c")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void emptyProgramYieldsEmptyAnswerSet() throws IOException {
		ParsedProgram parsedProgram = parseVisit(stream(""));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);

		List<AnswerSet> answerSets = getInstance(grounder).collectList();
		assertEquals(1, answerSets.size());
		assertEquals(BasicAnswerSet.EMPTY, answerSets.get(0));
	}

	@Test
	public void guessingMultipleAnswerSets() throws IOException {
		String testProgram = "a :- not nota.\n" +
			"nota :- not a.\n" +
			"b :- not notb.\n" +
			"notb :- not b.\n" +
			"c :- not notc.\n" +
			"notc :- not c.\n" +
			":- nota,notb,notc.";

		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);

		Solver solver = getInstance(grounder);

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder()
				.predicate("a")
				.predicate("b")
				.predicate("c")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("nota")
				.predicate("b")
				.predicate("c")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("a")
				.predicate("notb")
				.predicate("c")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("nota")
				.predicate("notb")
				.predicate("c")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("a")
				.predicate("b")
				.predicate("notc")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("nota")
				.predicate("b")
				.predicate("notc")
				.build(),
			new BasicAnswerSet.Builder()
				.predicate("a")
				.predicate("notb")
				.predicate("notc")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void builtinAtoms() throws IOException {
		String testProgram = "dom(1). dom(2). dom(3). dom(4). dom(5)." +
			"p(X) :- dom(X), X = 4." +
			"r(Y) :- dom(Y), Y <= 2.";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		Set<AnswerSet> expected = new HashSet<>(Collections.singletonList(
			new BasicAnswerSet.Builder()
				.predicate("dom")
				.instance("1")
				.instance("2")
				.instance("3")
				.instance("4")
				.instance("5")
				.predicate("p")
				.instance("4")
				.predicate("r")
				.instance("1")
				.instance("2")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void builtinAtomsGroundRule() throws IOException {
		String testProgram = "a :- 13 != 4." +
			"b :- 2 != 3, 2 = 3." +
			"c :- 2 <= 3, not 2 > 3.";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		Set<AnswerSet> expected = new HashSet<>(Collections.singletonList(
			new BasicAnswerSet.Builder()
				.predicate("a")
				.predicate("c")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void guessingProgramConstraint() throws IOException {
		String testProgram =
			"eq(1,1).\n" +
			"eq(2,2).\n" +
			"eq(3,3).\n" +
			"var(1).\n" +
			"var(2).\n" +
			"var(3).\n" +
			"val(VAR,1):-var(VAR),not val(VAR,2),not val(VAR,3).\n" +
			"val(VAR,2):-var(VAR),not val(VAR,1),not val(VAR,3).\n" +
			"val(VAR,3):-var(VAR),not val(VAR,1),not val(VAR,2).\n" +
			"%:- val(VAR1,VAL1), val(VAR2,VAL2), eq(VAL1,VAL2), not eq(VAR1,VAR2).\n" +
			":- eq(VAL1,VAL2), not eq(VAR1,VAR2), val(VAR1,VAL1), val(VAR2,VAL2).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		final BasicAnswerSet.Builder base = new BasicAnswerSet.Builder()
			.predicate("eq")
			.instance("1", "1")
			.instance("2", "2")
			.instance("3", "3")
			.predicate("var")
			.instance("1")
			.instance("2")
			.instance("3");

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("1", "1")
				.instance("2", "2")
				.instance("3", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("1", "1")
				.instance("3", "2")
				.instance("2", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("2", "1")
				.instance("1", "2")
				.instance("3", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("2", "1")
				.instance("3", "2")
				.instance("1", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("3", "1")
				.instance("1", "2")
				.instance("2", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("3", "1")
				.instance("2", "2")
				.instance("1", "3")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void guessingProgramConstraintPermutation() throws IOException {
		String testProgram =
			"eq(1,1).\n" +
				"eq(2,2).\n" +
				"eq(3,3).\n" +
				"var(1).\n" +
				"var(2).\n" +
				"var(3).\n" +
				"val(VAR,1):-var(VAR),not val(VAR,2),not val(VAR,3).\n" +
				"val(VAR,2):-var(VAR),not val(VAR,1),not val(VAR,3).\n" +
				"val(VAR,3):-var(VAR),not val(VAR,1),not val(VAR,2).\n" +
				":- val(VAR1,VAL1), val(VAR2,VAL2), eq(VAL1,VAL2), not eq(VAR1,VAR2).\n" +
				"%:- eq(VAL1,VAL2), not eq(VAR1,VAR2), val(VAR1,VAL1), val(VAR2,VAL2).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		final BasicAnswerSet.Builder base = new BasicAnswerSet.Builder()
			.predicate("eq")
			.instance("1", "1")
			.instance("2", "2")
			.instance("3", "3")
			.predicate("var")
			.instance("1")
			.instance("2")
			.instance("3");

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("1", "1")
				.instance("2", "2")
				.instance("3", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("1", "1")
				.instance("3", "2")
				.instance("2", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("2", "1")
				.instance("1", "2")
				.instance("3", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("2", "1")
				.instance("3", "2")
				.instance("1", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("3", "1")
				.instance("1", "2")
				.instance("2", "3")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("val")
				.instance("3", "1")
				.instance("2", "2")
				.instance("1", "3")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void simpleNoPropagation() throws IOException {
		String testProgram = "val(1,1).\n" +
			"val(2,2).\n" +
			"something:- val(VAR1,VAL1), val(VAR2,VAL2), anything(VAL1,VAL2).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		Set<AnswerSet> expected = new HashSet<>(Collections.singletonList(
			new BasicAnswerSet.Builder()
				.predicate("val")
				.instance("1", "1")
				.instance("2", "2")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void guessingAndPropagationAfterwards() throws IOException {
		String testProgram = "node(a).\n" +
			"node(b).\n" +
			"in(X) :- not out(X), node(X).\n" +
			"out(X) :- not in(X), node(X).\n" +
			"pair(X,Y) :- in(X), in(Y).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		final BasicAnswerSet.Builder base = new BasicAnswerSet.Builder()
			.predicate("node")
			.instance("a")
			.instance("b");

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder(base)
				.predicate("in")
				.instance("a")
				.instance("b")
				.predicate("pair")
				.instance("a", "a")
				.instance("a", "b")
				.instance("b", "a")
				.instance("b", "b")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("in")
				.instance("b")
				.predicate("out")
				.instance("a")
				.predicate("pair")
				.instance("b", "b")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("in")
				.instance("a")
				.predicate("out")
				.instance("b")
				.predicate("pair")
				.instance("a", "a")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("out")
				.instance("a")
				.instance("b")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void guessingAndConstraints() throws IOException {
		String testProgram = "node(a).\n" +
			"node(b).\n" +
			"edge(b,a).\n" +
			"in(X) :- not out(X), node(X).\n" +
			"out(X) :- not in(X), node(X).\n" +
			":- in(X), in(Y), edge(X,Y).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		NaiveGrounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		final BasicAnswerSet.Builder base = new BasicAnswerSet.Builder()
			.predicate("node")
			.instance("a")
			.instance("b");

		Set<AnswerSet> expected = new HashSet<>(Arrays.asList(
			new BasicAnswerSet.Builder(base)
				.predicate("in")
				.instance("b")
				.predicate("out")
				.instance("a")
				.predicate("edge")
				.instance("b", "a")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("in")
				.instance("a")
				.predicate("out")
				.instance("b")
				.predicate("edge")
				.instance("b", "a")
				.build(),
			new BasicAnswerSet.Builder(base)
				.predicate("out")
				.instance("a")
				.instance("b")
				.predicate("edge")
				.instance("b", "a")
				.build()
		));

		Set<AnswerSet> answerSets = solver.collectSet();
		assertEquals(expected, answerSets);
	}

	@Test
	public void testUnsatisfiableProgram() throws IOException {
		String testProgram = "p(a). p(b). :- p(a), p(b).";
		ParsedProgram parsedProgram = parseVisit(stream(testProgram));
		Grounder grounder = new NaiveGrounder(parsedProgram);
		Solver solver = getInstance(grounder);

		List<AnswerSet> answerSets = solver.collectList();

		assertEquals(0, answerSets.size());
	}
}