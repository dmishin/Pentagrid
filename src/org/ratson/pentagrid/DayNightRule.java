package org.ratson.pentagrid;

public class DayNightRule implements TotalisticRule {

	private Rule[] subRules = null;
	private int state = 0;
	
	@Override
	public int nextState(int prevState, int numNeighbores) {
		return subRules[state].nextState(prevState, numNeighbores);
	}
	
	public DayNightRule( Rule baseRule ){
		subRules = new Rule[]{ 
				baseRule.invertOutput(),
				baseRule.invertInputs() };
	}

	@Override
	public void nextIteration() {
		state = (state + 1)%2;
	}
	
	@Override
	public String toString() {
		return "DayNight{"+subRules[0]+"; "+subRules[1]+"}";
	}

	public void resetState() {
		state = 0;
	}
	public boolean isValidRule(){
		return subRules[0].isVacuumStable() && subRules[1].isVacuumStable();
	}
	public String getCode(){ return subRules[0].invertOutput().getCode(); };
}
