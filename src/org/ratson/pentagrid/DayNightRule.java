package org.ratson.pentagrid;

import java.io.Serializable;

public class DayNightRule implements TotalisticRule, Serializable {

	private Rule baseRule;
	private Rule[] subRules;
	private int state = 0;
	
	@Override
	public int nextState(int prevState, int numNeighbores) {
		return subRules[state].nextState(prevState, numNeighbores);
	}
	
	public DayNightRule( Rule baseRule ){
		assert baseRule.isValidDayNight();
		this.baseRule = baseRule;
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
		return "DayNight{"+baseRule+"} " + ( state==0? "NORM" : "INV" ) + "="+subRules[state];  
	}

	public void resetState() {
		state = 0;
	}
	public boolean isValidRule(){
		return subRules[0].isVacuumStable() && subRules[1].isVacuumStable();
	}
	public String getCode(){ return baseRule.getCode(); };
}
