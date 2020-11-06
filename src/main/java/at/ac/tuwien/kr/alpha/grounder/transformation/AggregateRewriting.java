package at.ac.tuwien.kr.alpha.grounder.transformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.Terms;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives.DIRECTIVE;
import at.ac.tuwien.kr.alpha.grounder.parser.ProgramParser;

// FIXME do proper internalizing of predicates
// Internalize before stitching programs together (otherwise clashes!)
// (maybe internalize on getInstance? in that case don't forget stuff that's parsed internally!)
public class AggregateRewriting extends ProgramTransformation<InputProgram, InputProgram> {

	public static final Predicate AGGREGATE_RESULT = Predicate.getInstance("aggregate_result", 2);
	private static final Predicate AGGREGATE = Predicate.getInstance("aggregate", 1);
	private static final Predicate LEQ_AGGREGATE = Predicate.getInstance("leq_aggregate", 2);

	private static final Predicate CNT_CANDIDATE = Predicate.getInstance("cnt_candidate", 2);
	private static final Predicate SUM_CANDIDATE = Predicate.getInstance("sum_candidate", 2);
	private static final Predicate CNT_ELEMENT_TUPLE = Predicate.getInstance("cnt_element_tuple", 2);
	private static final Predicate SUM_ELEMENT_TUPLE = Predicate.getInstance("sum_element_tuple", 3);
	private static final Predicate MINMAX_ELEMENT_TUPLE = Predicate.getInstance("minmax_element_tuple", 2);
	private static final Predicate ELEMENT_TUPLE_ORDINAL = Predicate.getInstance("element_tuple_ordinal", 3);

	private static final String ELEMENT_TUPLE_FN_SYM = "tuple";

	private static final String CNT_CANDIDATE_RULE = String.format(
			"%s(AGGREGATE_ID, I) :- %s(AGGREGATE_ID), %s(AGGREGATE_ID, TUPLE), %s(AGGREGATE_ID, TUPLE, I).",
			CNT_CANDIDATE.getName(), AGGREGATE.getName(), CNT_ELEMENT_TUPLE.getName(), ELEMENT_TUPLE_ORDINAL.getName());

	//@formatter:off
	// TODO can we get around hardcoded predicate names here??
	private static final String SUM_CANDIDATE_PROG = 
			"sum_element_at_index(SUM_ID, VAL, IDX) :- aggregate(SUM_ID), sum_element_tuple(SUM_ID, TPL, VAL), element_tuple_ordinal(SUM_ID, TPL, IDX)."
			+ "sum_element_index(SUM_ID, IDX) :- sum_element_at_index(SUM_ID, _, IDX)."
			// In case all elements are false, 0 is a candidate sum
			+ "sum_at_idx_candidate(SUM_ID, 0, 0) :- sum_element_at_index(SUM_ID, _, _)."
			// Assuming the element with index I is false, all candidate sums up to the last index are also valid candidates for
			// this index
			+ "sum_at_idx_candidate(SUM_ID, I, CSUM) :- sum_element_index(SUM_ID, I), sum_at_idx_candidate(SUM_ID, IPREV, CSUM), IPREV = I - 1."
			// Assuming the element with index I is true, all candidate sums up to the last index plus the value of the element at
			// index I are candidate sums."
			+ "sum_at_idx_candidate(SUM_ID, I, CSUM) :- sum_element_at_index(SUM_ID, VAL, I), sum_at_idx_candidate(SUM_ID, IPREV, PSUM), IPREV = I - 1, CSUM = PSUM + VAL."
			// Project indices away, we only need candidate values for variable binding.
			+ "sum_candidate(SUM_ID, CSUM) :- sum_at_idx_candidate(SUM_ID, _, CSUM).";
	
	private static final String MIN_ELEMENT_SEARCH_PROG =
			"element_tuple_less_than(AGGREGATE_ID, LESS, THAN) :- "
			+ "aggregate(AGGREGATE_ID), minmax_element_tuple(AGGREGATE_ID, LESS), minmax_element_tuple(AGGREGATE_ID, THAN), LESS < THAN."
			+ "element_tuple_has_smaller(AGGREGATE_ID, TPL) :- element_tuple_less_than(AGGREGATE_ID, _, TPL)."
			+ "min_element_tuple(AGGREGATE_ID, MIN) :- "
			+ "aggregate(AGGREGATE_ID), minmax_element_tuple(AGGREGATE_ID, MIN), not element_tuple_has_smaller(AGGREGATE_ID, MIN).";
	
