package edu.pitt.math.hol_ssreflect.ssreflect.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants.ColorConstants;

import edu.pitt.math.hol_ssreflect.ocaml.CamlEnvironment;
import edu.pitt.math.hol_ssreflect.core.Goalstate;
import edu.pitt.math.hol_ssreflect.core.printer.TermPrinterData;
import edu.pitt.math.hol_ssreflect.ocaml.DebugEnvironment;
import edu.pitt.math.hol_ssreflect.ocaml.ToplevelClientEnvironment;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.Compiler;
import edu.pitt.math.hol_ssreflect.ssreflect.parser.Interpreter;

/**
 * Test GUI
 * @author Alexey
 *
 */
@SuppressWarnings("serial")
public class TestSSReflectGUI extends JFrame implements Configuration.Saver, ActionListener {
	// OCaml environment
	private final CamlEnvironment camlEnv;
	
	// Interprets the script
	private final Interpreter interpreter;
	
	// Contains all settings
	private final Configuration configuration;
	
	// File manager
	private final FileManager fileManager;
	
	// Configuration group of this frame
	private static final String CONF_GROUP = "main-window";
	private static final String CONF_GROUP2 = "main-window.components";
	private static final String CONF_GROUP_EDIT = "editor";
	
	// Commands
	private static final String CMD_FILE_NEW = "file-new";
	private static final String CMD_FILE_OPEN = "file-open";
	private static final String CMD_FILE_SAVE = "file-save";
	private static final String CMD_FILE_SAVE_AS = "file-save-as";
	private static final String CMD_FILE_EXIT = "file-exit";
	private static final String CMD_EDIT_HIGHLIGHT = "edit-highlight";
	private static final String CMD_RUN_COMPILE = "run-compile";
	private static final String CMD_RUN_FAST_COMPILE = "run-fast-compile";
	
	// File menu
	private JMenu fileMenu;
	// Highlight switch
	private JCheckBoxMenuItem highlightMenu;
	
	// Splitter
	private JSplitPane splitter1, splitter2, splitter3;
	
	// Displays the proof state
	private GoalstatePanel goals;
	
	// For searching theorems
	private TheoremPanel theorems;

	// The main script editor
	private TextEditor editor;
	
    private JTextPane logArea;
	
	/**
	 * Constructor
	 */
	public TestSSReflectGUI(CamlEnvironment caml) {
		this.camlEnv = caml;
//		this.interpreter = new Interpreter(ocaml, "ocaml/test.log");
		this.interpreter = new Interpreter(caml, null);
		this.configuration = new Configuration("gui.xml");
		configuration.addSaver(this);

		// Create the main menu
		createMenu();

		// Create the file manager
		this.fileManager = new FileManager(configuration, fileMenu, this);
		configuration.addSaver(fileManager);

		// Initialize all components
		initMainWindow();
		initEditor();
		initLog();
		initGoalPanel();
		initTheoremPanel(caml);
		
		initSplitters();
		
		// Configure the file manager
		fileManager.addCurrentFileListener(new FileManager.CurrentFileListener() {
			@Override
			public void currentFileChanged(File currentFile) {
				String name = (currentFile == null) ? "New" : currentFile.getAbsolutePath();
				setTitle(name);
			}
		});
		
		setTitle("New");
		FileManager.FileInfo currentFile = fileManager.getCurrentFile();
		
		if (currentFile != null) {
			String text = fileManager.readCurrent();
			setNewText(text, currentFile.getPosition());
			setTitle(currentFile.file.getAbsolutePath());
		}

		// Finish the initialization
		setVisible(true);
	}
	

