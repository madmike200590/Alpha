package at.ac.tuwien.kr.alpha.grounder.parser;

import java.util.*;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class ParsedProgram extends CommonParsedObject {
	public static final ParsedProgram EMPTY = new ParsedProgram(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

	public List<ParsedRule> rules;
	public List<ParsedFact> facts;
	public List<ParsedConstraint> constraints;

	private ParsedProgram(List<ParsedRule> rules, List<ParsedFact> facts, List<ParsedConstraint> constraints) {
		this.rules = rules;
		this.facts = facts;
		this.constraints = constraints;
	}

	public ParsedProgram(Collection<? extends CommonParsedObject> objects) {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

		objects.forEach(o -> o.addTo(this));
	}

	public boolean addRule(ParsedRule rule) {
		return rules.add(rule);
	}

	public boolean addFact(ParsedFact fact) {
		return facts.add(fact);
	}

	public boolean addConstraint(ParsedConstraint constraint) {
		return constraints.add(constraint);
	}

	public void accumulate(ParsedProgram program) {
		rules.addAll(program.rules);
		facts.addAll(program.facts);
		constraints.addAll(program.constraints);
	}
}