	private static final String MAX_ELEMENT_SEARCH_PROG = 
			"element_tuple_less_than(AGGREGATE_ID, LESS, THAN) :- "
			+ "aggregate(AGGREGATE_ID), minmax_element_tuple(AGGREGATE_ID, LESS), minmax_element_tuple(AGGREGATE_ID, THAN), LESS < THAN."
			+ "element_tuple_has_greater(AGGREGATE_ID, TPL) :- element_tuple_less_than(AGGREGATE_ID, TPL, _)."
			+ "max_element_tuple(AGGREGATE_ID, MAX) :- "
			+ "aggregate(AGGREGATE_ID), minmax_element_tuple(AGGREGATE_ID, MAX), not element_tuple_has_greater(AGGREGATE_ID, MAX).";
	//@formatter:on

	private final ProgramParser parser = new ProgramParser();

	// TODO add a switch to control whether internal predicates should be internalized (debugging!)
	private final AggregateRewritingConfig config;

	public AggregateRewriting(AggregateRewritingConfig config) {
		this.config = config;
	}

	/**
	 * Transformation steps:
	 * - Preprocessing: build a "symbol table", assigning an ID to each distinct aggregate literal
	 * - Bounds normalization: everything to "left-associative" expressions with one operator
	 * - Operator normalization: everything to expressions of form "RESULT LEQ #agg{...}"
	 * - Cardinality normalization: rewrite #count expressions
	 * - Sum normalization: rewrite #sum expressions
	 */
	@Override
	public InputProgram apply(InputProgram inputProgram) {
		AggregateRewritingContext ctx = new AggregateRewritingContext();
		List<BasicRule> outputRules = new ArrayList<>();
		for (BasicRule inputRule : inputProgram.getRules()) {
			for (BasicRule splitRule : AggregateLiteralSplitting.split(inputRule)) {
				BasicRule operatorNormalizedRule = AggregateOperatorNormalization.normalize(splitRule);
				boolean hasAggregate = ctx.registerRule(operatorNormalizedRule);
				if (hasAggregate) {
					outputRules.add(insertAggregatePlaceholders(ctx, operatorNormalizedRule));
				} else {
					outputRules.add(operatorNormalizedRule);
				}
			}
		}
		List<Atom> aggregateFacts = new ArrayList<>();
		for (AggregateLiteral lit : ctx.getLiteralsToRewrite()) {
			aggregateFacts.add(new BasicAtom(AGGREGATE, ConstantTerm.getSymbolicInstance(ctx.getAggregateId(lit))));
		}
		List<BasicRule> aggregateEncodingRules = new ArrayList<>();
		for (AggregateFunctionSymbol func : ctx.getAggregateFunctionsToRewrite().keySet()) {
			aggregateEncodingRules.addAll(encodeAggregateFunction(func, ctx.getAggregateFunctionsToRewrite().get(func), ctx));
		}
		InlineDirectives aggregateEncodingDirectives = new InlineDirectives();
		aggregateEncodingDirectives.addDirective(DIRECTIVE.enum_predicate_is, ELEMENT_TUPLE_ORDINAL.getName());
		InputProgram resultProgram = InputProgram.builder()
				.addFacts(inputProgram.getFacts())
				.addFacts(aggregateFacts)
				.addRules(outputRules)
				.addInlineDirectives(inputProgram.getInlineDirectives())
				.addRules(aggregateEncodingRules)
				.addInlineDirectives(aggregateEncodingDirectives)
				.build();
		// FIXME add these transforms to general aggregate compilation
		CardinalityNormalization countRewriting = new CardinalityNormalization();
		SumNormalization sumRewriting = new SumNormalization();
		return countRewriting.andThen(sumRewriting).apply(resultProgram);
	}

	private List<BasicRule> encodeAggregateFunction(AggregateFunctionSymbol func, Set<AggregateLiteral> literals, AggregateRewritingContext ctx) {
		switch (func) {
			case COUNT:
				return encodeCountAggregate(literals, ctx);
			case SUM:
				return encodeSumAggregate(literals, ctx);
			case MIN:
				return encodeMinAggregate(literals, ctx);
			case MAX:
				return encodeMaxAggregate(literals, ctx);
			default:
				throw new UnsupportedOperationException();
		}
	}

