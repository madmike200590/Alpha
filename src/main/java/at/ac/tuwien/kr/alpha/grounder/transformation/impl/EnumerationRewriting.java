package at.ac.tuwien.kr.alpha.grounder.transformation.impl;

import static at.ac.tuwien.kr.alpha.Util.oops;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.impl.InputProgram;
import at.ac.tuwien.kr.alpha.common.rule.head.impl.DisjunctiveHead;
import at.ac.tuwien.kr.alpha.common.rule.impl.BasicRule;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.parser.InlineDirectives;
import at.ac.tuwien.kr.alpha.grounder.transformation.ProgramTransformation;

/**
 * Rewrites the ordinary atom whose name is given in the input program by the enumeration directive #enum_atom_is into the special EnumerationAtom. Copyright
 * (c) 2017-2019, the Alpha Team.
 */
public class EnumerationRewriting extends ProgramTransformation<InputProgram, InputProgram> {

	@Override
	public InputProgram apply(InputProgram inputProgram) {
		// Read enumeration predicate from directive.
		String enumDirective = inputProgram.getInlineDirectives().getDirectiveValue(InlineDirectives.DIRECTIVE.enum_predicate_is);
		if (enumDirective == null) {
			// Directive not set, nothing to rewrite.
			return inputProgram;
		}
		Predicate enumPredicate = Predicate.getInstance(enumDirective, 3);

		InputProgram.Builder programBuilder = InputProgram.builder().addInlineDirectives(inputProgram.getInlineDirectives());
		programBuilder.addFacts(this.rewriteFacts(inputProgram.getFacts(), enumPredicate));

		List<BasicRule> srcRules = new ArrayList<>(inputProgram.getRules());
		programBuilder.addRules(this.rewriteRules(srcRules, enumPredicate));
		return programBuilder.build();
	}

	private List<Atom> rewriteFacts(List<Atom> srcFacts, Predicate enumPredicate) {
		LinkedList<Atom> rewrittenFacts = new LinkedList<>();
		List<Atom> untouchedFacts = new ArrayList<>(srcFacts);
		Iterator<Atom> it = untouchedFacts.iterator();
		while (it.hasNext()) {
			Atom atom = it.next();
			if (!atom.getPredicate().equals(enumPredicate)) {
				continue;
			}
			it.remove();
			rewrittenFacts.add(new BasicAtom(EnumerationAtom.ENUMERATION_PREDICATE, atom.getTerms()));
		}
		// now add all facts that weren't touched to the rewritten ones to get a list of all facts in program
		rewrittenFacts.addAll(untouchedFacts);
		return rewrittenFacts;
	}

	private List<BasicRule> rewriteRules(List<BasicRule> srcRules, Predicate enumPredicate) {
		List<Literal> tmpLiterals;
		List<BasicRule> rewrittenRules = new ArrayList<>();
		for (BasicRule rule : srcRules) {
			if (rule.getHead() != null && !rule.getHead().isNormal()) {
				throw oops("Encountered rule whose head is not normal: " + rule);
			}
			if (rule.getHead() != null && ((DisjunctiveHead) rule.getHead()).disjunctiveAtoms.get(0).getPredicate().equals(enumPredicate)) {
				throw oops("Atom declared as enumeration atom by directive occurs in head of the rule: " + rule);
			}
			tmpLiterals = new ArrayList<>(rule.getBody());
			Iterator<Literal> rit = tmpLiterals.iterator();
			LinkedList<Literal> rewrittenLiterals = new LinkedList<>();
			while (rit.hasNext()) {
				Literal literal = rit.next();
				if (!(literal instanceof BasicLiteral)) {
					continue;
				}
				BasicLiteral basicLiteral = (BasicLiteral) literal;
				if (!basicLiteral.getPredicate().equals(enumPredicate)) {
					continue;
				}
				rit.remove();
				rewrittenLiterals.add(new EnumerationAtom(basicLiteral.getAtom().getTerms()).toLiteral());
			}
			tmpLiterals.addAll(rewrittenLiterals);
			rewrittenRules.add(new BasicRule(rule.getHead(), tmpLiterals));
		}
		return rewrittenRules;
	}

}
