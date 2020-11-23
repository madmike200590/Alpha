package at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.encoders;

import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateElement;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateAtom.AggregateFunctionSymbol;
import at.ac.tuwien.kr.alpha.common.atoms.AggregateLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.program.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.BasicRule;
import at.ac.tuwien.kr.alpha.common.rule.head.NormalHead;
import at.ac.tuwien.kr.alpha.common.terms.FunctionTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.transformation.PredicateInternalizer;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewritingContext;
import at.ac.tuwien.kr.alpha.grounder.transformation.aggregates.AggregateRewritingContext.AggregateInfo;

/**
 * Abstract base class for aggregate encoders. An aggregate encoder provides an encoding for a given aggregate literal,
 * i.e. it creates an ASP program that is semantically equivalent to the given literal.
 * 
 * Copyright (c) 2020, the Alpha Team.
 */
public abstract class AbstractAggregateEncoder {

	protected static final String ELEMENT_TUPLE_FUNCTION_SYMBOL = "tuple";

	private final AggregateFunctionSymbol aggregateFunctionToEncode;
	private final Set<ComparisonOperator> acceptedOperators;

	protected AbstractAggregateEncoder(AggregateFunctionSymbol aggregateFunctionToEncode, Set<ComparisonOperator> acceptedOperators) {
		this.aggregateFunctionToEncode = aggregateFunctionToEncode;
		this.acceptedOperators = acceptedOperators;
	}

	/**
	 * Encodes all aggregate literals in the given {@link AggregateRewritingContext} referenced by the given Ids.
	 * 
	 * @param ctx
	 * @param aggregateIdsToEncode
	 * @return
	 */
	public InputProgram encodeAggregateLiterals(AggregateRewritingContext ctx, Set<String> aggregateIdsToEncode) {
		InputProgram.Builder programBuilder = InputProgram.builder();
		for (String aggregateId : aggregateIdsToEncode) {
			programBuilder.accumulate(encodeAggregateLiteral(ctx.getAggregateInfo(aggregateId), ctx));
		}
		return programBuilder.build();
	}

	/**
	 * Encodes the aggregate literal referenced by the given {@link AggregateInfo}.
	 * 
	 * @param aggregateToEncode
	 * @param ctx
	 * @return
	 */
	public InputProgram encodeAggregateLiteral(AggregateInfo aggregateToEncode, AggregateRewritingContext ctx) {
		AggregateLiteral literalToEncode = aggregateToEncode.getLiteral();
		if (literalToEncode.getAtom().getAggregatefunction() != this.aggregateFunctionToEncode) {
			throw new IllegalArgumentException(
					"Encoder " + this.getClass().getSimpleName() + " cannot encode aggregate function " + literalToEncode.getAtom().getAggregatefunction());
		}
		if (!this.acceptedOperators.contains(literalToEncode.getAtom().getLowerBoundOperator())) {
			throw new IllegalArgumentException("Encoder " + this.getClass().getSimpleName() + " cannot encode aggregate function "
					+ literalToEncode.getAtom().getAggregatefunction() + " with operator " + literalToEncode.getAtom().getLowerBoundOperator());
		}
		String aggregateId = aggregateToEncode.getId();
		InputProgram literalEncoding = PredicateInternalizer.makePrefixedPredicatesInternal(encodeAggregateResult(aggregateToEncode, ctx), aggregateId);
		List<BasicRule> elementEncodingRules = new ArrayList<>();
		for (AggregateElement elementToEncode : literalToEncode.getAtom().getAggregateElements()) {
			BasicRule elementRule = encodeAggregateElement(aggregateId, elementToEncode, ctx);
			elementEncodingRules.add(PredicateInternalizer.makePrefixedPredicatesInternal(elementRule, aggregateId));
		}
		return new InputProgram(ListUtils.union(literalEncoding.getRules(), elementEncodingRules), literalEncoding.getFacts(), new InlineDirectives());
	}

	/**
	 * Encodes the "core" logic of an aggregate literal, i.e. rules that work on element tuples. Element tuples are derived
	 * by each aggregate element (see {@link AbstractAggregateEncoder#encodeAggregateElement}) and represent the values that
	 * are being aggregated.
	 * 
	 * @param aggregateToEncode
	 * @param ctx
	 * @return
	 */
	protected abstract InputProgram encodeAggregateResult(AggregateInfo aggregateToEncode, AggregateRewritingContext ctx);

	protected BasicRule encodeAggregateElement(String aggregateId, AggregateElement element, AggregateRewritingContext ctx) {
		Atom headAtom = buildElementRuleHead(aggregateId, element, ctx);
		return new BasicRule(new NormalHead(headAtom),
				ListUtils.union(element.getElementLiterals(), new ArrayList<>(ctx.getDependencies(aggregateId))));
	}

	/**
	 * Builds a the head atom for an aggregate element encoding rule of form
	 * <code>HEAD :- $element_literals$, $aggregate_dependencies$</code>, e.g.
	 * <code>count_1_element_tuple(count_1_args(Y), X) :- p(X, Y), q(Y)</code> for the rule body
	 * <code>N = #count{X : p(X, Y)}, q(Y)</code>.
	 * 
	 * @param aggregateId
	 * @param element
	 * @param ctx
	 * @return
	 */
	protected Atom buildElementRuleHead(String aggregateId, AggregateElement element, AggregateRewritingContext ctx) {
		Predicate headPredicate = Predicate.getInstance(this.getElementTuplePredicateSymbol(aggregateId), 2);
		AggregateInfo aggregate = ctx.getAggregateInfo(aggregateId);
		Term aggregateArguments = aggregate.getAggregateArguments();
		FunctionTerm elementTuple = FunctionTerm.getInstance(ELEMENT_TUPLE_FUNCTION_SYMBOL, element.getElementTerms());
		return new BasicAtom(headPredicate, aggregateArguments, elementTuple);
	}

	protected String getElementTuplePredicateSymbol(String aggregateId) {
		return aggregateId + "_element_tuple";
	}

	public AggregateFunctionSymbol getAggregateFunctionToEncode() {
		return this.aggregateFunctionToEncode;
	}

	public Set<ComparisonOperator> getAcceptedOperators() {
		return this.acceptedOperators;
	}

}
