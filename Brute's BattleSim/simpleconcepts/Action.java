package simpleconcepts;

/**
 * Defines what a unit is doing, where it is doing it to, and how long it has
 * been doing that for. Does not do anything by itself, simply stores variables.
 * 
 * @author InfuriatedBrute
 */
public class Action {
	/**
	 * What, i.e. move/attack/say/wait. Valid options are as follows : ATTACKING.
	 * MOVING. SAYING:"". Saying is dual-purpose, both printing to allied SaidList
	 * and casting a spell if the syntax matches.
	 */
	public final String description;
	/** Where it is targetting with this action */
	public final Point p;
	/**
	 * number of ticks complete; an action is complete when 100/SPD = ticks, rounded
	 * randomly using the battle seed. 20 SPD units take 5 ticks to complete actions.
	 */
	public int ticks = 0;

	/**
	 * Note that words have no meaning if the actiontype is not SAYING.
	 */
	public Action(String description, Point p) {
		this.description = description;
		assert (description.equals("Attack") || description.equals("Move"));
		this.p = p;
	}
}