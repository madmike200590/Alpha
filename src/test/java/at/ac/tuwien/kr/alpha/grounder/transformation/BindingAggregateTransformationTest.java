package at.ac.tuwien.kr.alpha.grounder.transformation;

public class BindingAggregateTransformationTest {

	//@formatter:off
	private static final String CNT_SIMPLE_ASP = 
			"thing(1..3)."
			+ "cnt_things(X) :- X = #count{N : thing(N)}.";
	private static final String CNT_MULTIPLE_AGGREGATES_ASP = 
			"thing1(1..3). thing2(1..3)."
			+ "cnt_things(X, Y) :- X = #count{N : thing1(N)}, Y = #count{K : thing2(K)}.";
	//@formatter:on
	
	//FIXME
//	@Test
//	public void countSimple() {
//		Alpha alpha = new Alpha();
//		ProgramTransformation<InputProgram, InputProgram> aggregateRewriting = new BindingAggregateTransformation();
//		InputProgram input = alpha.readProgramString(CNT_SIMPLE_ASP);
//		InputProgram rewritten = aggregateRewriting.apply(input);
//		BasicRule expectedAggregateResultRule = RuleParser
//				.parse("cnt_things(X) :- aggregate_result(count_1, X).");
//		BasicRule expectedEqualityRule = RuleParser
//				.parse("aggregate_result(AGGREGATE_ID, VAL) :- aggregate(AGGREGATE_ID), leq_aggregate(AGGREGATE_ID, VAL), not leq_aggregate(AGGREGATE_ID, NEXTVAL), NEXTVAL = VAL + 1.");
//		BasicRule expectedElementTupleRule = RuleParser
//				.parse("cnt_element_tuple(count_1, tuple(N)) :- thing(N).");
//		BasicRule expectedCandidateRule = RuleParser
//				.parse("cnt_candidate(AGGREGATE_ID, I) :- aggregate(AGGREGATE_ID), cnt_element_tuple(AGGREGATE_ID, TUPLE), element_tuple_ordinal(AGGREGATE_ID, TUPLE, I).");
//		BasicRule expectedValueRule = RuleParser
//				.parse("leq_aggregate(count_1, CNT) :- cnt_candidate(count_1, CNT), CNT <= #count{N : thing(N)}.");
//		TestUtils.assertProgramContainsRule(rewritten, expectedAggregateResultRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedEqualityRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedElementTupleRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedCandidateRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedValueRule);
//		Assert.assertEquals(5, rewritten.getRules().size());	
//		Atom aggregateFact = new BasicAtom(Predicate.getInstance("aggregate", 1), ConstantTerm.getSymbolicInstance("count_1"));
//		Assert.assertTrue(rewritten.getFacts().contains(aggregateFact));
//	}
	
	//FIXME
//	@Test
//	public void countMultipleAggregates() {
//		Alpha alpha = new Alpha();
//		ProgramTransformation<InputProgram, InputProgram> aggregateRewriting = new BindingAggregateTransformation();
//		InputProgram input = alpha.readProgramString(CNT_MULTIPLE_AGGREGATES_ASP);
//		InputProgram rewritten = aggregateRewriting.apply(input);
//		BasicRule expectedAggregateResultRule = RuleParser
//				.parse("cnt_things(X, Y) :- aggregate_result(count_1, X), aggregate_result(count_2, Y).");
//		BasicRule expectedEqualityRule = RuleParser
//				.parse("aggregate_result(AGGREGATE_ID, VAL) :- aggregate(AGGREGATE_ID), leq_aggregate(AGGREGATE_ID, VAL), not leq_aggregate(AGGREGATE_ID, NEXTVAL), NEXTVAL = VAL + 1.");
//		BasicRule expectedElementNTupleRule = RuleParser
//				.parse("cnt_element_tuple(count_1, tuple(N)) :- thing1(N).");
//		BasicRule expectedElementKTupleRule = RuleParser
//				.parse("cnt_element_tuple(count_2, tuple(K)) :- thing2(K).");
//		BasicRule expectedCandidateRule = RuleParser
//				.parse("cnt_candidate(AGGREGATE_ID, I) :- aggregate(AGGREGATE_ID), cnt_element_tuple(AGGREGATE_ID, TUPLE), element_tuple_ordinal(AGGREGATE_ID, TUPLE, I).");
//		BasicRule expectedValueRule1 = RuleParser
//				.parse("leq_aggregate(count_1, CNT) :- cnt_candidate(count_1, CNT), CNT <= #count{N : thing1(N)}.");
//		BasicRule expectedValueRule2 = RuleParser
//				.parse("leq_aggregate(count_2, CNT) :- cnt_candidate(count_2, CNT), CNT <= #count{K : thing2(K)}.");
//		TestUtils.assertProgramContainsRule(rewritten, expectedAggregateResultRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedEqualityRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedElementNTupleRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedElementKTupleRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedCandidateRule);
//		TestUtils.assertProgramContainsRule(rewritten, expectedValueRule1);
//		TestUtils.assertProgramContainsRule(rewritten, expectedValueRule2);
//		Assert.assertEquals(7, rewritten.getRules().size());	
//		Atom aggregate1Fact = new BasicAtom(Predicate.getInstance("aggregate", 1), ConstantTerm.getSymbolicInstance("count_1"));
//		Atom aggregate2Fact = new BasicAtom(Predicate.getInstance("aggregate", 1), ConstantTerm.getSymbolicInstance("count_2"));
//		Assert.assertTrue(rewritten.getFacts().contains(aggregate1Fact));
//		Assert.assertTrue(rewritten.getFacts().contains(aggregate2Fact));
//	}
}
