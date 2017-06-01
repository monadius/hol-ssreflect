package edu.pitt.math.hol_ssreflect.core.parser;


/**
 * Token
 * @author Alexey
 */
class Token {
	// Type
	public final TokenType type;
	
	// Value
	public final String value;

	/**
	 * Default constructor
	 */
	public Token(TokenType type, String value) {
		this.type = type;
		this.value = value;
	}
	
	public Token(TokenType type) {
		this(type, null);
	}
	
	@Override
	public String toString() {
		String str = type.toString();
		str += '[';
		if (value != null)
			str += value;
		str += ']';
		
		return str;
	}
}