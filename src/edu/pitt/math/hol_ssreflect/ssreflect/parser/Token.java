package edu.pitt.math.hol_ssreflect.ssreflect.parser;


/**
 * Token
 * @author Alexey
 */
class Token {
	// Type
	public final TokenType type;
	
	// Value
	public final String value;
	
	// Integer value
	public final int intValue;
	
	// Describe the position of the token in the input stream
	// (ch is the absolute position)
	public final int ch, line, col;

	/**
	 * Default constructor
	 */
	public Token(TokenType type, String value, int ch, int line, int col) {
		this.type = type;
		
		if (value != null)
			value = value.intern();
		this.value = value;
		
		this.ch = ch;
		this.line = line;
		this.col = col;
		
		if (type == TokenType.INTEGER) {
			intValue = Integer.parseInt(value);
		}
		else {
			intValue = 0;
		}
	}
	
	public Token(TokenType type, int ch, int line, int col) {
		this(type, null, ch, line, col);
	}
	
	@Override
	public String toString() {
		StringBuffer str = new StringBuffer(type.toString());
		str.append('[');
		if (value != null)
			str.append(value);
		str.append(']');
		str.append("{line = " + line + "; col = " + col + "}");
		
		return str.toString();
	}
}