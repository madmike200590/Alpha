package at.ac.tuwien.kr.alpha.grounder.sql;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tuwien.kr.alpha.Util;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.Atom;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.program.impl.InternalProgram;
import at.ac.tuwien.kr.alpha.common.rule.impl.InternalRule;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.AbstractGrounder;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

/**
 * Experimental grounder for use in stratified evaluation outside of Alpha's main ground/solve loop. Does not extend {@link AbstractGrounder} for now because
 * this implementation is at least initially only intended for stratified evaluation. Performs grounding and evaluation of rules by using an in-memory database.
 * 
 * Copyright (c) 2019, the Alpha Team.
 */
public class DatabaseBackedGrounder implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackedGrounder.class);

	private static final String H2_DRIVER = "org.h2.Driver";

	/**
	 * In-memory database that only supports one connection with isolation turned off (level = read uncommitted). No transaction features needed as the DB is
	 * really just a very fast and fancy working memory for the grounder.
	 */
	private static final String H2DB_URL = "jdbc:h2:mem:;LOCK_MODE=0";

	private static final String INSTANCE_TABLE_TEMPLATE = "CREATE TABLE %s (%s, create_time integer not null)";
	private static final String TERM_COLUMN_TEMPLATE = "%s varchar(255) not null";
	private static final String INSTANCE_TABLE_CREATE_IDX_TEMPLATE = "CREATE INDEX %s_ctime_idx ON %s(create_time)";
	private static final String INSTANCE_TABLE_PRIMARY_KEY_TEMPLATE = "ALTER TABLE %s ADD CONSTRAINT %s_pk PRIMARY KEY(%s)";

	private static final String INSTANCE_INSERT_TEMPLATE = "INSERT INTO %s (%s, create_time) VALUES (%s, ?)";

	private final Map<Predicate, PreparedStatement> instanceInserts = new HashMap<>();
	private final Map<Integer, RuleSqlMapper> ruleGroundingMappers = new HashMap<>();
	private final Map<Integer, PreparedStatement> ruleGroundingSelects = new HashMap<>();

	private InternalProgram program;

	private Connection db;

	public DatabaseBackedGrounder(InternalProgram program) throws SQLException {
		this.program = program;
		this.db = DatabaseBackedGrounder.createDatabase();
		this.prepareTables();
		this.prepareGroundingSelects();
	}

	public List<Substitution> groundRule(InternalRule rule) {
		List<Substitution> retVal;
		RuleSqlMapper mapper = this.ruleGroundingMappers.get(rule.getRuleId());
		PreparedStatement stmt = this.ruleGroundingSelects.get(rule.getRuleId());
		try {
			ResultSet rs = stmt.executeQuery();
			retVal = this.mapFromResultSet(rs, mapper);
			rs.close();
		} catch (SQLException ex) {
			throw Util.oops("DatabaseBackedGrounder failed executing groundingSelect for rule " + rule.toString());
		}
		return retVal;
	}

	private List<Substitution> mapFromResultSet(ResultSet rs, RuleSqlMapper mapper) throws SQLException {
		List<Substitution> retVal = new ArrayList<>();
		Substitution subst;
		while (rs.next()) {
			subst = new Substitution();
			for (VariableTerm var : mapper.variableToColumns.keySet()) {
				subst.put(var, ConstantTerm.getInstance(rs.getString(var.getVariableName())));
			}
			retVal.add(subst);
		}
		return retVal;
	}

	private static Connection createDatabase() throws SQLException {
		try {
			Class.forName(H2_DRIVER);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException("Failed loading H2 JDBC driver!");
		}
		return DriverManager.getConnection(H2DB_URL);
	}

	private void prepareTables() throws SQLException {
		// create instance tables for all predicates in facts
		for (Predicate p : this.program.getFactsByPredicate().keySet()) {
			if (!this.instanceInserts.containsKey(p)) {
				this.createPredicateTable(p);
			}
		}
		// now the same for all predicates occurring in rules
		for (InternalRule rule : this.program.getRules()) {
			for (Predicate p : rule.getOccurringPredicates()) {
				if (!this.instanceInserts.containsKey(p)) {
					this.createPredicateTable(p);
				}
			}
		}
	}

	private void prepareGroundingSelects() throws SQLException {
		RuleSqlMapper mapper;
		for (InternalRule rule : this.program.getRules()) {
			mapper = new RuleSqlMapper(rule);
			this.ruleGroundingMappers.put(rule.getRuleId(), mapper);
			this.ruleGroundingSelects.put(rule.getRuleId(), this.db.prepareStatement(mapper.groundingSelect));
		}
	}

	private void createPredicateTable(Predicate pred) throws SQLException {
		String columnDefs = this.createColumnDefinitions(pred);
		String columnNames = this.createColumnNames(pred);
		String createTableSql = String.format(INSTANCE_TABLE_TEMPLATE, pred.getName(), columnDefs);
		String createPrimaryKeySql = String.format(INSTANCE_TABLE_PRIMARY_KEY_TEMPLATE, pred.getName(), pred.getName(), columnNames);
		String createCTimeIndexSql = String.format(INSTANCE_TABLE_CREATE_IDX_TEMPLATE, pred.getName(), pred.getName());
		Statement stmt = this.db.createStatement();
		stmt.execute(createTableSql);
		stmt.execute(createPrimaryKeySql);
		stmt.execute(createCTimeIndexSql);
		this.instanceInserts.put(pred, this.generateInstanceInsert(pred));
	}

	private PreparedStatement generateInstanceInsert(Predicate pred) throws SQLException {
		String colNames = this.createColumnNames(pred);
		String paramPlaceholders = StringUtils.repeat("?", ", ", pred.getArity());
		String sql = String.format(INSTANCE_INSERT_TEMPLATE, pred.getName(), colNames, paramPlaceholders);
		return this.db.prepareStatement(sql);
	}

	private String createColumnNames(Predicate pred) {
		String[] cols = new String[pred.getArity()];
		for (int i = 0; i < pred.getArity(); i++) {
			cols[i] = "t_" + i;
		}
		return StringUtils.join(cols, ",");
	}

	private String createColumnDefinitions(Predicate pred) {
		String[] cols = new String[pred.getArity()];
		for (int i = 0; i < pred.getArity(); i++) {
			cols[i] = String.format(TERM_COLUMN_TEMPLATE, "t_" + i);
		}
		return StringUtils.join(cols, ",");
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	public Map<Predicate, PreparedStatement> getInstanceInserts() {
		return Collections.unmodifiableMap(this.instanceInserts);
	}

	public Map<Integer, RuleSqlMapper> getRuleGroundingMappers() {
		return Collections.unmodifiableMap(this.ruleGroundingMappers);
	}

	private class RuleSqlMapper {

		private final InternalRule rule;

		private final Map<Atom, String> atomToTableAlias = new HashMap<>();
		private final Map<Atom, Map<Term, ImmutablePair<String, String>>> termToColumnAlias = new HashMap<>();
		private final Map<VariableTerm, List<ImmutablePair<String, String>>> variableToColumns = new HashMap<>();
		private final Map<Predicate, Integer> predicateOccurrences = new HashMap<>();

		private final String groundingSelect;

		private RuleSqlMapper(InternalRule rule) {
			this.rule = rule;
			this.generateMappings();
			this.groundingSelect = this.generateGroundingSelect();
		}

		private String generateGroundingSelect() {
			Set<VariableTerm> selectSet = this.variableToColumns.keySet();
			List<String> fromTables = new ArrayList<>();
			for (Atom a : this.rule.getBodyAtomsPositive()) {
				fromTables.add(a.getPredicate().getName() + " " + this.atomToTableAlias.get(a));
			}
			List<ImmutablePair<String, String>> joins = this.generateJoins();
			return this.renderSelectFor(selectSet, fromTables, joins);
		}

		private String renderSelectFor(Set<VariableTerm> selectSet, List<String> fromTables, List<ImmutablePair<String, String>> joins) {
			StringBuilder bld = new StringBuilder();
			bld.append("select ");
			ImmutablePair<String, String> variableMapping;
			List<String> selectExprs = new ArrayList<>();
			for (VariableTerm var : selectSet) {
				// always use the first mapping for selecting,
				// other mappings are only needed for joins
				variableMapping = this.variableToColumns.get(var).get(0);
				selectExprs.add(variableMapping.left + "." + variableMapping.right + " as " + var.toString());
			}
			bld.append(StringUtils.join(selectExprs, ", ")).append(" from ").append(StringUtils.join(fromTables, ", "));
			if (!joins.isEmpty()) {
				bld.append(" where ");
				List<String> joinExprs = new ArrayList<>();
				for (ImmutablePair<String, String> join : joins) {
					joinExprs.add(join.left + " = " + join.right);
				}
				bld.append(StringUtils.join(joinExprs, " and "));
			}
			return bld.toString();
		}

		private List<ImmutablePair<String, String>> generateJoins() {
			List<ImmutablePair<String, String>> retVal = new ArrayList<>();
			List<ImmutablePair<String, String>> columnMappings;
			for (VariableTerm var : this.variableToColumns.keySet()) {
				columnMappings = this.variableToColumns.get(var);
				if (columnMappings.size() > 1) {
					Iterator<ImmutablePair<String, String>> it = columnMappings.iterator();
					ImmutablePair<String, String> firstMapping = it.next();
					String leftSide = firstMapping.left + "." + firstMapping.right;
					ImmutablePair<String, String> currentMapping;
					while (it.hasNext()) {
						currentMapping = it.next();
						retVal.add(new ImmutablePair<>(leftSide, currentMapping.left + "." + currentMapping.right));
					}
				}
			}
			return retVal;
		}

		private void generateMappings() {
			// map head atom to a table
			Atom head = this.rule.getHeadAtom();
			this.generateMappingsForAtom(head, false);
			// map body atoms to tables
			for (Literal lit : this.rule.getBody()) {
				this.generateMappingsForAtom(lit.getAtom(), true);
			}
		}

		private void generateMappingsForAtom(Atom atom, boolean performVariableMapping) {
			this.registerPredicateOccurrence(atom.getPredicate());
			String tableAlias = this.generateTableAlias(atom.getPredicate());
			this.atomToTableAlias.put(atom, tableAlias);

			HashMap<Term, ImmutablePair<String, String>> termsToColumns = new HashMap<>();
			String column;
			ImmutablePair<String, String> columnMapping;
			int termPosition = 1;
			for (Term t : atom.getTerms()) {
				column = "t_" + termPosition;
				columnMapping = new ImmutablePair<>(tableAlias, column);
				termsToColumns.put(t, columnMapping);
				if (t instanceof VariableTerm && performVariableMapping) {
					// only do the variableMapping for non-head atoms
					this.registerVariableMapping((VariableTerm) t, columnMapping);
				}
				termPosition++;
			}
			this.termToColumnAlias.put(atom, termsToColumns);
		}

		private void registerVariableMapping(VariableTerm var, ImmutablePair<String, String> column) {
			if (!this.variableToColumns.containsKey(var)) {
				this.variableToColumns.put(var, new ArrayList<>());
			}
			this.variableToColumns.get(var).add(column);
		}

		private void registerPredicateOccurrence(Predicate p) {
			Integer previousCount = this.predicateOccurrences.putIfAbsent(p, 1);
			if (previousCount != null) {
				this.predicateOccurrences.put(p, this.predicateOccurrences.get(p) + 1);
			}
		}

		private String generateTableAlias(Predicate p) {
			return p.getName() + this.predicateOccurrences.get(p);
		}

	}

}
