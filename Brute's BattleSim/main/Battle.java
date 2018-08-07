package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import pathfinding.AStarHeuristic;
import pathfinding.AStarPathFinder;
import pathfinding.ClosestHeuristic;
import pathfinding.Mover;
import pathfinding.TileBasedMap;
import simpleconcepts.Action;
import simpleconcepts.Point;
import simpleconcepts.Unit;
import types.Attribute;
import types.Stat;
import types.UnitType;

/**
 * The top-level tactical class. Handles just about all model aspects of a
 * simulation. Also handles all model aspects of a replay; battles with
 * identical setup (including the RNG seed) will always play out identically
 * @author InfuriatedBrute
 */

public class Battle implements TileBasedMap {
	public static final int MAX_SEARCH_DISTANCE = 100;
	public static final double STUN_THRESHOLD = 1.10;
	public static final int MAP_WIDTH = 50;
	public static final int MAP_HEIGHT = 10;
	public static final int START_AREA_WIDTH = 6;
	public static final int START_AREA_HEIGHT = 6;
	public static final int TURN_LIMIT = 1000;
	public static final boolean START_AREA_CAN_BE_CENTERED = START_AREA_HEIGHT % 2 == MAP_HEIGHT % 2;
	private static final double NO_ACTION_STUN_VALUE = -0.10;
	private static final int BASE_MOVE_TIME = 10;
	Random random;
	int turn = 1;
	List<String> saidList = new ArrayList<String>();
	Unit[][] grid = new Unit[MAP_HEIGHT][MAP_WIDTH];
	boolean[][] pathFinderVisited = new boolean[MAP_HEIGHT][MAP_WIDTH];
	BattleController battleController;
	boolean actionPerformedThisTurn = false;
	AStarPathFinder pf = new AStarPathFinder(this, MAX_SEARCH_DISTANCE, true, new ClosestHeuristic());

	public Battle(File file, BattleController battleController)
			throws FileNotFoundException, IOException, NumberFormatException {
		this.battleController = battleController;
		Scanner sc = new Scanner(file);
		String line = sc.nextLine();
		assert (line.indexOf("Attacker: ") == 0);
		File attackerFile = new File("textfiles\\Armies\\" + line.substring(line.indexOf(" ") + 1) + ".txt");
		line = sc.nextLine();
		assert (line.indexOf("Defender: ") == 0);
		File defenderFile = new File("textfiles\\Armies\\" + line.substring(line.indexOf(" ") + 1) + ".txt");
		line = sc.nextLine();
		assert (line.indexOf("Seed: ") == 0);
		random = new Random(Long.parseLong(line.substring(line.indexOf(" ") + 1)));
		addUnitsOfBothSides(attackerFile, defenderFile);
		sc.close();
	}

	private void addUnitsOfBothSides(File attackerFile, File defenderFile) throws FileNotFoundException {
		assert (START_AREA_CAN_BE_CENTERED);
		addUnitsOfOneSide(attackerFile, true);
		addUnitsOfOneSide(defenderFile, false);
	}

	private void addUnitsOfOneSide(File file, boolean attacker) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		int row = 0;