	/**
	 * Initializes the main window
	 */
	private void initMainWindow() {
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}			
		});
		
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		// Set up the size of the window
		Configuration.Group conf = configuration.getGroup(CONF_GROUP);
		
		setPreferredSize(conf.getDimensionVal("preferred-size", 1200, 850));
		setMinimumSize(new Dimension(400, 300));

		setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));
		setBounds(conf.getIntVal("x", 0), conf.getIntVal("y", 0), conf.getIntVal("w", 1200), conf.getIntVal("h", 850));
	}
	

	/**
	 * Initializes the editor
	 */
	private void initEditor() {
		// Create the text editor
		editor = new TextEditor(interpreter);
		
		Configuration.Group conf = configuration.getGroup(CONF_GROUP_EDIT);
		boolean highlight = conf.getBoolVal("highlight", false);
		highlightMenu.setSelected(highlight);
		editor.setHighlightFlag(highlight);
		
		editor.addListener(new TextEditor.Listener() {
			@Override
			public void modified(boolean modifiedFlag) {
				String title = getTitle();
				if (title == null || title.length() == 0)
					title = "New";
				
				if (title.charAt(title.length() - 1) == '*')
					title = title.substring(0, title.length() - 1);
				
				if (modifiedFlag)
					title += "*";
				
				setTitle(title);
			}
		});

		// Set up the font parameters
		editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
	}
	

	/**
	 * Initializes the log area
	 */
	private void initLog() {
        logArea = new JTextPane();
        logArea.setEditable(false);
//        logArea.setColumns(80);
//        logArea.setLineWrap(true);
//        logArea.setRows(100);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));

        final SimpleAttributeSet redAttrs = new SimpleAttributeSet();
		redAttrs.addAttribute(ColorConstants.Foreground, Color.red);
        
        // Create a message listener
        interpreter.addMessageListener(new Interpreter.MessageListener() {
			@Override
			public void info(String msg) {
				try {
					logArea.getStyledDocument().insertString(0, msg, null);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void error(String msg) {
				if (msg == null)
					return;

				try {
					logArea.getStyledDocument().insertString(0, msg, redAttrs);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void begin() {
				// Clear the log area
				logArea.setText("");
			}
		});
	}
	
	
	/**
	 * Initialize the theorem panel
	 */
	private void initTheoremPanel(CamlEnvironment caml) {
		// Create the theorem panel
		theorems = new TheoremPanel(configuration, caml, fileManager);
	}
	
	
	/**
	 * Initializes the goal panel
	 */
	private void initGoalPanel() {
        goals = new GoalstatePanel(configuration);

        // Add a goal update listener
		interpreter.addGoalListener(new Interpreter.GoalListener() {
			@Override
			public void updateGoal(Goalstate state) {
				goals.update(state);
			}
		});
	}
	
	
	@Override
	public void save(Configuration conf) {
		Configuration.Group group = conf.getGroup(CONF_GROUP);
		// Preferred size
		group.setVal("preferred-size", getPreferredSize());
		
		// Location and size
		Point p = getLocation();
		Dimension s = getSize();
		group.setVal("x", p.x);
		group.setVal("y", p.y);
		group.setVal("w", s.width);
		group.setVal("h", s.height);
		
		// Splitters
		group = conf.getGroup(CONF_GROUP2);
		group.setVal("split1", splitter1.getDividerLocation());
		group.setVal("split2", splitter2.getDividerLocation());
		group.setVal("split3", splitter3.getDividerLocation());
		
		// Editor
		group = conf.getGroup(CONF_GROUP_EDIT);
		group.setVal("highlight", editor.getHighlightFlag());
	}
	
	
	/**
	 * Sets the new text in the editor and resets the interpreter
	 */
	private void setNewText(String text, int initPosition) {
//		String logName = "ocaml/test.log";
		
//		if (fileManager.getCurrentFile() != null) {
//			logName = fileManager.getCurrentFile().file.getAbsolutePath() + ".log";
//		}

		if (text != null) {
			editor.initText(text, initPosition);
//			interpreter.clearAndInit(logName);
			interpreter.clearAndInit(null);
		}
	}

   
	/**
	 * Initializes components
	 */
    private void initSplitters() {
        // Finish the initialization
        JScrollPane textScroll = new JScrollPane(editor);
        textScroll.setPreferredSize(new Dimension(700, 600));
        textScroll.setMinimumSize(new Dimension(300, 300));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(500, 200));
        
        splitter1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, textScroll, logScroll);
        splitter3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, goals, theorems);
        splitter2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter1, splitter3);
        
        // Configure splitters
        Configuration.Group group = configuration.getGroup(CONF_GROUP2);
        splitter1.setDividerLocation(group.getIntVal("split1", 100));
        splitter2.setDividerLocation(group.getIntVal("split2", 100));
        splitter3.setDividerLocation(group.getIntVal("split3", 100));
        
        add(splitter2);
    }
    
    
    /**
     * Creates the main menu
     */
    private void createMenu() {
    	JMenuBar menuBar;
    	JMenu menu;
    	JMenuItem menuItem;

    	// Create the menu bar.
    	menuBar = new JMenuBar();

    	// Build the File menu.
    	menu = new JMenu("File");
    	menu.setMnemonic(KeyEvent.VK_F);
    	menuBar.add(menu);

    	// Menu items
    	// New
    	menuItem = new JMenuItem("New",
    	                         KeyEvent.VK_N);
    	menuItem.setActionCommand(CMD_FILE_NEW);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);

    	// Open
    	menuItem = new JMenuItem("Open...",
    	                         KeyEvent.VK_O);
    	menuItem.setActionCommand(CMD_FILE_OPEN);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);

    	// Save
    	menuItem = new JMenuItem("Save", KeyEvent.VK_S);
    	menuItem.setAccelerator(KeyStroke.getKeyStroke(
    	        KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    	menuItem.setActionCommand(CMD_FILE_SAVE);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);

    	// Save as
    	menuItem = new JMenuItem("Save as...");
    	menuItem.setAccelerator(KeyStroke.getKeyStroke(
    	        KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
    	menuItem.setActionCommand(CMD_FILE_SAVE_AS);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);
    	
    	// Exit
    	menu.addSeparator();
    	
    	menuItem = new JMenuItem("Exit", KeyEvent.VK_X);
    	menuItem.setActionCommand(CMD_FILE_EXIT);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);

    	this.fileMenu = menu;

    	/////////////////////
    	// Edit
    	menu = new JMenu("Edit");
    	menu.setMnemonic(KeyEvent.VK_E);
    	menuBar.add(menu);
    	
    	highlightMenu = new JCheckBoxMenuItem("Highlight");
    	menuItem = highlightMenu;
    	menuItem.setActionCommand(CMD_EDIT_HIGHLIGHT);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);

    	/////////////////////
    	// Run
    	menu = new JMenu("Run");
    	menu.setMnemonic(KeyEvent.VK_R);
    	menuBar.add(menu);
    	
    	menuItem = new JMenuItem("Compile (fast)");
    	menuItem.setActionCommand(CMD_RUN_FAST_COMPILE);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);
    	
    	menuItem = new JMenuItem("Compile");
    	menuItem.setAccelerator(KeyStroke.getKeyStroke(
    	        KeyEvent.VK_B, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
    	menuItem.setActionCommand(CMD_RUN_COMPILE);
    	menuItem.addActionListener(this);
    	menu.add(menuItem);
    	
    	// Finish the menu initialization
    	this.setJMenuBar(menuBar);    	
    }
    
    
    /**
     * Compiles the current text
     */
    private void compileText(boolean fastFlag) {
    	String text = editor.getText();
    	FileManager.FileInfo currentFile = fileManager.getCurrentFile();
    	
    	if (currentFile == null) {
    		System.err.println("compileText(): null file");
    		return;
    	}
    	
    	String outName = currentFile.getName();
    	FileOutputStream out = null;
    	try {
    		File outFile = new File(currentFile.file.getParentFile(), outName + "-compiled.hl");
    		out = new FileOutputStream(outFile);
    		Reader in = new StringReader(text);
    		boolean errors = false;
    		
    		logArea.setText("");
    		editor.revertAll();
    		
    		try {
    			Compiler compiler = new Compiler(in, out);
    			if (fastFlag)
    				compiler.compile(null);
    			else
    				compiler.compile(camlEnv);
    		}
    		catch (Exception e) {
    	        final SimpleAttributeSet redAttrs = new SimpleAttributeSet();
    			redAttrs.addAttribute(ColorConstants.Foreground, Color.red);
				logArea.getStyledDocument().insertString(0, e.getMessage(), redAttrs);
				errors = true;
    		}
    		
    		if (!errors)
    			logArea.getStyledDocument().insertString(0, "Compile complete", null);
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	finally {
    		if (out != null) {
    			try {
    				out.close();
    			}
    			catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}
    }
    

    @Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		
		cmd = cmd.intern();
		
		// Compile
		if (cmd == CMD_RUN_COMPILE || cmd == CMD_RUN_FAST_COMPILE) {
			// Save all modifications first
			if (fileManager.saveCurrent(editor.getText(), editor.getCaretPosition())) {
				editor.setModified(false);
				compileText(cmd == CMD_RUN_FAST_COMPILE);
			}
			
			return;
		}
		
		// Highlight
		if (cmd == CMD_EDIT_HIGHLIGHT) {
			boolean flag = highlightMenu.isSelected();
			editor.setHighlightFlag(flag);
			return;
		}
		
		// Exit
		if (cmd == CMD_FILE_EXIT) {
			exit();
			return;
		}
		
		// New
		if (cmd == CMD_FILE_NEW) {
			if (!saveModified()) {
				return;
			}
			
			fileManager.setCurrentFile(null);
			setNewText("", 0);
			return;
		}
		
		// Open
		if (cmd == CMD_FILE_OPEN) {
			if (!saveModified()) {
				return;
			}
			
			String text = fileManager.openAndRead();
			setNewText(text, 0);
			return;
		}
		
		// Save
		if (cmd == CMD_FILE_SAVE) {
			if (fileManager.saveCurrent(editor.getText(), editor.getCaretPosition()))
				editor.setModified(false);
			return;
		}
		
		// Save as
		if (cmd == CMD_FILE_SAVE_AS) {
			if (fileManager.saveAs(editor.getText(), editor.getCaretPosition()))
				editor.setModified(false);
			return;
		}
		
		// Recently open file
		if (cmd.startsWith(FileManager.CMD_FILE_RECENT)) {
			cmd = cmd.substring(FileManager.CMD_FILE_RECENT.length());
			String[] els = cmd.split(";");
			String name = els[0];
			int pos = Integer.parseInt(els[1]);
			if (!saveModified()) {
				return;
			}
			
			fileManager.setCurrentFile(new File(name), pos);
			setNewText(fileManager.readCurrent(), pos);
			return;
		}
	}
    
    
    /**
     * Shows a dialog for saving the modified text.
     * Returns false if a user selects the 'Cancel' option
     */
    private boolean saveModified() {
		if (editor.isModified()) {
			int result = JOptionPane.showConfirmDialog(TestSSReflectGUI.this, "Save the text?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
			switch (result) {
			case JOptionPane.YES_OPTION:
				fileManager.saveCurrent(editor.getText(), editor.getCaretPosition());
				break;
			
			case JOptionPane.CANCEL_OPTION:
				return false;
			}
		}
		
		return true;
    }
    
    
    
    /**
     * Exits the program
     */
    private void exit() {
    	if (!saveModified()) {
    		return;
    	}

		// Save the configuration
		try {
			configuration.updateAndSave();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		// Exit
		System.exit(0);
    }

	/**
	 * Main function
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		TermPrinterData.init();
		
//		final CamlEnvironment caml = new DebugEnvironment();
		CamlEnvironment caml0;

		try {
//			caml0 = new ToplevelClientEnvironment("Alexeys-iMac.local", 1500);
			caml0 = new ToplevelClientEnvironment("192.168.56.101", 1500);
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			caml0 = new DebugEnvironment();
		}

/*
		String holName = "hol_light";
		if (args.length >= 1) {
			holName = args[0];
		}
		
		CamlEnvironment caml0;
		
		try {
			caml0 = new TestCamlEnvironment(holName);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			caml0 = new EmptyCamlEnvironment();
		}
*/
		final CamlEnvironment caml = caml0;
		
        SwingUtilities.invokeLater(new Runnable() {
        	public void run() {
        		new TestSSReflectGUI(caml);
		    }
		});
	}


}
