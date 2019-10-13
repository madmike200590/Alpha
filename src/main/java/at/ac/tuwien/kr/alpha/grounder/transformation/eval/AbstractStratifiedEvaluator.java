package at.ac.tuwien.kr.alpha.grounder.transformation.eval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.depgraph.ComponentGraph;
import at.ac.tuwien.kr.alpha.common.depgraph.ComponentGraph.SCComponent;
import at.ac.tuwien.kr.alpha.common.depgraph.Node;
import at.ac.tuwien.kr.alpha.common.depgraph.StratificationHelper;
import at.ac.tuwien.kr.alpha.common.program.impl.AnalyzedProgram;
import at.ac.tuwien.kr.alpha.common.program.impl.InternalProgram;
import at.ac.tuwien.kr.alpha.common.rule.impl.InternalRule;
import at.ac.tuwien.kr.alpha.grounder.transformation.ProgramTransformation;

public abstract class AbstractStratifiedEvaluator extends ProgramTransformation<AnalyzedProgram, InternalProgram> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStratifiedEvaluator.class);

	private StratificationHelper stratificationHelper = new StratificationHelper();
	private Map<Predicate, HashSet<InternalRule>> predicateDefiningRules;

	private Set<Integer> solvedRuleIds = new HashSet<>();

	@Override
	public InternalProgram apply(AnalyzedProgram inputProgram) {
		// first, we calculate a component graph and stratification
		ComponentGraph componentGraph = inputProgram.getComponentGraph();
		Map<Integer, List<SCComponent>> strata = this.stratificationHelper.calculateStratification(componentGraph);
		this.predicateDefiningRules = inputProgram.getPredicateDefiningRules();

		this.initializeWorkingMemory(inputProgram);

		ComponentEvaluationOrder evaluationOrder = new ComponentEvaluationOrder(strata);
		for (SCComponent currComponent : evaluationOrder) {
			this.evaluateComponent(currComponent);
		}

		// build the resulting program
		List<Atom> outputFacts = this.buildOutputFacts();
		List<InternalRule> outputRules = new ArrayList<>();
		inputProgram.getRulesById().entrySet().stream().filter((entry) -> !this.solvedRuleIds.contains(entry.getKey()))
				.forEach((entry) -> outputRules.add(entry.getValue()));
		InternalProgram retVal = new InternalProgram(outputRules, outputFacts);
		return retVal;
	}

	protected abstract void initializeWorkingMemory(AnalyzedProgram program);

	protected abstract List<Atom> buildOutputFacts();

	private void evaluateComponent(SCComponent comp) {
		LOGGER.debug("Evaluating component {}", comp);
		Set<InternalRule> rulesToEvaluate = this.getRulesToEvaluate(comp);
		if (rulesToEvaluate.isEmpty()) {
			LOGGER.debug("No rules to evaluate for component {}", comp);
			return;
		}
		int newInstances;
		do {
			newInstances = this.evaluateRules(rulesToEvaluate);
		} while (newInstances != 0); // if evaluation of rules doesn't modify the working memory we have a fixed point
		LOGGER.debug("Evaluation done - reached a fixed point on component {}", comp);
		rulesToEvaluate.forEach((rule) -> this.solvedRuleIds.add(rule.getRuleId()));
		LOGGER.debug("Finished adding program facts");
	}

	protected abstract int evaluateRules(Set<InternalRule> rules);

	protected abstract int evaluateRule(InternalRule rule);

	private Set<InternalRule> getRulesToEvaluate(SCComponent comp) {
		Set<InternalRule> retVal = new HashSet<>();
		HashSet<InternalRule> tmpPredicateRules;
		for (Node node : comp.getNodes()) {
			tmpPredicateRules = this.predicateDefiningRules.get(node.getPredicate());
			if (tmpPredicateRules != null) {
				retVal.addAll(tmpPredicateRules);
			}
		}
		return retVal;
	}

	private class ComponentEvaluationOrder implements Iterable<SCComponent> {

		private Map<Integer, List<SCComponent>> stratification;
		private Iterator<Entry<Integer, List<SCComponent>>> strataIterator;
		private Iterator<SCComponent> componentIterator;

		private ComponentEvaluationOrder(Map<Integer, List<SCComponent>> stratification) {
			this.stratification = stratification;
			this.strataIterator = this.stratification.entrySet().iterator();
			this.startNextStratum();
		}

		private boolean startNextStratum() {
			if (!this.strataIterator.hasNext()) {
				return false;
			}
			this.componentIterator = this.strataIterator.next().getValue().iterator();
			return true;
		}

		@Override
		public Iterator<SCComponent> iterator() {
			return new Iterator<SCComponent>() {

				@Override
				public boolean hasNext() {
					if (ComponentEvaluationOrder.this.componentIterator == null) {
						// can happen when there are actually no components, as is the case for empty programs or programs just consisting of facts
						return false;
					}
					if (ComponentEvaluationOrder.this.componentIterator.hasNext()) {
						return true;
					} else {
						if (!ComponentEvaluationOrder.this.startNextStratum()) {
							return false;
						} else {
							return this.hasNext();
						}
					}
				}

				@Override
				public SCComponent next() {
					return ComponentEvaluationOrder.this.componentIterator.next();
				}

			};
		}
	}
}
