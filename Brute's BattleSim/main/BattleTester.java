package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import enums.Stat;
import simpleconcepts.Unit;

/**
 * @author InfuriatedBrute
 */
public final class BattleTester extends BattleController {
	Battle b;
	public static String ls = System.lineSeparator();
	private JTextArea attackerTextArea, defenderTextArea, seedTextArea;
	private JTextField attackerLabel, defenderLabel, seedLabel, attackerCostDisplayArea, defenderCostDisplayArea;
	private JFrame frame;
	private JTextPane resultPane = new JTextPane();
	boolean lastTurn = false;
	int results = -999;
	Random random = new Random();
	WindowAdapter exitOnClose = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	};
	ActionListener attackerRandomListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setRandomArmy(true);
		}
	};
	ActionListener attackerBlankListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setBlankArmy(true);
		}
	};
	ActionListener defenderRandomListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setRandomArmy(false);
		}
	};
	ActionListener defenderBlankListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setBlankArmy(false);
		}
	};
	ActionListener runListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			run();
		}
	};

	public static void main(String[] args) {
		new BattleTester();
	}

	private BattleTester() {
		inputPopup();
	}

	private void run() {
		String[] attackerArmy = attackerTextArea.getText().split(ls);
		String s = isValidArmy(attackerArmy);
		if (!s.equals("")) {
			errorBox(s);
			return;
		}
		String[] defenderArmy = defenderTextArea.getText().split(ls);
		s = isValidArmy(defenderArmy);
		if (!s.equals("")) {
			errorBox(s);
			return;
		}
		Long RNGseed;
		try {
			RNGseed = Long.parseLong(seedTextArea.getText());
		} catch (NumberFormatException e) {
			errorBox("The string in the seed box must be a number between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE
					+ "." + ls + "No commas, no decimals, etc.");
			return;
		}
		b = new Battle(attackerArmy, defenderArmy, RNGseed, this);
		frame.dispose();
		results = b.simulate();
		lastTurn = true;
		try {
			endOfTurn();
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public static void errorBox(String infoMessage) {
		JOptionPane.showMessageDialog(null, infoMessage, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * 
	 * @param army
	 *            the army to check whether is valid
	 * @return a string for a reason the army isn't valid, or the empty string if it
	 *         is valid
	 */
	private String isValidArmy(String[] army) {
		if (army.length != Constants.START_AREA_HEIGHT)
			return "An army is the wrong height. Please keep the armies the same height as the default and try again.";
		for (String line : army)
			if (line.length() != Constants.START_AREA_WIDTH)
				return "An army is the wrong width. Please keep the armies the same width as the default and try again.";
		for (String line : army)
			for (char c : line.toCharArray()) {
				if (c != ls.toCharArray()[0] && c != Constants.BLANK_SPACE_CHAR) {
					if (Unit.getTypes().get(c) == null)
						return "A character is not recognized. Please try again. Keep in mind the input is case-sensitive.";
				}
			}
		return "";
	}

	@Override
	public void endOfTurn() throws Exception {
		if (b.actionPerformedThisTurn || b.turn == 1 || lastTurn) {
			if (lastTurn) {
				String victory;
				switch (results) {
				case 1:
					victory = "N ATTACKER VICTORY!";
					break;
				case 0:
					victory = " STALEMATE.";
					break;
				case -1:
					victory = " DEFENDER VICTORY!";
					break;
				default:
					throw new RuntimeException();
				}
				appendToPane(resultPane,
						(ls + ls + ls + "BATTLE ENDED ON TURN " + (b.turn - 1) + " WITH A" + victory + ls),
						Color.BLACK);
			} else
				appendToPane(resultPane, (ls + ls + ls + "Turn " + b.turn + ":" + ls), Color.BLACK);
			for (int row = 0; row < Constants.MAP_HEIGHT; row++) {
				appendToPane(resultPane, ls, Color.BLACK);
				for (int col = 0; col < Constants.MAP_WIDTH; col++) {
					if (b.grid[row][col] != null) {
						float howHurt = (1 - ((float) b.grid[row][col].get(Stat.CURRENTHP)
								/ (float) b.grid[row][col].get(Stat.MAXHP)));
						Color color = new Color(howHurt, howHurt / 2.0f, howHurt / 2.0f);
						if (b.grid[row][col].attacker == 1) {
							appendToPane(resultPane, String.valueOf(b.grid[row][col].getChar()), color, true);
						} else {
							appendToPane(resultPane, String.valueOf(b.grid[row][col].getChar()), color);
						}

					} else {
						appendToPane(resultPane, String.valueOf(Constants.BLANK_SPACE_CHAR), Color.BLACK);
					}
				}
			}
		}
		if (lastTurn) {
			try {
				resultsPopup();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void appendToPane(JTextPane tp, String msg, Color c) {
		appendToPane(tp, msg, c, false);
	}

	private void appendToPane(JTextPane tp, String msg, Color c, boolean italic) {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

		aset = sc.addAttribute(aset, StyleConstants.FontFamily, Constants.FONT);
		aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		aset = sc.addAttribute(aset, StyleConstants.Italic, italic);

		int len = tp.getDocument().getLength();
		tp.setCaretPosition(len);
		tp.setCharacterAttributes(aset, false);
		tp.replaceSelection(msg);
	}

	private void inputPopup() {
		attackerTextArea = new JTextArea();
		attackerCostDisplayArea = new JTextField();
		attackerLabel = new JTextField();
		defenderTextArea = new JTextArea();
		defenderCostDisplayArea = new JTextField();
		defenderLabel = new JTextField();
		seedTextArea = new JTextArea();
		seedLabel = new JTextField();
		Font font = new Font(Constants.FONT, Font.PLAIN, 18);

		attackerCostDisplayArea.setEditable(false);
		defenderCostDisplayArea.setEditable(false);
		attackerLabel.setEditable(false);
		defenderLabel.setEditable(false);
		seedLabel.setEditable(false);

		attackerTextArea.setFont(font);
		attackerCostDisplayArea.setFont(font);
		attackerLabel.setFont(font);
		defenderTextArea.setFont(font);
		defenderCostDisplayArea.setFont(font);
		defenderLabel.setFont(font);
		seedTextArea.setFont(font);
		seedLabel.setFont(font);

		setRandomArmy(true);
		attackerLabel.setText("Attacker");
		updateCost(true);
		setRandomArmy(false);
		updateCost(false);
		defenderLabel.setText("Defender");
		seedTextArea.setText(String.valueOf(new Random().nextLong()));
		seedLabel.setText("Seed");

		JButton doneButton = new JButton("Done");
		doneButton.addActionListener(runListener);
		JButton attackerBlankButton = new JButton("Blank");
		attackerBlankButton.addActionListener(attackerBlankListener);
		JButton attackerRandomButton = new JButton("Random");
		attackerRandomButton.addActionListener(attackerRandomListener);
		JButton defenderBlankButton = new JButton("Blank");
		defenderBlankButton.addActionListener(defenderBlankListener);
		JButton defenderRandomButton = new JButton("Random");
		defenderRandomButton.addActionListener(defenderRandomListener);

		JPanel armyPanel = new JPanel(), seedPanel = new JPanel(), donePanel = new JPanel(),
				attackerPanel = new JPanel(), defenderPanel = new JPanel(), attackerButtonPanel = new JPanel(), defenderButtonPanel = new JPanel();
		attackerPanel.setLayout(new BoxLayout(attackerPanel, BoxLayout.Y_AXIS));
		attackerPanel.add(attackerLabel);
		attackerPanel.add(attackerCostDisplayArea);
		attackerPanel.add(attackerTextArea);
		attackerButtonPanel.add(attackerBlankButton);
		attackerButtonPanel.add(attackerRandomButton);
		attackerPanel.add(attackerButtonPanel);
		defenderPanel.setLayout(new BoxLayout(defenderPanel, BoxLayout.Y_AXIS));
		defenderPanel.add(defenderLabel);
		defenderPanel.add(defenderCostDisplayArea);
		defenderPanel.add(defenderTextArea);		
		defenderButtonPanel.add(defenderBlankButton);
		defenderButtonPanel.add(defenderRandomButton);
		defenderPanel.add(defenderButtonPanel);
		seedPanel.setLayout((new BoxLayout(seedPanel, BoxLayout.Y_AXIS)));
		seedPanel.add(seedLabel);
		seedPanel.add(seedTextArea);
		donePanel.add(doneButton);

		addChangeListener(attackerTextArea, e -> updateCost(true));
		addChangeListener(defenderTextArea, e -> updateCost(false));

		armyPanel.add(attackerPanel);
		armyPanel.add(defenderPanel);
		JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(armyPanel);
		mainPanel.add(seedPanel);
		mainPanel.add(donePanel);
		frame = new JFrame();
		frame.add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addWindowListener(exitOnClose);
	}

	private void setRandomArmy(boolean attacker) {
		if (attacker) {
			attackerTextArea.setText(randomArmy());
		} else {
			defenderTextArea.setText(randomArmy());
		}
	}

	private String randomArmy() {
		String toReturn = "";
		Object[] cs = Unit.getTypes().keySet().toArray();
		for (int row = 0; row < Constants.START_AREA_HEIGHT; row++) {
			for (int col = 0; col < Constants.START_AREA_WIDTH; col++) {
				if (random.nextDouble() < 0.1) {
					toReturn += cs[random.nextInt(cs.length)];
				} else {
					toReturn += Constants.BLANK_SPACE_CHAR;
				}
			}
			if (row < Constants.START_AREA_HEIGHT - 1)
				toReturn += ls;
		}
		return toReturn;
	}

	private void updateCost(boolean attacker) {
		if (attacker)
			attackerCostDisplayArea.setText(String.valueOf(Battle.getCost(attackerTextArea.getText().split(ls))));
		else
			defenderCostDisplayArea.setText(String.valueOf(Battle.getCost(defenderTextArea.getText().split(ls))));
	}

	private void setBlankArmy(boolean attacker) {
		String blankArmy = blankArmy();
		if (attacker) {
			attackerTextArea.setText(blankArmy);
		} else {
			defenderTextArea.setText(blankArmy);
		}
	}

	private String blankArmy() {
		String toReturn = "";
		for (int row = 0; row < Constants.START_AREA_HEIGHT; row++) {
			for (int col = 0; col < Constants.START_AREA_WIDTH; col++)
				toReturn += Constants.BLANK_SPACE_CHAR;
			if (row < Constants.START_AREA_HEIGHT - 1)
				toReturn += ls;
		}
		return toReturn;
	}

	private void resultsPopup() {
		JPanel middlePanel = new JPanel();
		JScrollPane scroll = new JScrollPane(resultPane);
		resultPane.setFont(new Font(Constants.FONT, Font.PLAIN, 18));
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setPreferredSize(new Dimension(800, 800));
		middlePanel.add(scroll);
		JFrame frame = new JFrame();
		frame.add(middlePanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.addWindowListener(exitOnClose);
	}

	// https://stackoverflow.com/questions/3953208/value-change-listener-to-jtextfield
	/**
	 * Installs a listener to receive notification when the text of any
	 * {@code JTextComponent} is changed. Internally, it installs a
	 * {@link DocumentListener} on the text component's {@link Document}, and a
	 * {@link PropertyChangeListener} on the text component to detect if the
	 * {@code Document} itself is replaced.
	 * 
	 * @param text
	 *            any text component, such as a {@link JTextField} or
	 *            {@link JTextArea}
	 * @param changeListener
	 *            a listener to receieve {@link ChangeEvent}s when the text is
	 *            changed; the source object for the events will be the text
	 *            component
	 * @throws NullPointerException
	 *             if either parameter is null
	 */
	public static void addChangeListener(JTextComponent text, ChangeListener changeListener) {
		Objects.requireNonNull(text);
		Objects.requireNonNull(changeListener);
		DocumentListener dl = new DocumentListener() {
			private int lastChange = 0, lastNotifiedChange = 0;

			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				lastChange++;
				SwingUtilities.invokeLater(() -> {
					if (lastNotifiedChange != lastChange) {
						lastNotifiedChange = lastChange;
						changeListener.stateChanged(new ChangeEvent(text));
					}
				});
			}
		};
		text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
			Document d1 = (Document) e.getOldValue();
			Document d2 = (Document) e.getNewValue();
			if (d1 != null)
				d1.removeDocumentListener(dl);
			if (d2 != null)
				d2.addDocumentListener(dl);
			dl.changedUpdate(null);
		});
		Document d = text.getDocument();
		if (d != null)
			d.addDocumentListener(dl);
	}
}