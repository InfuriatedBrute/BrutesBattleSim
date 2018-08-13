package main;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import enums.Stat;
import pathfinding.AStarPathFinder;
import pathfinding.ClosestHeuristic;
import pathfinding.Mover;
import pathfinding.TileBasedMap;
import simpleconcepts.Action;
import simpleconcepts.Point;
import simpleconcepts.Unit;

/**
 * The top-level tactical class. Handles just about all model aspects of a
 * simulation. Also handles all model aspects of a replay; battles with
 * identical setup (including the RNG seed) will always play out identically
 * 
 * @author InfuriatedBrute
 */

public class Battle implements TileBasedMap {
	Random random;
	int turn = 1;
	List<String> saidList = new ArrayList<String>();
	Unit[][] grid = new Unit[Constants.MAP_HEIGHT][Constants.MAP_WIDTH];
	boolean[][] pathFinderVisited = new boolean[Constants.MAP_HEIGHT][Constants.MAP_WIDTH];
	BattleController battleController;
	boolean actionPerformedThisTurn = false;
	AStarPathFinder pf;

	public Battle(String[] attackerArmy, String[] defenderArmy, Long RNGseed, BattleController battleController) {
		this.battleController = battleController;
		random = new Random(RNGseed);
		pf = new AStarPathFinder(this, Constants.MAX_SEARCH_DISTANCE, true, new ClosestHeuristic(), random);
		addUnitsOfBothSides(attackerArmy, defenderArmy);
	}

	private void addUnitsOfBothSides(String[] attackerArmy, String[] defenderArmy) {
		assert (Constants.START_AREA_CAN_BE_CENTERED);
		addUnitsOfOneSide(attackerArmy, true);
		addUnitsOfOneSide(defenderArmy, false);
	}

	private void addUnitsOfOneSide(String[] army, boolean attacker) {
		assert (army.length == Constants.START_AREA_HEIGHT);
		int row = 0;
		for (String line : army) {
			assert (line.length() == Constants.START_AREA_WIDTH );
			int col = 0;
			for (char c : line.toCharArray()) {
				if (c != Constants.BLANK_SPACE_CHAR) {
					Unit u;
					try {
						u = (Unit.getTypes().get(c)).getConstructor().newInstance();
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException();
					}
					u.attacker = attacker ? 1 : -1;
					int x = row + ((Constants.MAP_HEIGHT - Constants.START_AREA_HEIGHT) / 2);
					int y = attacker ? col : Constants.MAP_WIDTH - col - 1;
					grid[x][y] = u;
				}
				col++;
			}
			row++;
		}
	}

