package edu.pitt.math.hol_ssreflect.test;


import edu.pitt.math.hol_ssreflect.ocaml.CamlEnvironment;
import edu.pitt.math.hol_ssreflect.ocaml.CamlObject;
import edu.pitt.math.hol_ssreflect.ocaml.CamlType;
import edu.pitt.math.hol_ssreflect.core.HOLType;
import edu.pitt.math.hol_ssreflect.core.Term;

/**
 * A Caml environment for test purposes
 * @author Alexey
 *
 */
public class EmptyCamlEnvironment extends CamlEnvironment {
	private final Term testTerm;

	public EmptyCamlEnvironment() {
		testTerm = Term.mk_var("test", HOLType.mk_vartype("Z"));
	}
	
	@Override
	public CamlObject execute(String command) throws Exception {
		throw new Exception("Not implemented");
	}

	@Override
	public CamlObject execute(String command, CamlType returnType)
			throws Exception {
		System.out.println("Executing: " + command);
/*		
		if (returnType.equals(CamlType.TERM)) {
			return testTerm;
		}
		
		if (returnType.equals(CamlType.THM)) {
			return new Theorem.TempTheorem(testTerm, true);
		}
*/		
		return null;
	}

	@Override
	public String runCommand(String rawCommand) throws Exception {
		throw new Exception("Not implemented");
	}

	@Override
	public String getRawOutput() {
		return null;
	}
	
}
