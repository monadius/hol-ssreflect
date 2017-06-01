package edu.pitt.math.hol_ssreflect.test;

import java.util.ArrayList;


import edu.pitt.math.hol_ssreflect.ocaml.CamlEnvironment;
import edu.pitt.math.hol_ssreflect.ocaml.CamlList;
import edu.pitt.math.hol_ssreflect.ocaml.CamlObject;
import edu.pitt.math.hol_ssreflect.ocaml.CamlPair;
import edu.pitt.math.hol_ssreflect.ocaml.CamlString;
import edu.pitt.math.hol_ssreflect.ocaml.CamlType;
import edu.pitt.math.hol_ssreflect.core.Term;
import edu.pitt.math.hol_ssreflect.core.Theorem;

/**
 * A database of theorems
 */
public class TheoremDatabase {
	private final CamlEnvironment caml;
	
	/**
	 * Constructor
	 */
	public TheoremDatabase(CamlEnvironment env) {
		this.caml = env;
	}
	
	
	/**
	 * Finds all theorems containing the given term t
	 */
	public ArrayList<Theorem> find(Term t) {
		ArrayList<Theorem> result = new ArrayList<Theorem>();
		
		// Prepare a command
		String tCmd = t.makeCamlCommand();
		String cmd = "search[" + tCmd + "]";
		
		CamlType pairType = new CamlType.PairType(CamlType.STRING, CamlType.THM);
		CamlType returnType = new CamlType.ListType(pairType);
		
		// Execute the command
		CamlObject obj;
		try {
			obj = caml.execute(cmd, returnType);
		}
		catch (Exception e) {
			e.printStackTrace();
			return result;
		}
		
		// Process the result
		CamlList list = (CamlList) obj;
		if (list == null)
			return result;

		for (int i = 0; i < list.size(); i++) {
			CamlPair pair = (CamlPair) list.get(i);
			CamlString name = (CamlString) pair.first();
			Theorem th = (Theorem) pair.second();
			
			result.add(Theorem.mk_theorem(name.str, th.concl()));
		}
		
		return result;
	}
	
	
	/**
	 * Finds all theorems name of which contains the given string
	 */
	public ArrayList<Theorem> find(String str) {
		ArrayList<Theorem> result = new ArrayList<Theorem>();
		
		// Prepare a command
		String cmd = "search[name \"" + str + "\"]";
		
		CamlType pairType = new CamlType.PairType(CamlType.STRING, CamlType.THM);
		CamlType returnType = new CamlType.ListType(pairType);
		
		// Execute the command
		CamlObject obj;
		try {
			obj = caml.execute(cmd, returnType);
		}
		catch (Exception e) {
			e.printStackTrace();
			return result;
		}
		
		// Process the result
		CamlList list = (CamlList) obj;
		if (list == null)
			return result;

		for (int i = 0; i < list.size(); i++) {
			CamlPair pair = (CamlPair) list.get(i);
			CamlString name = (CamlString) pair.first();
			Theorem th = (Theorem) pair.second();
			
			result.add(Theorem.mk_theorem(name.str, th.concl()));
		}
		
		return result;
	}
}
