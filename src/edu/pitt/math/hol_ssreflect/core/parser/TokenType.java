package edu.pitt.math.hol_ssreflect.core.parser;

/**
 * Token type
 */
enum TokenType {
	EOF,
	STRING, IDENTIFIER, INTEGER,
	LPAR, RPAR,
	LBRACK, RBRACK,
	COMMA, COLON, SEMICOLON,
	False, True,
	Tyapp, Tyvar,
	Var, Const, Comb, Abs,
	String, Int, HOLType, Term, Theorem, List, Pair,
	Goal, Goalstate
}
