package edu.pitt.math.hol_ssreflect.core.parser;

import java.io.StringReader;
import java.util.ArrayList;

import edu.pitt.math.hol_ssreflect.ocaml.*;
import edu.pitt.math.hol_ssreflect.core.*;

/**
 * Parses HOL terms and types
 * @author Alexey
 */
public class Parser {
	/**
	 * Creates a term from a raw string description
	 */
	public final static Term parseTerm(String rawString) throws Exception {
		Scanner s = new Scanner(new StringReader(rawString));
		return parseTerm(s);		
	}
	
	
	/**
	 * Creates a HOL type from a raw string description
	 */
	public final static HOLType parseHOLType(String rawString) throws Exception {
		Scanner s = new Scanner(new StringReader(rawString));
		return parseHOLType(s);		
	}
	
	
	/**
	 * Creates a Caml object from a raw string
	 */
	public final static CamlObject parse(String rawString) throws Exception {
		Scanner s = new Scanner(new StringReader(rawString));
		return parse(s);
	}
	
	
	/**
	 * Parses a Caml type
	 */
	private static CamlType parseType(Scanner s) throws Exception {
		Token t = s.nextToken();
		
		switch (t.type) {
		case String:
			return CamlType.STRING;
			
		case Int:
			return CamlType.INT;
		
		case Theorem:
			return CamlType.THM;
			
		case Term:
			return CamlType.TERM;
			
		case HOLType:
			return CamlType.HOL_TYPE;
			
		case Goal:
			return CamlType.GOAL;
			
		case Goalstate:
			return CamlType.GOAL_STATE;
			
		case Pair:
			// (
			t = s.nextToken();
			if (t.type != TokenType.LPAR)
				throw new Exception("( expected: " + t);
			
			CamlType t1 = parseType(s);
			
			// ,
			t = s.nextToken();
			if (t.type != TokenType.COMMA)
				throw new Exception(", expected: " + t);
			
			CamlType t2 = parseType(s);
			
			// )
			t = s.nextToken();
			if (t.type != TokenType.RPAR)
				throw new Exception(") expected: " + t);
			
			return new CamlType.PairType(t1, t2);
			
		case List:
			// (
			t = s.nextToken();
			if (t.type != TokenType.LPAR)
				throw new Exception("( expected: " + t);
			
			CamlType el = parseType(s);
			
			// )
			t = s.nextToken();
			if (t.type != TokenType.RPAR)
				throw new Exception(") expected: " + t);
			
			return new CamlType.ListType(el);		
		}
		
		throw new Exception("Unexpected token: " + t);
	}
	
	
	/**
	 * Parses a Caml object
	 */
	private static CamlObject parse(Scanner s) throws Exception {
		Token t = s.peekToken();
		
		switch (t.type) {
		// String
		case STRING:
			s.nextToken();
			return new CamlString(t.value);
		
		// Boolean: false
		case False:
			s.nextToken();
			return new CamlBool(false);
			
		// Boolean: true
		case True:
			s.nextToken();
			return new CamlBool(true);
			
		// Integer
		case INTEGER:
			s.nextToken();
			return new CamlInt(Integer.parseInt(t.value));
		
		// List
		case List:
			return parseList(s);
			
		// Theorem
		case Theorem:
			return parseTheorem(s);

		// Pair
		case Pair:
			return parsePair(s);

		// Term
		case Var:
		case Const:
		case Comb:
		case Abs:
			return parseTerm(s);
			
		// HOLType
		case Tyapp:
		case Tyvar:
			return parseHOLType(s);
			
		case Goal:
			return parseGoal(s);
			
		case Goalstate:
			return parseGoalstate(s);
		}
		
		throw new Exception("Unexpected token: " + t);
	}
	
	
	/**
	 * Parses a goal
	 */
	private final static Goal parseGoal(Scanner s) throws Exception {
		// Goal
		Token t = s.nextToken();
		if (t.type != TokenType.Goal)
			throw new Exception("Goal expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		CamlList assumptions = parseList(s);
		
		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception(", expected: " + t);

		Term goalTerm = parseTerm(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		// Convert the CamlList into a Java list 
		ArrayList<Pair<String, Theorem>> assumptionList = new ArrayList<Pair<String,Theorem>>();
		
//		CamlFunction ASSUME = new CamlFunction("ASSUME", CamlType.mk_function(CamlType.TERM, CamlType.THM));
		
		for (int i = 0; i < assumptions.size(); i++) {
			CamlPair p = (CamlPair) assumptions.get(i);
			CamlString name = (CamlString) p.first();
			Theorem.TempTheorem th = (Theorem.TempTheorem) p.second();
			
			Theorem.AssumptionTheorem ath = new Theorem.AssumptionTheorem(th.concl(), true, name.str);

//			CamlObject.CamlApplication assumption = (CamlObject.CamlApplication) ASSUME.apply(th.concl());
//			th.setCommand(assumption);
			
			assumptionList.add(new Pair<String, Theorem>(name.str, ath));
		}
		
		return new Goal(assumptionList, goalTerm);
	}
	
	

	/**
	 * Parses a goal state
	 */
	private final static Goalstate parseGoalstate(Scanner s) throws Exception {
		// Goalstate
		Token t = s.nextToken();
		if (t.type != TokenType.Goalstate)
			throw new Exception("Goalstate expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		CamlList goals = parseList(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		// Convert the CamlList into a Java list 
		ArrayList<Goal> goalList = new ArrayList<Goal>();
		
		for (int i = 0; i < goals.size(); i++) {
			Goal g = (Goal) goals.get(i);
			goalList.add(g);
		}
		
		return new Goalstate(goalList);
	}
	
	
	
	/**
	 * Parses a Caml pair
	 */
	private final static CamlPair parsePair(Scanner s) throws Exception {
		// Pair
		Token t = s.nextToken();
		if (t.type != TokenType.Pair)
			throw new Exception("Pair expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		CamlObject a = parse(s);
		
		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception(", expected: " + t);
		
		CamlObject b = parse(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return new CamlPair(a, b);
	}
	
	
	
	/**
	 * Parses a Caml list
	 */
	private final static CamlList parseList(Scanner s) throws Exception {
		// List
		Token t = s.nextToken();
		if (t.type != TokenType.List)
			throw new Exception("List expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);

		CamlType type = parseType(s);

		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception(", expected: " + t);
		
		// [
		t = s.nextToken();
		if (t.type != TokenType.LBRACK)
			throw new Exception("[ expected: " + t);

		// Parse elements
		ArrayList<CamlObject> list = new ArrayList<CamlObject>();
		t = s.peekToken();

		while (true) {
			if (t.type == TokenType.RBRACK) {
				// ]
				s.nextToken();
				break;
			}
			
			CamlObject obj = parse(s);
			// The type will be checked when the list is created
			list.add(obj);
			
			// ; or ]
			t = s.nextToken();
			if (t.type == TokenType.RBRACK)
				break;
			
			if (t.type != TokenType.SEMICOLON)
				throw new Exception("; expected: " + t);
		}
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return new CamlList(type, list);
	}
	
	
	/**
	 * Parses a theorem
	 */
	private final static Theorem parseTheorem(Scanner s) throws Exception {
		// Theorem
		Token t = s.nextToken();
		if (t.type != TokenType.Theorem)
			throw new Exception("Theorem expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);

		// Parse assumptions
		CamlList hyp = parseList(s);
		
		
		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception(", expected: " + t);
		
		Term concl = parseTerm(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return new Theorem.TempTheorem(concl, hyp.size() > 0);
	}
	
	
	/**
	 * Parses a HOL type
	 */
	private static HOLType parseHOLType(Scanner s) throws Exception {
		Token t = s.peekToken();
		
		switch (t.type) {
		case Tyapp:
			return parseTyapp(s);
		case Tyvar:
			return parseTyvar(s);
		}

		throw new Exception("Unexpected token: " + t);
	}
	
	
	/**
	 * Parses a term
	 */
	private static Term parseTerm(Scanner s) throws Exception {
		Token t = s.peekToken();
		
		switch (t.type) {
		case Var:
			return parseVar(s);
		case Const:
			return parseConst(s);
		case Comb:
			return parseComb(s);
		case Abs:
			return parseAbs(s);
		}
		
		throw new Exception("Unexpected token: " + t);
	}
	
	

	/**
	 * Parses a type variable
	 */
	protected static HOLType parseTyvar(Scanner s) throws Exception {
		// Tyvar
		Token t = s.nextToken();
		
		if (t.type != TokenType.Tyvar)
			throw new Exception("Tyvar expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		// name
		t = s.nextToken();
		if (t.type != TokenType.STRING)
			throw new Exception("STRING expected: " + t);
		
		HOLType result = HOLType.mk_vartype(t.value);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return result;
	}
	
	
	/**
	 * Parses a HOL type application
	 */
	protected static HOLType parseTyapp(Scanner s) throws Exception {
		// Tyapp
		Token t = s.nextToken();
		
		if (t.type != TokenType.Tyapp)
			throw new Exception("Tyapp expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		// name
		t = s.nextToken();
		if (t.type != TokenType.STRING)
			throw new Exception("STRING expected: " + t);
		
		String name = t.value;
		ArrayList<HOLType> args = new ArrayList<HOLType>();
		
		// [
		t = s.nextToken();
		if (t.type != TokenType.LBRACK)
			throw new Exception("[ expected: " + t);
		
		t = s.peekToken();
		if (t.type == TokenType.RBRACK) {
			// ]
			s.nextToken();
		}
		else {
			while (true) {
				HOLType arg = parseHOLType(s);
				args.add(arg);
				
				t = s.nextToken();
				if (t.type == TokenType.RBRACK)
					break;
				
				if (t.type != TokenType.COMMA)
					throw new Exception(", expected: " + t);
			}
		}
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return HOLType.mk_type(name, args);
	}
	
	
	/**
	 * Parses a variable term
	 */
	protected static Term parseVar(Scanner s) throws Exception {
		// Var
		Token t = s.nextToken();
		
		if (t.type != TokenType.Var)
			throw new Exception("Var expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		// name
		t = s.nextToken();
		if (t.type != TokenType.STRING)
			throw new Exception("STRING expected: " + t);
		
		String name = t.value;

		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception("COMMA expected: " + t);
		
		HOLType type = parseHOLType(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return Term.mk_var(name, type);
	}
	
	/**
	 * Parses a constant term
	 */
	protected static Term parseConst(Scanner s) throws Exception {
		// Const
		Token t = s.nextToken();
		
		if (t.type != TokenType.Const)
			throw new Exception("Const expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);
		
		// name
		t = s.nextToken();
		if (t.type != TokenType.STRING)
			throw new Exception("STRING expected: " + t);
		
		String name = t.value;

		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception("COMMA expected: " + t);
		
		HOLType type = parseHOLType(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return Term.mk_mconst(name, type);
	}
	

	/**
	 * Parses a combinator term
	 */
	protected static Term parseComb(Scanner s) throws Exception {
		// Comb
		Token t = s.nextToken();
		
		if (t.type != TokenType.Comb)
			throw new Exception("Comb expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);

		// f
		Term f = parseTerm(s);

		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception("COMMA expected: " + t);
		
		// a
		Term a = parseTerm(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return Term.mk_comb(f, a);
	}
	
	
	/**
	 * Parses an abstraction term
	 */
	protected static Term parseAbs(Scanner s) throws Exception {
		// Abs
		Token t = s.nextToken();
		
		if (t.type != TokenType.Abs)
			throw new Exception("Abs expected: " + t);
		
		// (
		t = s.nextToken();
		if (t.type != TokenType.LPAR)
			throw new Exception("( expected: " + t);

		// v
		Term v = parseTerm(s);

		// ,
		t = s.nextToken();
		if (t.type != TokenType.COMMA)
			throw new Exception("COMMA expected: " + t);
		
		// b
		Term b = parseTerm(s);
		
		// )
		t = s.nextToken();
		if (t.type != TokenType.RPAR)
			throw new Exception(") expected: " + t);
		
		return Term.mk_abs(v, b);
	}
	
}
