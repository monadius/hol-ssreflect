package edu.pitt.math.hol_ssreflect.core.printer;

import edu.pitt.math.hol_ssreflect.ocaml.CamlObject;
import edu.pitt.math.hol_ssreflect.core.HOLType;
import edu.pitt.math.hol_ssreflect.core.Term;
import edu.pitt.math.hol_ssreflect.core.Theorem;

/**
 * Returns printable trees for Caml objects
 */
public class Printer {
	/**
	 * Prints a term
	 */
	public static SelectionTree print(Term tm) {
		return TermPrinter.print(tm);
	}
	

	/**
	 * Prints a theorem
	 */
	public static SelectionTree print(Theorem th) {
		return TheoremPrinter.print(th);
	}
	
	
	/**
	 * Prints a HOL type
	 */
	public static SelectionTree print(HOLType type) {
		return TypePrinter.print(type);
	}
	
	
	/**
	 * Prints a generic Caml object
	 */
	public static SelectionTree print(CamlObject obj) {
		if (obj instanceof Term)
			return print((Term) obj);
		
		if (obj instanceof Theorem)
			return print((Theorem) obj);
		
		if (obj instanceof HOLType)
			return print((HOLType) obj);
		
		
		// Default
		SelectionTree tree = new SelectionTree(obj, obj.toCommandString());
		return tree;
	}
}
