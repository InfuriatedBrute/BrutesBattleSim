package main;

/**
 * @author InfuriatedBrute
 */

import java.io.File;
import java.io.FileWriter;

public final class BattleTester extends BattleController {
	FileWriter fw;
	Battle b;
	File BattleResultsFile;

	public static void main(String[] args) {
		if (args.length > 0) {
			new BattleTester(args[0]);
		} else {
			new BattleTester("Default");
		}
	}

	private BattleTester(String filename) {
		File BattleSetupFile = new File("textfiles\\BattleSetups\\" + filename + ".txt");
		BattleResultsFile = new File("textfiles\\Battles\\" + filename + ".txt");
		try {
			(new FileWriter(BattleResultsFile)).close(); // clear the file
			fw = new FileWriter(BattleResultsFile, true);
			b = new Battle(BattleSetupFile, this);
			b.simulate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void endOfTurn(boolean lastTurn) throws Exception {
		String n = System.lineSeparator();
		if (b.actionPerformedThisTurn || b.turn == 1 || lastTurn) {
			if (lastTurn)
				fw.write(n + n + n + "BATTLE ENDED ON TURN " + b.turn + ": " + n);
			else
				fw.write(n + n + n + "Turn " + b.turn + ":" + n);
			for (int row = 0; row < b.MAP_HEIGHT; row++) {
				fw.write(n);
				for (int col = 0; col < b.MAP_WIDTH; col++) {
					if (b.grid[row][col] != null) {
						fw.write(b.grid[row][col].type.toString());
					} else {
						fw.write("~");
					}
				}
			}
		}
		if (lastTurn) {
			try {
				fw.flush();
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}