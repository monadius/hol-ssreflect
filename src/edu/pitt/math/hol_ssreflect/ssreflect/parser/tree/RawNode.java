package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;

/**
 * Purely raw command
 */
public class RawNode extends Node {
	// Raw text
	private final String rawText;
	
	/**
	 * Default constructor 
	 */
	public RawNode(String rawText) {
		assert(rawText != null);
		this.rawText = rawText;
	}

	@Override
	protected String getString() {
		return "{" + rawText + "}";
	}

	@Override
	protected void translate(StringBuffer buffer) {
		buffer.append(rawText);
	}

	@Override
	public String getRevertCommand() {
		return null;
	}
	
}
