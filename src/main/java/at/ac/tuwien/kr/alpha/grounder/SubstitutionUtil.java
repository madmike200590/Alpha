package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

/**
 * This class provides functions for first-order variable substitution.
 * Copyright (c) 2016, the Alpha Team.
 */
public class SubstitutionUtil {

	/**
	 * This method applies a substitution to an atom and returns the AtomId of the ground atom.
	 * @param variableSubstitution the variable substitution to apply.
	 * @return the AtomId of the corresponding substituted ground atom.
	 */
	public static AtomId groundingSubstitute(AtomStore atomStore, PredicateInstance nonGroundAtom, NaiveGrounder.VariableSubstitution variableSubstitution) {
		Term[] groundTermList = new Term[nonGroundAtom.termList.length];
		for (int i = 0; i < nonGroundAtom.termList.length; i++) {
			Term nonGroundTerm = nonGroundAtom.termList[i];
			Term groundTerm = groundTerm(nonGroundTerm, variableSubstitution);
			if (!groundTerm.isGround()) {
				throw new RuntimeException("Grounding substitution yields a non-ground term: " + groundTerm + "\nShould not happen, aborting.");
			}
			groundTermList[i] = groundTerm;
		}
		PredicateInstance groundAtom = new PredicateInstance(nonGroundAtom.predicate, groundTermList);
		return atomStore.createAtomId(groundAtom);
	}

	/**
	 * Applies a substitution, result may be nonground.
	 * @param nonGroundTerm the (potentially) non-ground term to apply the substitution to.
	 * @param variableSubstitution the variable substitution to apply.
	 * @return the non-ground term where all variable substitutions have been applied.
	 */
	public static Term groundTerm(Term nonGroundTerm, NaiveGrounder.VariableSubstitution variableSubstitution) {
		if (nonGroundTerm instanceof ConstantTerm) {
			return nonGroundTerm;
		} else if (nonGroundTerm instanceof VariableTerm) {
			Term groundTerm = variableSubstitution.substitution.get(nonGroundTerm);
			if (groundTerm == null) {
				return nonGroundTerm;	// If variable is not substituted, keep term as is.
				//throw new RuntimeException("SubstitutionUtil encountered variable without a substitution given: " + nonGroundTerm);
			}
			return  groundTerm;
		} else if (nonGroundTerm instanceof FunctionTerm) {
			ArrayList<Term> groundTermList = new ArrayList<>(((FunctionTerm) nonGroundTerm).termList.size());
			for (Term term : ((FunctionTerm) nonGroundTerm).termList) {
				groundTermList.add(groundTerm(term, variableSubstitution));
			}
			return FunctionTerm.getFunctionTerm(((FunctionTerm) nonGroundTerm).functionSymbol, groundTermList);
		} else {
			throw new RuntimeException("SubstitutionUtil: Unknown term type encountered.");
		}
	}

	/**
	 * This method applies a substitution to a potentially non-ground atom. The resulting atom may be non-ground.
	 * @param nonGroundAtom the (non-ground) atom to apply the substitution to (parameter is not  modified).
	 * @param variableSubstitution the variable substitution to apply.
	 * @return a pair <Boolean>isGround</Boolean>, <PredicateInstance>substitutedAtom</PredicateInstance> where
	 * isGround true if the substitutedAtom is ground and substitutedAtom is the atom resulting from the applying
	 * the substitution.
	 */
	public static Pair<Boolean, PredicateInstance> substitute(PredicateInstance nonGroundAtom, NaiveGrounder.VariableSubstitution variableSubstitution) {
		Term[] substitutedTerms = new Term[nonGroundAtom.termList.length];
		boolean isGround = true;
		for (int i = 0; i < nonGroundAtom.termList.length; i++) {
			substitutedTerms[i] = groundTerm(nonGroundAtom.termList[i], variableSubstitution);
			if (isGround && !substitutedTerms[i].isGround()) {
				isGround = false;
			}
		}
		PredicateInstance substitutedAtom = new PredicateInstance(nonGroundAtom.predicate, substitutedTerms);
		return new ImmutablePair<>(isGround, substitutedAtom);
	}

	public static String groundAndPrintRule(NonGroundRule rule, NaiveGrounder.VariableSubstitution substitution) {
		String ret = "";
		if (!rule.isConstraint()) {
			PredicateInstance groundHead = substitute(rule.headAtom, substitution).getRight();
			ret += groundHead.toString();
		}
		ret += " :- ";
		boolean isFirst = true;
		for (PredicateInstance bodyAtom : rule.bodyAtomsPositive) {
			if (!isFirst) {
				ret += ", ";
			}
			PredicateInstance groundBodyAtom = substitute(bodyAtom, substitution).getRight();
			ret += groundBodyAtom.toString();
			isFirst = false;
		}
		for (PredicateInstance bodyAtom : rule.bodyAtomsNegative) {
			if (!isFirst) {
				ret += ", ";
			}
			PredicateInstance groundBodyAtom = substitute(bodyAtom, substitution).getRight();
			ret += "not " + groundBodyAtom.toString();
			isFirst = false;
		}
		ret += ".";
		return ret;
	}
}