	private List<BasicRule> encodeCountAggregate(Set<AggregateLiteral> literals, AggregateRewritingContext ctx) {
		List<BasicRule> retVal = new ArrayList<>();
		for (AggregateLiteral countAggregate : literals) {
			for (AggregateElement element : countAggregate.getAtom().getAggregateElements()) {
				retVal.add(buildCountElementTupleRule(ctx.getAggregateId(countAggregate), element));
			}
			retVal.add(buildCntLeqRule(countAggregate, ctx.getAggregateId(countAggregate)));
		}
		InputProgram candidateRulePrg = parser.parse(CNT_CANDIDATE_RULE);
		retVal.add(candidateRulePrg.getRules().get(0));
		retVal.add(buildEqualityRule());
		return retVal;
	}

	private List<BasicRule> encodeSumAggregate(Set<AggregateLiteral> literals, AggregateRewritingContext ctx) {
		List<BasicRule> retVal = new ArrayList<>();
		for (AggregateLiteral sumAggregate : literals) {
			for (AggregateElement element : sumAggregate.getAtom().getAggregateElements()) {
				retVal.add(buildSumElementTupleRule(ctx.getAggregateId(sumAggregate), element));
			}
			retVal.add(buildSumLeqRule(sumAggregate, ctx.getAggregateId(sumAggregate)));
		}
		InputProgram candidateGenerationPrg = parser.parse(SUM_CANDIDATE_PROG);
		retVal.addAll(candidateGenerationPrg.getRules());
		retVal.add(buildEqualityRule());
		return retVal;
	}

	private List<BasicRule> encodeMinAggregate(Set<AggregateLiteral> literals, AggregateRewritingContext ctx) {
		List<BasicRule> retVal = new ArrayList<>();
		for (AggregateLiteral minAggregate : literals) {
			for (AggregateElement element : minAggregate.getAtom().getAggregateElements()) {
				retVal.add(buildOrderedElementTupleRule(ctx.getAggregateId(minAggregate), element));
			}
			retVal.add(buildMinAggregateResultRule(ctx.getAggregateId(minAggregate)));
		}
		InputProgram minElementSearchPrg = parser.parse(MIN_ELEMENT_SEARCH_PROG);
		retVal.addAll(minElementSearchPrg.getRules());
		return retVal;
	}

	private List<BasicRule> encodeMaxAggregate(Set<AggregateLiteral> literals, AggregateRewritingContext ctx) {
		List<BasicRule> retVal = new ArrayList<>();
		for (AggregateLiteral maxAggregate : literals) {
			for (AggregateElement element : maxAggregate.getAtom().getAggregateElements()) {
				retVal.add(buildOrderedElementTupleRule(ctx.getAggregateId(maxAggregate), element));
			}
			retVal.add(buildMaxAggregateResultRule(ctx.getAggregateId(maxAggregate)));
		}
		InputProgram maxElementSearchPrg = parser.parse(MAX_ELEMENT_SEARCH_PROG);
		retVal.addAll(maxElementSearchPrg.getRules());
		return retVal;
	}

	private BasicRule buildEqualityRule() {
		VariableTerm aggregateIdVar = VariableTerm.getInstance("AGGREGATE_ID");
		VariableTerm valueVar = VariableTerm.getInstance("VAL");
		VariableTerm nextValVar = VariableTerm.getInstance("NEXTVAL");
		Atom headAtom = new BasicAtom(AGGREGATE_RESULT, aggregateIdVar, valueVar);
		Literal leqValue = new BasicLiteral(new BasicAtom(LEQ_AGGREGATE, aggregateIdVar, valueVar), true);
		Literal notLeqNextval = new BasicLiteral(new BasicAtom(LEQ_AGGREGATE, aggregateIdVar, nextValVar), false);
		Literal bindNextval = Terms.incrementTerm(valueVar, nextValVar);
		Literal bindAggregateId = new BasicLiteral(new BasicAtom(AGGREGATE, aggregateIdVar), true);
		return BasicRule.getInstance(new NormalHead(headAtom), leqValue, notLeqNextval, bindNextval, bindAggregateId);
	}

	private BasicRule buildCountElementTupleRule(String aggregateId, AggregateElement element) {
		FunctionTerm elementTuple = FunctionTerm.getInstance(ELEMENT_TUPLE_FN_SYM, element.getElementTerms());
		Atom headAtom = new BasicAtom(CNT_ELEMENT_TUPLE, ConstantTerm.getSymbolicInstance(aggregateId), elementTuple);
		return new BasicRule(new NormalHead(headAtom), element.getElementLiterals());
	}

