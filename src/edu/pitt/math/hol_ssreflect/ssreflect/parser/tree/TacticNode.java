package edu.pitt.math.hol_ssreflect.ssreflect.parser.tree;

/**
 * A node for a tactic
 */
public abstract class TacticNode extends Node {
	@Override
	public String getRevertCommand() {
		return "Sections.revert_proof_step()";
	}
}
