package edu.pitt.math.hol_ssreflect.ssreflect.parser;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import edu.pitt.math.hol_ssreflect.ocaml.CamlEnvironment;
import edu.pitt.math.hol_ssreflect.ocaml.CamlObject;
import edu.pitt.math.hol_ssreflect.ocaml.CamlType;
import edu.pitt.math.hol_ssreflect.core.Goalstate;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.LemmaNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.Node;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.RawNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.SectionHypothesisNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.SectionNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.SectionVariableNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.TacticNode;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.tree.ModuleNode;

/**
 * Converts the Coq-Ssreflect like text into HOL Light commands
 */
public class Interpreter {
    // Describes the last error
    private Exception error;
    // Contains the most recent output
    private String recentOutput;

    // A command executor
    private CommandExecutor executor;
    // The latest goal state
    private Goalstate state;
    // The scanner
    private Scanner scanner;

    // The base offset of the current script text
    private int baseTextPosition;

    // The active mode
    private int mode;

    // Mode constants
    private final static int PROOF_MODE = 1;
    private final static int GLOBAL_MODE = 2;

    // Information about all executed commands
    private final Stack<GlobalCommand> globalCommands;
    private final Stack<ProofCommand> proofCommands;

    // True if a module is declared (only one module is allowed)
    private boolean moduleFlag;

    // Listen for goal state updates
    private ArrayList<GoalListener> goalListeners = new ArrayList<GoalListener>();
    // Listen for messages
    private ArrayList<MessageListener> messageListeners = new ArrayList<MessageListener>();

    /**
     * Listens for goal state updates
     */
    public interface GoalListener {
        public void updateGoal(Goalstate state);
    }

    /**
     * Listens for important messages
     */
    public interface MessageListener {
        /**
         * Called when the interpretation begins
         */
        public void begin();

        /**
         * An error message
         */
        public void error(String msg);

        /**
         * An info message
         */
        public void info(String msg);
    }

    /**
     * Executes CAML commands
     */
    private class CommandExecutor {
        // A CAML environment
        private final CamlEnvironment caml;

        // For logging executed commands;
        private PrintWriter commandLog;

        /**
         * Constructor
         */
        public CommandExecutor(CamlEnvironment caml) {
            assert (caml != null);

            this.caml = caml;
            this.commandLog = null;
        }


