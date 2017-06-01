package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;

/**
 * try tac
 * try [tac_1 | ... | tac_k]
 */
public class TryNode extends TacticNode {
	private TacticParallelNode tactics;
	
	/**
	 * Default constructor
	 */
	public TryNode(TacticParallelNode tactics) {
		assert (tactics != null);
		this.tactics = tactics;
	}
	
	@Override
	protected String getString() {
		return "try " + tactics;
	}

	@Override
	protected void translate(StringBuffer buffer) {
		buffer.append('(');
		
		buffer.append("TRY (");
		if (tactics.size() > 1) {
			buffer.append("FIRST ");
		}
		
		// If tactics.size() > 1 then square brackets will be added automatically
		tactics.translate(buffer);

		buffer.append(')');
		buffer.append(')');		
	}
	
}
