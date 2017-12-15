package at.ac.tuwien.kr.alpha.grounder.transformation;

import at.ac.tuwien.kr.alpha.common.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.Program;
import at.ac.tuwien.kr.alpha.common.Rule;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BodyElement;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.*;

import static at.ac.tuwien.kr.alpha.Util.oops;

/**
 * Removes variable equalities from rules by replacing one variable with the other.
 * Copyright (c) 2017, the Alpha Team.
 */
public class VariableEqualityRemoval implements ProgramTransformation {
	@Override
	public void transform(Program inputProgram) {
		for (Rule rule : inputProgram.getRules()) {
			findAndReplaceVariableEquality(rule);
		}
	}

	private void findAndReplaceVariableEquality(Rule rule) {
		// Collect all equal variables.
		HashMap<VariableTerm, HashSet<VariableTerm>> variableToEqualVariables = new HashMap<>();
		//HashSet<Variable> equalVariables = new LinkedHashSet<>();
		HashSet<BodyElement> equalitiesToRemove = new HashSet<>();
		for (BodyElement bodyElement : rule.getBody()) {
			if (!(bodyElement instanceof ComparisonAtom)) {
				continue;
			}
			ComparisonAtom comparisonAtom = (ComparisonAtom) bodyElement;
			if (!comparisonAtom.isNormalizedEquality()) {
				continue;
			}
			if (comparisonAtom.getTerms().get(0) instanceof VariableTerm && comparisonAtom.getTerms().get(1) instanceof VariableTerm) {
				VariableTerm leftVariable = (VariableTerm) comparisonAtom.getTerms().get(0);
				VariableTerm rightVariable = (VariableTerm) comparisonAtom.getTerms().get(1);
				HashSet<VariableTerm> leftEqualVariables = variableToEqualVariables.get(leftVariable);
				HashSet<VariableTerm> rightEqualVariables = variableToEqualVariables.get(rightVariable);
				if (leftEqualVariables == null && rightEqualVariables == null) {
					HashSet<VariableTerm> equalVariables = new LinkedHashSet<>(Arrays.asList(leftVariable, rightVariable));
					variableToEqualVariables.put(leftVariable, equalVariables);
					variableToEqualVariables.put(rightVariable, equalVariables);
				}
				if (leftEqualVariables == null && rightEqualVariables != null) {
					rightEqualVariables.add(leftVariable);
					variableToEqualVariables.put(leftVariable, rightEqualVariables);
				}
				if (leftEqualVariables != null && rightEqualVariables == null) {
					leftEqualVariables.add(rightVariable);
					variableToEqualVariables.put(rightVariable, leftEqualVariables);
				}
				if (leftEqualVariables != null && rightEqualVariables != null) {
					leftEqualVariables.addAll(rightEqualVariables);
					for (VariableTerm rightEqualVariable : rightEqualVariables) {
						variableToEqualVariables.put(rightEqualVariable, leftEqualVariables);
					}
				}
				equalitiesToRemove.add(comparisonAtom);
			}
		}
		if (variableToEqualVariables.isEmpty()) {
			// Skip rule if there is no equality between variables.
			return;
		}

		// Use substitution for actual replacement.
		Substitution replacementSubstitution = new Substitution();
		// For each set of equal variables, take the first variable and replace all others by it.
		for (Map.Entry<VariableTerm, HashSet<VariableTerm>> variableEqualityEntry : variableToEqualVariables.entrySet()) {
			VariableTerm variableToReplace = variableEqualityEntry.getKey();
			VariableTerm replacementVariable = variableEqualityEntry.getValue().iterator().next();
			if (variableToReplace == replacementVariable) {
				continue;
			}
			replacementSubstitution.put(variableToReplace, replacementVariable);
		}
		// Replace/Substitute in each literal every term where one of the common variables occurs.
		Iterator<BodyElement> bodyIterator = rule.getBody().iterator();
		while (bodyIterator.hasNext()) {
			BodyElement bodyElement = bodyIterator.next();
			if (!(bodyElement instanceof Literal)) {
				throw oops("BodyElement is not a Literal.");
			}
			Literal literal = (Literal) bodyElement;
			if (equalitiesToRemove.contains(literal)) {
				bodyIterator.remove();
			}
			for (int i = 0; i < literal.getTerms().size(); i++) {
				Term replaced = literal.getTerms().get(i).substitute(replacementSubstitution);
				literal.getTerms().set(i, replaced);
			}
		}
		// Replace variables in head.
		if (rule.getHead() != null) {
			if (!rule.getHead().isNormal()) {
				throw new UnsupportedOperationException("Cannot treat non-normal rule heads yet.");
			}
			DisjunctiveHead head = (DisjunctiveHead) rule.getHead();
			Atom headAtom = head.disjunctiveAtoms.get(0);
			for (int i = 0; i < headAtom.getTerms().size(); i++) {
				Term replaced = headAtom.getTerms().get(i).substitute(replacementSubstitution);
				headAtom.getTerms().set(i, replaced);
			}
		}
	}
}