        /**
         * Initializes a new session
         */
        public void initSession() {
            try {
                runCommand("Sections.end_all_sections();;");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        /**
         * Initializes the log file
         */
        public void initLog(String logName) {
            if (commandLog != null) {
                commandLog.flush();
                commandLog.close();
            }

            if (logName != null) {
                PrintWriter writer = null;

                try {
                    FileOutputStream fos = new FileOutputStream(logName, false);
                    writer = new PrintWriter(fos, true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    this.commandLog = writer;
                }
            } else {
                this.commandLog = null;
            }
        }

        /**
         * Executes a command and logs it if the result is not null
         */
        public CamlObject execute(String command, CamlType type, boolean outFlag, boolean logFlag) throws Exception {
            CamlObject result = caml.execute(command, type);
            if (logFlag) {
                if (result != null && commandLog != null)
                    commandLog.println(command + ";;");
            }

            if (outFlag) {
                recentOutput = caml.getRawOutput();
            }

            return result;
        }

        /**
         * Runs the given raw command and logs it
         */
        public String runCommand(String rawCommand) throws Exception {
            String out = caml.runCommand(rawCommand);
            recentOutput = out;

            // Check the output
            if (out == null ||
                    out.contains("Characters ") ||
                    out.contains("^^^") ||
                    out.contains("Unbound value") ||
                    out.contains("Exception:") ||
                    out.contains("Parse error") ||
                    out.contains("Error: ") ||
                    out.contains("Interrupted")) {
                throw new Exception("Command failed: " + out);
            }

            // Log the command on success only
            if (commandLog != null)
                commandLog.println(rawCommand);
            return out;
        }

    }


    /**
     * Describes a command
     */
    private abstract class CommandInfo {
        // The command itself
        public final Node command;

        // The position of the last character of the command in the script text
        public int endTextPosition;

        /**
         * Protected constructor
         */
        protected CommandInfo(Node command, int endTextPosition) {
            this.command = command;
            this.endTextPosition = endTextPosition;
        }

        // Reverts the command
        // Returns true on success
        public boolean revert() {
            if (command == null)
                return true;

            // A special action for modules
            if (command instanceof ModuleNode) {
                Interpreter.this.moduleFlag = false;
                return true;
            }

            String cmd = command.getRevertCommand();
            if (cmd == null)
                return true;

            try {
                executor.runCommand(cmd + ";;");
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            if (command == null)
                return "[no command]";

            return command.toString() + "[" + endTextPosition + "]";
        }
    }

    // A command in the "global" mode
    private class GlobalCommand extends CommandInfo {
        GlobalCommand(Node command, int endTextPosition) {
            super(command, endTextPosition);
        }
    }

    // A command for starting and ending a section
    private class SectionCommand extends GlobalCommand {
        private final SectionNode section;
        // All lemmas finalized in the corresponding section
        private final ArrayList<LemmaCommand> finalizedLemmas;

        SectionCommand(SectionNode section, int endTextPosition) {
            super(section, endTextPosition);
            this.section = section;
            if (!section.isStartSection())
                this.finalizedLemmas = new ArrayList<LemmaCommand>();
            else
                this.finalizedLemmas = null;
        }

        public boolean isStartSection() {
            return section.isStartSection();
        }

        public String getSectionName() {
            return section.getName();
        }

        public ArrayList<LemmaCommand> getFinalizedLemmas() {
            return finalizedLemmas;
        }
    }

    // A lemma description command
    private class LemmaCommand extends GlobalCommand {
        // The lemma description
        private final LemmaNode lemma;
        // If true, then the proof of the lemma has been completed
        public boolean completeFlag;
        // True, if the lemma is proved
        public boolean provedFlag;

        LemmaCommand(LemmaNode lemma, int endTextPosition) {
            super(lemma, endTextPosition);
            this.lemma = lemma;
            this.completeFlag = false;
            this.provedFlag = false;
        }

        public String getLemmaName() {
            return lemma.getName();
        }

        public boolean isLet() {
            return lemma.isLet();
        }
    }

    // A command in the "proof" mode
    private class ProofCommand extends CommandInfo {
        ProofCommand(Node command, int endTextPosition) {
            super(command, endTextPosition);
        }
    }

    /**
     * Default constructor
     */
    public Interpreter(CamlEnvironment caml, String logName) {
        assert (caml != null);
        this.executor = new CommandExecutor(caml);
        this.globalCommands = new Stack<GlobalCommand>();
        this.proofCommands = new Stack<ProofCommand>();

        clearAndInit(logName);
    }


    /**
     * Clears the interpreter state and changes the log file
     *
     * @param logName
     */
    public void clearAndInit(String logName) {
        executor.initLog(logName);
        executor.initSession();
        this.mode = GLOBAL_MODE;
        this.state = null;
        this.globalCommands.clear();
        this.proofCommands.clear();
        this.error = null;
        this.moduleFlag = false;
    }


    /**
     * Adds a listener for goal updates
     */
    public void addGoalListener(GoalListener listener) {
        if (listener == null || goalListeners.contains(listener))
            return;

        goalListeners.add(listener);
    }


    /**
     * Removes the given listener
     */
    public void removeGoalListener(GoalListener listener) {
        goalListeners.remove(listener);
    }


    /**
     * Adds a listener for goal updates
     */
    public void addMessageListener(MessageListener listener) {
        if (listener == null || messageListeners.contains(listener))
            return;

        messageListeners.add(listener);
    }


    /**
     * Removes the given listener
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }


    /**
     * Returns errors of the last interpretation
     */
    public Exception getError() {
        return error;
    }


    /**
     * Returns the latest goal state
     */
    public Goalstate getGoalstate() {
        return state;
    }


    /**
     * Updates the listeners
     */
    private void updateListeners() {
        // Update goal listeners
        for (GoalListener listener : goalListeners) {
            listener.updateGoal(state);
        }

        // Update message listeners
        for (MessageListener listener : messageListeners) {
            if (recentOutput != null) {
                listener.info(recentOutput);
            }

            if (error != null) {
                listener.error(error.getMessage());
            }
        }
    }


    /**
     * Updates the goal state
     *
     * @throws Exception
     */
    private void getAndUpdateState() {
        try {
            CamlObject obj = executor.execute("top_goalstate()", CamlType.GOAL_STATE, false, false);
            if (obj == null) {
                System.err.println("Unexpected null result");
                obj = executor.execute("top_goalstate()", CamlType.GOAL_STATE, false, false);
            }
            Goalstate newState = (Goalstate) obj;
            state = newState;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Runs the command given by the syntax tree node and saves it in an appropriate stack.
     * The command will be not executed if there is no '.' after the command
     * in the text.
     */
    private void runCommand(Node nodeCmd) throws Exception {
        // .
        Token t = scanner.nextToken();
        if (t.type != TokenType.PERIOD)
            throw new Exception(". expected: " + t);

        String rawCmd = nodeCmd.toHOLCommand();
        if (mode == PROOF_MODE) {
//			rawCmd = "(hd o e) (" + rawCmd + ")";
//			CamlObject result = executor.execute(rawCmd, CamlType.GOAL_STATE, true);
//			if (result == null)
//				throw new Exception("Null result");
            rawCmd = "refine (by (VALID (" + rawCmd + ")));;";
            executor.runCommand(rawCmd);
        } else {
            rawCmd += ";;";
            executor.runCommand(rawCmd);
        }

        // Save the command in one of two stacks

        // Compute the end position of the command in the script text
        int end = baseTextPosition + t.ch + 1;

        switch (mode) {
            // Proof
            case PROOF_MODE:
                proofCommands.push(new ProofCommand(nodeCmd, end));
                break;

            // Global
            case GLOBAL_MODE:
                GlobalCommand cmd = null;
                // Lemma
                if (nodeCmd instanceof LemmaNode)
                    cmd = new LemmaCommand((LemmaNode) nodeCmd, end);
                    // Section
                else if (nodeCmd instanceof SectionNode)
                    cmd = new SectionCommand((SectionNode) nodeCmd, end);
                else if (nodeCmd instanceof RawNode ||
                        nodeCmd instanceof SectionHypothesisNode ||
                        nodeCmd instanceof SectionVariableNode ||
                        nodeCmd instanceof ModuleNode)
                    cmd = new GlobalCommand(nodeCmd, end);
                else
                    throw new Exception("Unexpected global command: " + nodeCmd);

                globalCommands.push(cmd);
                break;
        }
    }


    /**
     * Returns the last command saved in the stack
     */
    private CommandInfo getLastCommand() {
        if (proofCommands.size() > 0)
            return proofCommands.peek();

        if (globalCommands.size() > 0)
            return globalCommands.peek();

        return null;
    }


    /**
     * Sets up the base text position using the information about
     * executed commands
     */
    private void setBaseTextPosition() {
        CommandInfo lastCmd = getLastCommand();

        if (lastCmd == null) {
            baseTextPosition = 0;
            return;
        }

        baseTextPosition = lastCmd.endTextPosition;
    }


    /**
     * Interprets commands in the "proof" mode
     */
    private void processProofMode(TreeBuilder builder) throws Exception {
        Token t = scanner.peekToken();

        // Initial 'Proof' command (optional)
        if (t.value == "Proof" || t.value == "proof") {
            // Proof
            scanner.nextToken();

            // 'Proof' is allowed only as the first command in a proof
            if (proofCommands.size() > 0)
                throw new Exception("'Proof' should be the first proof command: " + t);

            // Should be .
            t = scanner.nextToken();
            if (t.type != TokenType.PERIOD)
                throw new Exception(". expected: " + t);

            // Do nothing
            proofCommands.add(new ProofCommand(null, baseTextPosition + t.ch + 1));
            return;
        }

        // Special commands
        if (t.value == "Abort" || t.value == "Qed" || t.value == "abort" || t.value == "qed") {
            boolean abortFlag = (t.value == "Abort" || t.value == "abort");

            // Abort or Qed
            scanner.nextToken();

            // Should be .
            t = scanner.nextToken();
            if (t.type != TokenType.PERIOD)
                throw new Exception(". expected: " + t);

            // This conversion should succeed
            LemmaCommand lemma = (LemmaCommand) globalCommands.peek();

            if (!abortFlag) {
                // Finish the proof
                String lemmaCmd;

                if (lemma.isLet()) {
                    lemmaCmd = "Sections.add_section_lemma " + '"' + lemma.getLemmaName() + '"'
                            + " (Sections.end_section_proof());;";
                } else {
                    lemmaCmd = "let " + lemma.getLemmaName() + " = Sections.end_section_proof();;";
                }

                executor.runCommand(lemmaCmd);
                lemma.provedFlag = true;
            } else {
                // Abort the proof
                executor.runCommand("clear_goalstack();;");
            }


            // Remove all proof commands and modify the previous lemma command
            proofCommands.clear();

            lemma.completeFlag = true;
            lemma.endTextPosition = baseTextPosition + t.ch + 1;

            // Switch the mode
            mode = GLOBAL_MODE;
            getAndUpdateState();
            return;
        }

        TacticNode node = builder.parseProof();

        // Run the command
        runCommand(node);
        getAndUpdateState();
    }


    /**
     * Returns the command which starts the last section
     */
    private SectionCommand getLastSection() {
        for (int i = globalCommands.size() - 1; i >= 0; i--) {
            CommandInfo cmd = globalCommands.get(i);
            if (cmd instanceof SectionCommand) {
                SectionCommand section = (SectionCommand) cmd;
                if (section.isStartSection())
                    return section;
            }
        }

        return null;
    }


    /**
     * Interprets commands in the "global" mode
     */
    private void processGlobalMode(TreeBuilder builder) throws Exception {
        assert (proofCommands.size() == 0);

        Node node = builder.parseGlobal();

        // Special treatment for modules
        if (node instanceof ModuleNode) {
            if (moduleFlag)
                throw new Exception("Another module is already declared");

            if (getLastSection() != null)
                throw new Exception("A module cannot be declared inside a section");

            moduleFlag = true;
        }

        // Special treatment for End Section
        if (node instanceof SectionNode && !((SectionNode) node).isStartSection()) {
            // Finalize all theorems in the section
            String sectionName = ((SectionNode) node).getName();

            SectionCommand start = getLastSection();
            if (start == null)
                throw new Exception("No open section");

            if (!sectionName.equals(start.getSectionName()))
                throw new Exception("The open section has the name " + start.getSectionName());

            // Get all theorems in the section
            ArrayList<LemmaCommand> lemmas = new ArrayList<LemmaCommand>();
            for (int i = globalCommands.size() - 1; i >= 0; i--) {
                CommandInfo cmd = globalCommands.get(i);
                if (cmd == start)
                    break;

                if (cmd instanceof LemmaCommand) {
                    LemmaCommand lemmaCmd = (LemmaCommand) cmd;
                    if (!lemmaCmd.isLet() && lemmaCmd.provedFlag)
                        lemmas.add(lemmaCmd);
                }

                // Get theorems from another (closed) section
                if (cmd instanceof SectionCommand) {
                    SectionCommand sectionCmd = (SectionCommand) cmd;
                    assert (!sectionCmd.isStartSection());

                    lemmas.addAll(sectionCmd.getFinalizedLemmas());
                }
            }

            // Finalize theorems
            int n = lemmas.size();
            for (int i = 0; i < n; i++) {
                StringBuffer bufferCmd = new StringBuffer();
                LemmaCommand lemma = lemmas.get(i);
                String lemmaName = lemma.getLemmaName();
                bufferCmd.append("let " + lemmaName + " = Sections.finalize_theorem " + lemmaName);
                bufferCmd.append(";;");
                executor.runCommand(bufferCmd.toString());
            }

            // Close the section
            runCommand(node);

            // Remove all commands in the section
            // The last command should be the end section command
            SectionCommand endCmd = (SectionCommand) globalCommands.pop();

            while (true) {
                if (globalCommands.size() == 0)
                    throw new Exception("End 'Section' without Section 'Section'");

                CommandInfo cmd = globalCommands.pop();
                if (cmd == start)
                    break;
            }

            endCmd.getFinalizedLemmas().addAll(lemmas);
            globalCommands.push(endCmd);
        } else {
            runCommand(node);
        }

        // Switch the mode after the lemma
        if (node instanceof LemmaNode) {
            getAndUpdateState();
            mode = PROOF_MODE;
        }

    }

    /**
     * Reverts the last command and returns the absolute position in the text
     * of the end of the second to the last command
     */
    public int revertOneCommand() {
        boolean updateStateFlag = false;

        CommandInfo last = getLastCommand();
        if (last instanceof GlobalCommand)
            updateStateFlag = false;
        else
            updateStateFlag = true;

        int pos = revertOne();

        if (updateStateFlag) {
            getAndUpdateState();
            updateListeners();
        }

        return pos;
    }


    /**
     * Reverts the last command and returns the absolute position in the text
     * of the end of the second to the last command
     */
    private int revertOne() {
        CommandInfo lastCmd = null;

        if (proofCommands.size() > 0) {
            lastCmd = proofCommands.pop();
        } else {
            if (globalCommands.size() > 0) {
                GlobalCommand cmd = globalCommands.pop();
                // Switch the mode if necessary
                if (cmd instanceof LemmaCommand && !((LemmaCommand) cmd).completeFlag)
                    mode = GLOBAL_MODE;

                lastCmd = cmd;
            }
        }

        if (lastCmd == null)
            return 0;

        // Revert the last command
        lastCmd.revert();

        // Get the new last command
        lastCmd = getLastCommand();
        if (lastCmd == null)
            return 0;

        return lastCmd.endTextPosition;
    }


    /**
     * Reverts all commands up to the given absolute position in the script text
     */
    public int revert(int pos) {
        int endPos = pos;
        boolean updateStateFlag = false;

        // Iterate through proof commands first
        while (proofCommands.size() > 0) {
            CommandInfo cmd = proofCommands.peek();
            if (cmd.endTextPosition < pos) {
                if (updateStateFlag) {
                    getAndUpdateState();
                    updateListeners();
                }

                return endPos;
            }

            endPos = revertOne();
            updateStateFlag = true;
        }

        // Iterate through lemma commands next
        while (globalCommands.size() > 0) {
            CommandInfo cmd = globalCommands.peek();
            if (cmd.endTextPosition < pos)
                break;

            endPos = revertOne();
        }

        if (updateStateFlag) {
            getAndUpdateState();
            updateListeners();
        }

        return endPos;
    }


    /**
     * Interprets the script.
     * Returns the number of characters interpreted before the first error (if any)
     */
    public int interpret(String text) {
        // Reset the error and the most recent output
        error = null;
        recentOutput = null;

        // Begin the interpretation
        for (MessageListener listener : messageListeners) {
            listener.begin();
        }

        // Initialize the scanner first
        scanner = new Scanner(new StringReader(text));
        TreeBuilder builder = new TreeBuilder(scanner);

        // Set up the base text position
        setBaseTextPosition();

        try {
            while (true) {
                if (scanner.peekToken().type == TokenType.EOF)
                    break;

                switch (mode) {
                    // Proof
                    case PROOF_MODE:
                        processProofMode(builder);
                        break;

                    // Global
                    case GLOBAL_MODE:
                        processGlobalMode(builder);
                        break;
                }
            }
        } catch (Exception e) {
//			e.printStackTrace();
            System.err.println(e.getMessage());
            error = e;
        } finally {
            updateListeners();
        }

        CommandInfo lastCmd = getLastCommand();
        if (lastCmd == null)
            return 0;

        return lastCmd.endTextPosition - baseTextPosition;
    }
}