	/**
	 * 
	 * @return 1 for attacker victory, 0 for stalemate, -1 for defender victory
	 */
	public int simulate() {
		try {
			while (turn <= Constants.TURN_LIMIT && oneSideEmpty() == 0) {
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
				battleController.endOfTurn();
				turn++;
				actionPerformedThisTurn = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return oneSideEmpty();
	}

	/**
	 * 
	 * @return 1 if defenders empty, 0 if neither side empty, -1 if attackers empty
	 */
	private int oneSideEmpty() {
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
		if(attackers == 0 && defenders == 0)
			throw new RuntimeException("Both attackers and defenders are dead. This should be impossible.");
		if(defenders == 0)
			return 1;
		if(attackers == 0)
			return -1;
		return 0;
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
		Point enemyPointInAttackRange = null;
		Point p = getLocationOf(u);
		ArrayList<Integer> xList = new ArrayList<>();
		ArrayList<Integer> yList = new ArrayList<>();
		for (int x = 0; x < grid.length; x++)
			xList.add(x);
		for (int y = 0; y < grid[0].length; y++)
			yList.add(y);
		Collections.shuffle(xList, random);
		Collections.shuffle(yList, random);
		for (int x : xList) {
			for (int y : yList) {
				Unit u2 = grid[x][y];
				if (u2 != null && u2.attacker == u.attacker * -1) { // they are enemies and obviously not duplicates
					if (enemyPointInAttackRange == null
							&& ((Math.abs(p.x - x) + Math.abs(p.y - y)) > u.get(Stat.RNG))) { // no enemies in
																								// range
						Point enemyPoint = new Point(x, y);
						pathfinding.Path path = pathfind(u, enemyPoint, u.get(Stat.RNG));
						if (path != null) {
							int pathDistance = path.getLength();
							if (pathDistance < closestEnemyDist) {
								closestEnemyDist = pathDistance;
								closestEnemyPoint = enemyPoint;
							}
						}
					} else if (((Math.abs(p.x - x) + Math.abs(p.y - y)) <= u.get(Stat.RNG))) {
						enemyPointInAttackRange = new Point(x, y);
					}
				}
			}
		}
		if (enemyPointInAttackRange == null && closestEnemyPoint != null) {
			u.currentAction = new Action("Move", closestEnemyPoint);
		} else if (enemyPointInAttackRange != null) {
			u.currentAction = new Action("Attack", enemyPointInAttackRange);
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
			 */

	// return null if no way to reach point, or point within leniency of point
	// note that the leniency area will be square-shaped because of diagonal
	// movement, plus-shaped would be for orthogonal movement
	private pathfinding.Path pathfind(Unit unit, Point target, int leniency) {
		int deviation = 0;
		pathfinding.Path optimalPath = null;
		while (deviation <= leniency) {
			ArrayList<Integer> xDevList = new ArrayList<>();
			ArrayList<Integer> yDevList = new ArrayList<>();
			for (int xDev = deviation * -1; xDev <= deviation; xDev++)
				xDevList.add(xDev);
			for (int yDev = deviation * -1; yDev <= deviation; yDev++)
				yDevList.add(yDev);
			Collections.shuffle(xDevList, random);
			Collections.shuffle(yDevList, random);
			for (int xDev : xDevList) {
				for (int yDev : yDevList) {
					int tx = target.x + xDev;
					int ty = target.y + yDev;
					// if we're not checkingOuter don't bother we've already checked those points
					boolean checkingOuterPartsOfCurrentDeviation = xDev == deviation || xDev == deviation * -1
							|| yDev == deviation || yDev == deviation * -1;
					boolean combinedXYDevLessThanLeniency = Math.abs(xDev) + Math.abs(yDev) <= leniency;
					boolean pointInMapBoundaries = (tx >= 0 && tx < Constants.MAP_HEIGHT)
							&& (ty > 0 && ty < Constants.MAP_WIDTH);
					if (checkingOuterPartsOfCurrentDeviation && combinedXYDevLessThanLeniency && pointInMapBoundaries) {
						Point uLocation = getLocationOf(unit);
						pathfinding.Path path = pf.findPath(unit, uLocation.x, uLocation.y, tx, ty);
						if (path != null) {
							if (optimalPath == null || path.getLength() < optimalPath.getLength()) {
								optimalPath = path;
							}
						}
					}
				}
			}
			deviation++;
		}
		return optimalPath;
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
			return Constants.NO_ACTION_STUN_VALUE; // else
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
		double unroundedTicksNeeded = Constants.BASE_MOVE_TIME / u.get(Stat.SPD);
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
			if ((Math.abs(p.x - tx) + Math.abs(p.y - ty)) <= unit.get(Stat.RNG) && enemy != null) {
				if (enemy.get(Stat.DMG) / unit.get(Stat.MAXHP) + stunValue(enemy) >= Constants.STUN_THRESHOLD) {
					// crit
					enemy.currentAction = null;
					damage(enemy, unit.get(Stat.DMG));
				} else {
					damage(enemy, unit.get(Stat.DMG) - enemy.get(Stat.ARM));
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
		int plusOrMinusOne = random.nextInt(3) - 1;
		int newHP = u.get(Stat.CURRENTHP) - dmg + plusOrMinusOne;
		if (newHP < 0)
			kill(u);
		else
			u.setHP(newHP);
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

	public static int getCost(String[] army) {
		int toReturn = 0;
		for (String line : army) {
			for (char c : line.toCharArray()) {
				if (c != Constants.BLANK_SPACE_CHAR) {
					Class<? extends Unit> cl = Unit.getTypes().get(c);
					if (cl != null) {
						try {
							toReturn += cl.getConstructor().newInstance().get(Stat.COST);
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException();
						}
					}
				}
			}
		}
		return toReturn;
	}
}