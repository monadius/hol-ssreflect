package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;


/**
 * move
 */
public class MoveNode extends TacticNode {
	/**
	 * Default constructor
	 */
	public MoveNode() {
	}
	
	@Override
	protected String getString() {
		return "move";
	}

	@Override
	protected void translate(StringBuffer buffer) {
		// Beta normalization
		buffer.append("BETA_TAC");
	}
	
}
