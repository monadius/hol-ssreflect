package edu.pitt.math.hol_ssreflect.ocaml;

import edu.pitt.math.hol_ssreflect.ocaml.CamlEnvironment;
import edu.pitt.math.hol_ssreflect.ocaml.CamlObject;
import edu.pitt.math.hol_ssreflect.ocaml.CamlType;
import edu.pitt.math.hol_ssreflect.core.parser.Parser;

/**
 * Created by monad on 5/31/17.
 */
public class ToplevelClientEnvironment extends CamlEnvironment {
    private HOLLightWrapper caml;
    private String output;

    public ToplevelClientEnvironment(String hostName, int port) throws Exception {
        caml = new HOLLightWrapper(holName);
//		ocaml = new HOLLightWrapper("dmtcp_restart", "/home/monad/hol_light_ckpts/cp4_trig_hyp_fan_pack_tame.dmtcp");
//		ocaml = new HOLLightWrapper("cr_restart", "--no-restore-pid", "-S", "2",
//					"/home/monad/hol_light_ckpts/cr_current.cr");
        caml.runCommand("needs \"ocaml/raw_printer.hl\";;");
        caml.runCommand("needs \"ocaml/ssreflect.hl\";;");
        caml.runCommand("needs \"ocaml/sections.hl\";;");
    }


    @Override
    public CamlObject execute(String command) throws Exception {
        throw new Exception("Unimplemented");
    }


    @Override
    public String runCommand(String rawCommand) throws Exception {
        System.out.println("Executing: " + rawCommand);
        output = caml.runCommand(rawCommand);
        System.out.println("Output: " + output);

        return output;
    }


    @Override
    public CamlObject execute(String command, CamlType returnType) throws Exception {
        String printCmd = returnType.getPrintCommand();
        command = "raw_print_string(" + printCmd + "(" + command + "));;";

        command = escape(command);
        System.out.println("Executing: " + command);

        output = caml.runCommand(command);
        String testString = output;

        if (testString.length() > 1000) {
            testString = testString.substring(0, 1000);
        }
        System.out.println("Out: " + testString);

        String result = strip(output);
        if (result == null) {
            System.err.println("Null result");
            return null;
        }

        return Parser.parse(result);
    }


    private static String escape(String str) {
        StringBuilder out = new StringBuilder(str.length() * 2);
        int n = str.length();

        for (int i = 0; i < n; i++) {
            char ch = str.charAt(i);
            if (ch == '\\' && i < n - 1 && str.charAt(i + 1) == '"') {
                out.append('\\');
            }

            out.append(ch);
        }

        return out.toString();
    }

    private static String unescapeOCamlString(String str) {
        StringBuffer out = new StringBuffer(str.length());
        int n = str.length();

        for (int i = 0; i < n; i++) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                if (i >= n) {
                    System.err.println("Bad OCaml string: " + str);
                    out.append(ch);
                    continue;
                }

                i += 1;
                char next = str.charAt(i);
                switch (next) {
                    case '"':
                        out.append(ch);
                        break;
                    case '\\':
                        out.append(ch);
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 'b':
                        out.append('\b');
                        break;
                    default:
                        if (i >= n || next < '0' || next > '9') {
                            System.err.println("Bad OCaml string: " + str);
                            out.append(next);
                            continue;
                        }

                        int code = (next - '0') * 100;
                        i += 1;
                        next = str.charAt(i);
                        if (i >= n || next < '0' || next > '9') {
                            System.err.println("Bad OCaml string: " + str);
                            out.append(next);
                            continue;
                        }

                        code += (next - '0') * 10;
                        i += 1;
                        next = str.charAt(i);
                        if (i >= n || next < '0' || next > '9') {
                            System.err.println("Bad OCaml string: " + str);
                            out.append(next);
                            continue;
                        }

                        code += next - '0';
                        out.append((char) code);
                        break;
                }
            }
            else {
                out.append(ch);
            }
        }

        return out.toString();
    }


    private static String strip(String str) {
        int i1 = str.indexOf("$begin$");
        int i2 = str.indexOf("$end$");

        if (i2 <= i1)
            return null;

        return str.substring(i1 + "$begin".length() + 1, i2);
    }


    @Override
    public String getRawOutput() {
        return output;
    }

}