	private BasicRule buildSumElementTupleRule(String aggregateId, AggregateElement element) {
		FunctionTerm elementTuple = FunctionTerm.getInstance(ELEMENT_TUPLE_FN_SYM, element.getElementTerms());
		Term sumTerm = element.getElementTerms().get(0);
		Atom headAtom = new BasicAtom(SUM_ELEMENT_TUPLE, ConstantTerm.getSymbolicInstance(aggregateId), elementTuple, sumTerm);
		return new BasicRule(new NormalHead(headAtom), element.getElementLiterals());
	}

	private BasicRule buildOrderedElementTupleRule(String aggregateId, AggregateElement element) {
		FunctionTerm elementTuple = FunctionTerm.getInstance(ELEMENT_TUPLE_FN_SYM, element.getElementTerms());
		Atom headAtom = new BasicAtom(MINMAX_ELEMENT_TUPLE, ConstantTerm.getSymbolicInstance(aggregateId), elementTuple);
		return new BasicRule(new NormalHead(headAtom), element.getElementLiterals());
	}

	private BasicRule buildCntLeqRule(AggregateLiteral countEq, String countEqId) {
		VariableTerm cnt = VariableTerm.getInstance("CNT");
		Atom headAtom = new BasicAtom(LEQ_AGGREGATE, ConstantTerm.getSymbolicInstance(countEqId), cnt);
		AggregateAtom sourceAggregate = countEq.getAtom();
		Literal valueLeqCnt = new AggregateLiteral(
				new AggregateAtom(ComparisonOperator.LE, cnt, null, null, AggregateFunctionSymbol.COUNT, sourceAggregate.getAggregateElements()), true);
		Literal valueIsCandidate = new BasicLiteral(new BasicAtom(CNT_CANDIDATE, ConstantTerm.getSymbolicInstance(countEqId), cnt), true);
		return BasicRule.getInstance(new NormalHead(headAtom), valueLeqCnt, valueIsCandidate);
	}

	private BasicRule buildSumLeqRule(AggregateLiteral sumEq, String sumEqId) {
		VariableTerm sum = VariableTerm.getInstance("SUM");
		Atom headAtom = new BasicAtom(LEQ_AGGREGATE, ConstantTerm.getSymbolicInstance(sumEqId), sum);
		AggregateAtom sourceAggregate = sumEq.getAtom();
		Literal valueLeqSum = new AggregateLiteral(
				new AggregateAtom(ComparisonOperator.LE, sum, AggregateFunctionSymbol.SUM, sourceAggregate.getAggregateElements()), true);
		Literal valueIsCandidate = new BasicLiteral(new BasicAtom(SUM_CANDIDATE, ConstantTerm.getSymbolicInstance(sumEqId), sum), true);
		return BasicRule.getInstance(new NormalHead(headAtom), valueLeqSum, valueIsCandidate);
	}

	private BasicRule buildMinAggregateResultRule(String aggregateId) {
		return parser.parse(String.format("aggregate_result(%s, M) :- min_element_tuple(%s, tuple(M)).", aggregateId, aggregateId)).getRules().get(0);
	}

	private BasicRule buildMaxAggregateResultRule(String aggregateId) {
		return parser.parse(String.format("aggregate_result(%s, M) :- max_element_tuple(%s, tuple(M)).", aggregateId, aggregateId)).getRules().get(0);
	}

	// Transforms (restricted) aggregate literals of format "VAR1 OP #AGG_FN{...}" into literals of format
	// "_aggregate_result(AGG_ID,
	// output(VAR1, VAR2))".
	private static BasicRule insertAggregatePlaceholders(AggregateRewritingContext ctx, BasicRule rule) {
		List<Literal> rewrittenBody = new ArrayList<>();
		for (Literal lit : rule.getBody()) {
			if (lit instanceof AggregateLiteral && ctx.getLiteralsToRewrite().contains((AggregateLiteral) lit)) {
				BasicAtom aggregateOutputAtom = ctx.getAggregateOutputAtom((AggregateLiteral) lit);
				rewrittenBody.add(new BasicLiteral(aggregateOutputAtom, !lit.isNegated()));
			} else {
				rewrittenBody.add(lit);
			}
		}
		return new BasicRule(rule.getHead(), rewrittenBody);
	}

}
