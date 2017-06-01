package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;

/**
 * _
 */
public class WildObjectNode extends ObjectNode {
	/**
	 * Default constructor
	 */
	public WildObjectNode() {
	}
	
	@Override
	protected String getString() {
		return "_";
	}

	@Override
	protected int getType() {
		return UNKNOWN;
	}

	@Override
	protected void translate(StringBuffer buffer) {
		throw new RuntimeException("wildcard.translate(): unimplemented");
		
	}

	@Override
	protected boolean isWildCard() {
		return true;
	}

}
