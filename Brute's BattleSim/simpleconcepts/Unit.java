package simpleconcepts;

import java.util.HashMap;
import enums.Stat;
import pathfinding.Mover;
import unittypes.*;

/**
 * Depicts units on the battlefield. Does not do anything by itself simply
 * stores the variables. Subclasses are unit types.
 */

/*
 * For each subclass put the following line in the static block (where the parts
 * surrounded by % are not literals) or the program will attempt to exit ASAP:
 * Unit.addType(%icon%, %classname%.class);
 * 
 * @author InfuriatedBrute
 */
public abstract class Unit implements Mover {

	public Action currentAction = null;
	/** -1 = defender, 0 = not in a battle, 1 = attacker */
	public int attacker = 0;
	private int CURRENTHP;
	private int MAXHP;
	private int DMG;
	private int ARM;
	private int RNG;
	private int SPD;
	private int COST;
	private static final HashMap<Character, Class<? extends Unit>> charToUnitType;
	private static final HashMap<Class<? extends Unit>, Character> unitTypeToChar;

	static {
		charToUnitType = new HashMap<Character, Class<? extends Unit>>();
		unitTypeToChar = new HashMap<Class<? extends Unit>, Character>();
		Unit.addType('a', Archer.class);
		Unit.addType('b', Berserker.class);
		Unit.addType('D', Dragon.class);
		Unit.addType('G', Golem.class);
		Unit.addType('k', Knight.class);
		Unit.addType('m', Musketeer.class);
		Unit.addType('p', Pikeman.class);
		Unit.addType('r', Rat.class);
		Unit.addType('w', Warrior.class);
	}

	protected Unit(int MAXHP, int DMG, int ARM, int RNG, int SPD, int COST) {
		this.MAXHP = MAXHP;
		this.DMG = DMG;
		this.ARM = ARM;
		this.RNG = RNG;
		this.SPD = SPD;
		this.CURRENTHP = MAXHP;
		this.COST = COST;
	}

	public int get(Stat stat) {
		switch (stat) {
		case MAXHP:
			return MAXHP;
		case CURRENTHP:
			return CURRENTHP;
		case DMG:
			return DMG;
		case ARM:
			return ARM;
		case RNG:
			return RNG;
		case SPD:
			return SPD;
		case COST:
			return COST;
		default:
			throw new RuntimeException("Stat not found.");
		}
	}

	public void setHP(int hp) {
		this.CURRENTHP = hp;
	}

	public char getChar() {
		return unitTypeToChar.get(this.getClass());
	}

	@SuppressWarnings("unchecked")
	public static HashMap<Character, Class<? extends Unit>> getTypes() {
		return (HashMap<Character, Class<? extends Unit>>) charToUnitType.clone();
	}

	protected static void addType(char c, Class<? extends Unit> type) {
		if (charToUnitType.get(c) != null && charToUnitType.get(c) != type)
			throw new RuntimeException("Two units mapped to one character.");
		charToUnitType.put(c, type);
		unitTypeToChar.put(type, c);
	}
}
