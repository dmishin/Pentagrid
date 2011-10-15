package org.ratson.pentagrid;

public interface TotalisticRule {
	/**Calculate next state from the current and the sum of neighbores*/
	public abstract int nextState(int worldState, int prevState, int numNeighbores);
	/**For the rules that depend on the global field state, update it*/
	public int nextFieldState( int prevFieldState );
	/**Return rule code string*/
	public String getCode();
}