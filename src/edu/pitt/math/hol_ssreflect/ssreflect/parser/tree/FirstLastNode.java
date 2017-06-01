package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;

/**
 * first tactic
 * last tactic
 */
public class FirstLastNode extends LeftAssociativeTacticNode {
	// If true then 'first tactic' else 'last tactic'
	private boolean firstFlag;
	// Tactic
	private TacticNode tactic;
	
	/**
	 * Default constructor
	 */
	public FirstLastNode(boolean firstFlag, TacticNode tactic) {
		assert (tactic != null);
		this.firstFlag = firstFlag;
		this.tactic = tactic;
	}
	
	@Override
	protected String getString() {
		if (firstFlag)
			return "first " + tactic;
		else
			return "last " + tactic;
	}

	@Override
	protected void translate(StringBuffer buffer) {
		// This method should be never called
		throw new RuntimeException("FirstLastNode.translate()");
	}
	
	@Override
	public TacticNode transformTactic(TacticChainNode left) {
		TacticNode tac = new RawTactic(firstFlag ? "THENL_FIRST" : "THENL_LAST");
		return new BinaryNode(false, tac, left, tactic);
	}
	
}
