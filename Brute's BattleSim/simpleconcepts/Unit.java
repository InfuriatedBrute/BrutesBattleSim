package simpleconcepts;

import pathfinding.Mover;
import types.Stat;
import types.UnitType;

/**
 * Depicts units on the battlefield. Does not do anything by itself simply
 * stores the variables
 * 
 * @author InfuriatedBrute
 */
public class Unit implements Mover {
	public UnitType type;
	public int hp;
	public Action currentAction = null;
	/** -1 = defender, 0 = not in a battle, 1 = attacker */
	public int attacker = 0;

	public Unit(UnitType type) {
		this.type = type;
		hp = Integer.valueOf(type.get(Stat.HP));
	}
}
