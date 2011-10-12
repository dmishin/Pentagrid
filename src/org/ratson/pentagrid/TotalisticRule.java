package org.ratson.pentagrid;

public interface TotalisticRule {
	/**Calculate next state from the current and the sum of neighbores*/
	public abstract int nextState(int prevState, int numNeighbores);
	/**Support for the rules hat change their state*/
	public abstract void nextIteration();
	/**For the rules that has internal state, resets it.*/
	public abstract void resetState();
	/**Return rule code string*/
	public String getCode();
}