		while (sc.hasNextLine()) {
			assert (row < START_AREA_HEIGHT);
			String line = sc.nextLine();
			int column = 0;
			for (char c : line.toCharArray()) {
				assert (column < START_AREA_WIDTH);
				if (c != '~') {
					Unit u = new Unit(getUnitTypeByCharacter(c));
					u.attacker = attacker ? 1 : -1;
					int x = row + ((MAP_HEIGHT - START_AREA_HEIGHT) / 2);
					int y = attacker ? column : MAP_WIDTH - column - 1;
					grid[x][y] = u;
				}
				column++;
			}
			row++;
		}
		sc.close();
	}

	private UnitType getUnitTypeByCharacter(char c) {
		UnitType toReturn = null;
		File dir = new File("textfiles\\UnitTypes\\");
		String[] unitTypeFiles = dir.list();
		for (String s : unitTypeFiles) {
			UnitType ut = new UnitType(s.substring(0, s.lastIndexOf(".txt")));
			char[] ca = ut.get(Attribute.ICON).toCharArray();
			if (ca.length > 1) {
				throw new RuntimeException("A unit type's icon is more than one character.");
			} else if (ca[0] == c) {
				if (toReturn == null) {
					toReturn = ut;
				} else {
					throw new RuntimeException("Two unit types share the same character.");
				}
			}
		}
		return toReturn;
	}

	public void simulate() {
		try {
			while (turn <= TURN_LIMIT && !oneSideEmpty()) {
				ArrayList<Unit> queued = new ArrayList<Unit>();
				for (Unit[] ua : grid) {
					for (Unit u : ua) {
						if (u != null) {
							queued.add(u);
						}
					}
				}
				Collections.shuffle(queued, random);
				for (Unit u : queued)
					tick(u);
				battleController.endOfTurn(false);
				turn++;
				actionPerformedThisTurn = false;
			}
			if (turn > TURN_LIMIT)
				turn--;
			battleController.endOfTurn(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean oneSideEmpty() {
		int attackers = 0;
		int defenders = 0;
		for (Unit[] ua : grid) {
			for (Unit u : ua) {
				if (u == null)
					;
				else if (u.attacker == 1)
					attackers++;
				else if (u.attacker == -1)
					defenders++;
				else
					throw new RuntimeException("There's a neutral unit on the field. How did this happen?");
			}
		}
		return (attackers == 0 || defenders == 0);
	}

	/**
	 * If no targets are in range queue an action to move within range as quickly as
	 * possible. Else queue an action to attack a random enemy.
	 * 
	 * @param u
	 *            the unit queueing the action
	 */
	private void moveAttackNearestEnemy(Unit u) {
		int closestEnemyDist = Integer.MAX_VALUE;
		Point closestEnemyPoint = null;
		ArrayList<Point> enemyPointsInAttackRange = new ArrayList<>();
		Point p = getLocationOf(u);
		for (int x = 0; x < grid.length; x++)
			for (int y = 0; y < grid[x].length; y++) {
				Unit u2 = grid[x][y];
				if (u2 != null && u2.attacker == u.attacker * -1) { // they are enemies and obviously not duplicates
					if (enemyPointsInAttackRange.isEmpty()
							&& ((Math.abs(p.x - x) + Math.abs(p.y - y)) > u.type.get(Stat.RNG))) { // no enemies in
																									// range
						Point enemyPoint = new Point(x, y);
						pathfinding.Path path = pathfind(u, enemyPoint, u.type.get(Stat.RNG));
						if (path != null) {
							int pathDistance = path.getLength();
							if (pathDistance < closestEnemyDist) {
								closestEnemyDist = pathDistance;
								closestEnemyPoint = enemyPoint;
							}
						}
					} else if (((Math.abs(p.x - x) + Math.abs(p.y - y)) <= u.type.get(Stat.RNG))) {
						enemyPointsInAttackRange.add(new Point(x, y));
					}
				}
			}
		if (enemyPointsInAttackRange.isEmpty() && closestEnemyPoint != null) {
			u.currentAction = new Action("Move", closestEnemyPoint);
		} else if (!enemyPointsInAttackRange.isEmpty()) {
			u.currentAction = new Action("Attack",
					enemyPointsInAttackRange.get(random.nextInt(enemyPointsInAttackRange.size())));
		}
	}

	private Point getLocationOf(Unit u) {
		for (int row = 0; row < grid.length; row++)
			for (int col = 0; col < grid[row].length; col++)
				if (grid[row][col] != null && u.equals(grid[row][col]))
					return new Point(row, col);
		throw new RuntimeException("Unit not found.");
	}

	/*	*//**
			 * Performs the order for the given unit, managing most of the implications of
			 * said action. Every unit on the battlefield has this method run every turn
			 * 
			 * 
			 * @param unit
			 *            The unit whose orders are to be performed
			 * @throws IllegalArgumentException
			 *             if the unit's order file does not follow the proper syntax
			 * @throws InvalidPathException
			 *             generally if the unit's name contains an escape character such as
			 *             ".", but in case of an unforeseen error there may be other causes
			 *//*
				 * private void parseOrder(Unit unit) throws IllegalArgumentException,
				 * InvalidPathException { String unitTypeName = unit.type.get(Stat.NAME); Path
				 * path = Paths.get("textfiles\\UnitTypes\\" + unitTypeName + ".txt"); if
				 * (unit.currentAction != null) { // actions cannot be cancelled, so just
				 * continue preparing the action tickAction(unit); } else { try { if
				 * (!Files.exists(path)) Files.write(path, new ArrayList<String>());
				 * List<String> lines = Files.readAllLines(path); boolean unitFound = false; int
				 * x = 0; int y = 0; while(x < grid.length && !unitFound) { while(y <
				 * grid.length && !unitFound) { if(grid[x][y].equals(unit))unitFound=true; else
				 * y++; } x++; } if (!unitFound) throw new
				 * RuntimeException("Unit whose orders are being parsed is not on the map.");
				 * for (int i = 0; i < lines.size(); i++) { } } catch (IOException e) {
				 * System.out.println("This shouldn't be possible, critical error");
				 * e.printStackTrace(); } } }
				 */

	/*
	 * attacker = whether the unit is an attacker, thus enemies would be the
	 * defender, hence the ? -1 : 1 below
	 * 
	 * private Point nearestPointWithinLeniencyWithEnemy(boolean attacker, Point p,
	 * int leniency) { List<Unit> enemies = new ArrayList<Unit>(); for (Unit[] ua :
	 * grid) for (Unit u : ua) if (u != null && u.attacker == (attacker ? -1 : 1))
	 * enemies.add(u); Unit closestEnemy = null; int closestEnemyDistance =
	 * Integer.MAX_VALUE; for (Unit enemy : enemies) { if
	 * (enemy.point.distanceFrom(p) < closestEnemyDistance) { closestEnemy = enemy;
	 * closestEnemyDistance = enemy.point.distanceFrom(p); } } if (closestEnemy ==
	 * null || closestEnemyDistance < leniency) return null; // else return
	 * closestEnemy.point; }
	 */

	// return null if no way to reach point, or point within leniency of point
	// note that the leniency area will be square-shaped because of diagonal
	// movement, plus-shaped would be for orthogonal movement
	private pathfinding.Path pathfind(Unit u, Point p, int leniency) {
		int deviation = 0;
		pathfinding.Path shortestPath = null;
		while (deviation <= leniency) {
			for (int xDev = deviation * -1; xDev <= deviation; xDev++) {
				for (int yDev = deviation * -1; yDev <= deviation; yDev++) {
					int tx = p.x + xDev;
					int ty = p.y + yDev;
					if ((xDev == deviation || xDev == deviation * -1 || yDev == deviation || yDev == deviation * -1)
							&& tx >= 0 && tx < MAP_HEIGHT && ty > 0 && p.y + yDev < MAP_WIDTH) {
						p = getLocationOf(u);
						pathfinding.Path path = pf.findPath(u, p.x, p.y, tx, ty);
						if (path != null && (shortestPath == null || shortestPath.getLength() > path.getLength()))
							shortestPath = path;
					} // else don't bother we've already checked those points
				}
			}
			deviation++;
		}
		return shortestPath;
	}

	private void tick(Unit unit) {
		boolean unitOnGrid = false;
		for (Unit[] ua : grid)
			for (Unit u : ua)
				if (unit.equals(u))
					unitOnGrid = true;
		if (!unitOnGrid)
			return;
		if (unit.currentAction == null)
			moveAttackNearestEnemy(unit);
		if (unit.currentAction != null)
			tickAction(unit);
		// else if it still has no action there's simply nothing it can do.
	}

	/*
	 * private Point parsePoint(String substring) throws IllegalArgumentException {
	 * //Auto-generated method stub return null; }
	 * 
	 * private boolean conditionHoldsTrue(Unit unit, String condition) throws
	 * IllegalArgumentException { //Auto-generated method stub return false; }
	 */

	/**
	 * Continues moving towards the unit's queued action. Assumes there is an
	 * action. Does not process leniency; the action will continue ticking until
	 * completion even if it becomes impossible.
	 * 
	 * @param unit
	 *            The unit whose action is to be ticked.
	 */
	private void tickAction(Unit u) {
		assert (u.currentAction != null) : "Contract violated.";
		u.currentAction.ticks += 1;
		if (actionCompleteness(u) >= 1) {
			performAction(u);
			u.currentAction = null;
		}
	}

	public double stunValue(Unit u) {
		if (u.currentAction == null)
			return NO_ACTION_STUN_VALUE; // else
		return actionCompleteness(u);
	}

	/**
	 * Uses the RNG seed to determine whether the BASE_MOVE_TIME/SPD check will be
	 * rounded up or down; i.e. 0.01 will be rounded down 99% of the time
	 * 
	 * @param u
	 *            the unit whose actioncompleteness is returned
	 * @return how close this unit is to completing its action, as a double where
	 *         1.0 is 100%
	 */
	private double actionCompleteness(Unit u) {
		assert (u.currentAction != null);
		double unroundedTicksNeeded = BASE_MOVE_TIME / u.type.get(Stat.SPD);
		double roundedTicksNeeded = Math.floor(unroundedTicksNeeded);
		if (unroundedTicksNeeded % 1.0 > random.nextDouble())
			roundedTicksNeeded++;
		return (double) u.currentAction.ticks / roundedTicksNeeded;
	}

	@Override
	public int getWidthInTiles() {
		return grid.length;
	}

	@Override
	public int getHeightInTiles() {
		return grid[0].length;
	}

	@Override
	public void pathFinderVisited(int x, int y) {
		pathFinderVisited[x][y] = true;
	}

	@Override
	public boolean blocked(Mover mover, int x, int y) {
		return grid[x][y] != null;
	}

	@Override
	public float getCost(Mover mover, int sx, int sy, int tx, int ty) {
		return 1;
	}

	private void performAction(Unit unit) {
		int tx = unit.currentAction.p.x;
		int ty = unit.currentAction.p.y;
		Point p = getLocationOf(unit);
		if (unit.currentAction.description.equals("Attack")) {
			Unit enemy = grid[tx][ty];
			if ((Math.abs(p.x - tx) + Math.abs(p.y - ty)) <= unit.type.get(Stat.RNG) && enemy != null) {
				if (enemy.type.get(Stat.HP) / unit.type.get(Stat.DMG) + stunValue(enemy) >= STUN_THRESHOLD) {
					// crit
					enemy.currentAction = null;
					damage(enemy, unit.type.get(Stat.DMG));
				} else {
					damage(enemy, unit.type.get(Stat.DMG) - enemy.type.get(Stat.ARM));
				}
			} else {
				moveOneStepTowards(unit, new Point(tx, ty));
			}
		} else if (unit.currentAction.description.equals("Move")) {
			moveOneStepTowards(unit, new Point(tx, ty));
		} else {
			throw new RuntimeException(
					"Action type asked to perform not recognized: " + unit.currentAction.description);
		}
		actionPerformedThisTurn = true;
	}

	public void damage(Unit u, int dmg) {
		u.hp -= dmg;
		if (u.hp < dmg)
			kill(u);
	}

	public void kill(Unit u) {
		Point p = getLocationOf(u);
		grid[p.x][p.y] = null;
	}

	public void moveOneStepTowards(Unit u, Point p) {
		pathfinding.Path path = pathfind(u, p, 1);
		Point origin = getLocationOf(u);
		if (path != null) {
			assert (path.getLength() > 1);
			int x = path.getX(1);
			int y = path.getY(1);
			assert (!blocked(u, x, y)) : "Pathfinding is leading a unit into a blocked space. How could this happen?";
			grid[origin.x][origin.y] = null;
			grid[x][y] = u;
		}
	}

	// likely useless but here for potential reference
	// private void setOrder(Unit unit, List<String> s) {
	// try {
	// Files.write(Paths.get("textfiles\\UnitTypes\\" + unit.type.get(Stat.NAME) +
	// ".txt"), s);
	// } catch (IOException e) {
	// System.out.println("This shouldn't be possible, critical error");
	// e.printStackTrace();
	// }
	// }
}