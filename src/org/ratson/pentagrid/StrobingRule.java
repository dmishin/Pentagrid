package org.ratson.pentagrid;

import java.io.Serializable;

public class StrobingRule implements TotalisticRule, Serializable {
	private static final long serialVersionUID = 1L;
	private Rule baseRule;
	private Rule[] subRules;
	
	@Override
	public int nextState(int worldState, int prevState, int numNeighbores) {
		return subRules[ worldState ].nextState( 0, prevState, numNeighbores);
	}
	
	public StrobingRule( Rule baseRule ){
		assert baseRule.isValidDayNight();
		this.baseRule = baseRule;
		subRules = new Rule[]{ 
				baseRule.invertOutput(),
				baseRule.invertInputs() };
	}

	@Override
	public String toString() {
		return "Strobing{"+baseRule+"}";  
	}
	public boolean isValidRule(){
		return subRules[0].isVacuumStable() && subRules[1].isVacuumStable();
	}
	public String getCode(){ return baseRule.getCode(); }

	@Override
	public int nextFieldState(int prevFieldState) {
		switch( prevFieldState ){
		case 0: return 1;
		case 1: return 0;
		default: throw new RuntimeException("Bad world state:"+prevFieldState);
		}
	}

	/**Returns simple rule, that is equivalent to the DayNight rule*/ 
	public Rule getUnderlyingRule() {
		return baseRule;
	}
}
