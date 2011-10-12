package org.ratson.pentagrid;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule implements TotalisticRule {

	private boolean[] born = new boolean[11];
	private boolean[] live = new boolean[11];
	
	public Rule( int[] whenBorn, int[] whenLive){
		set( whenBorn, whenLive );
	}
	private void set(int[] whenBorn, int[] whenLive) {
		Arrays.fill(born, false);
		Arrays.fill(live, false);
		
		for (int i = 0; i < whenBorn.length; i++) {
			born[ whenBorn[i] ] = true;
		}
		for (int i = 0; i < whenLive.length; i++) {
			live[ whenLive[i] ] = true;
		}
	}
	/**Default rule B3/S23*/
	public Rule(){
		set( new int[]{3}, new int[]{2,3});
	}
	
	public static Rule parseRule( String rule ) throws RuleSyntaxException{
		Pattern ruleRegex = Pattern.compile("B([0-9Aa]*)/?S([0-9Aa]*)", Pattern.CASE_INSENSITIVE); //pattern to match the overall structure.
		Matcher m = ruleRegex.matcher(rule);
		if (! m.matches() ) throw new RuleSyntaxException( "Rule does not match syntax B.../S..." );
		String sBorn = m.group(1).toUpperCase();
		String sAlive = m.group(2).toUpperCase();
		
		Rule rval = new Rule( 
				parseRule_str2intArray(sBorn),
				parseRule_str2intArray(sAlive) );
		return rval;
	}
	/**Returns True, if vacuum (empty field) is stable under this rule*/
	public boolean isVacuumStable(){
		return born[0] == false;
	}
	/**Rturns True, i this rule is actuallt a Day/Night rule, when both vacuum and inverted vacuum are not stable*/
	public boolean isValidDayNight(){
		return (born[0] == true) && (live[10] == false);
	}
	//Pentagrid rule is described by the 19 bits of information.
	// 9 bits for born (0 is forbidden)
	// 10 bits for stay.
	public static TotalisticRule fromIndex( int idx ){
		assert( idx >= 0 && idx < 1<<19 );
		Rule r = new Rule();
		r.born[0] = false;
		for( int i =1; i < 10; ++i ){
			r.born[i] = (idx & 0x1) == 1;
			idx = idx >> 1;
		}
		for( int i =0; i < 10; ++i ){
			r.live[i] = (idx & 0x1) == 1;
			idx = idx >> 1;
		}
		return r;
	}
	
	/**Convert stringto the array of integers
	 * @throws RuleSyntaxException */
	private static int[] parseRule_str2intArray( String s ) throws RuleSyntaxException
	{
		int[] rval = new int[ s.length() ];
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ( c >= '0' && c <= '9' ){
				rval[i] = c - '0';
			}else if ( c == 'A' ){
				rval[i] = 10;
			}else throw new RuleSyntaxException("Rule contains unexpected character: " + c);
		}
		return rval;
	}
	
	/* (non-Javadoc)
	 * @see org.ratson.pentagrid.TotalisticRule#nextState(int, int)
	 */
	public int nextState(int prevState, int numNeighbores) {
		switch (prevState){
		case 1:
			return live[ numNeighbores ] ? 1 : 0;
		case 0:
			return born[ numNeighbores ] ? 1 : 0;
		default:
			throw new RuntimeException( "THis rule suports only 1 and 0 states");
		}
	}
	@Override
	public String toString() {
		return "B"+toString_arr2str( born ) + "/S" + toString_arr2str( live );
	}
	
	private static String toString_arr2str(boolean[] array) {
		String chars = "0123456789A";
		StringBuffer rval = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			if ( array[i])
				rval.append( chars.charAt(i));
		}
		return rval.toString();
	}
	/**Create new rule, that is equivalent to applying the original rule to the inverted field*/
	public Rule invertInputs(){
		Rule rval = new Rule();
		for( int i = 0; i < 11; ++i){
			//Important: born->live, live->born; it is not a mistake.
			rval.born[10-i] = live[i];
			rval.live[10-i] = born[i]; 
		}
		return rval;
	}
	/**Create new rule, that is equivalent to applying the original rule and inverting the result*/
	public Rule invertOutput(){
		Rule rval = new Rule();
		for( int i = 0; i < 11; ++i){
			rval.born[i] = ! born[i];
			rval.live[i] = ! live[i]; 
		}
		return rval;
	}
	/**Equivalent to the original rule, working in the inverted world*/
	public Rule invertBoth(){
		return invertInputs().invertOutput();
	}
	public void nextIteration() {}		//Doing nothing. Simple rules are not state-dependent
	public void resetState() {}
	public String getCode(){return toString();}
}
