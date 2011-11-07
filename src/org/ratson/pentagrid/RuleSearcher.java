package org.ratson.pentagrid;

import java.util.Arrays;
import java.util.LinkedList;

import org.ratson.pentagrid.fields.ArrayField;


/**Searches for he interesting rules*/
public class RuleSearcher {
	
	public final static int R_EXPONENTIAL = 1;
	public final static int R_STATIC = 2;
	public final static int R_DIE_OUT = 3;
	public final static int R_CHAOTIC = 4;
	public static final int R_CYCLIC = 5;
	private static final int R_CHAOTIC_GLIDERS = 6;
	
	public static final int NO_PERIOD = -1;
	
	class Result implements Comparable<Result>{
		int type;
		int steps;
		Result( int t, int s){ type = t; steps = s; };
		public String toString(){
			switch( type ){
			case R_EXPONENTIAL: return "Exp:"+steps;
			case R_STATIC: return "Static:"+steps;
			case R_DIE_OUT: return "Die:"+steps;
			case R_CHAOTIC: return "Chaos";
			case R_CYCLIC: return "Cycle("+steps+")";
			case R_CHAOTIC_GLIDERS: return "Chaos+Gliders!!!";
			}
			throw new RuntimeException( "Unknown result type");
		}
		
		@Override
		public int compareTo(Result r) {
			if ( r.type < type) return -1;
			if ( r.type > type) return 1;
			if (r.steps < steps ) return -1;
			if (r.steps > steps ) return 1;
			return 0;
		}
		@Override
		public boolean equals( Object t ){
			if ( ! (t instanceof Result) ) return false;
			Result r = (Result)(t);
			return compareTo(r) == 0;
		}
	}
	
	private void sortCells( Path[] cells ){
		Arrays.sort( cells, PathHashComparator.getInst());
	}
	
	Result tryRule( TotalisticRule r, int fieldSize ){
		double p = 0.5;
		
		ArrayField field = new ArrayField();
		field.setCells( Util.randomField( fieldSize, p) );
		Path[] soup = field.getAliveCellsArray(); //it returns reference
		sortCells( soup );
		
		int initialPopulation = soup.length;
		
		int limitPopulaion = initialPopulation * 10;
		int limitIteration = 500;
		int step = 0;
		
		while ( soup.length > 0 && soup.length < limitPopulaion && step < limitIteration){
			field.evaluate( r );
			Path[] soup1 = field.getAliveCellsArray();
			step += 1;
			sortCells( soup1 );
			if (Arrays.equals( soup1, soup )){//stabilized
				return new Result( R_STATIC, step );
			}
			soup = soup1;
		}
		//now analyze the situation.
		if ( soup.length == 0 ) return new Result( R_DIE_OUT, step );
		if ( soup.length >= limitPopulaion ) return new Result(R_EXPONENTIAL, step);
		//most interesting case.
		//Trying to detect cycle
		int period = detectCycle( field, r );
		if (period == NO_PERIOD){
			//search for the gliders
			if ( hasGliders( soup, fieldSize ))
				return new Result( R_CHAOTIC_GLIDERS, NO_PERIOD);
			else
				return new Result( R_CHAOTIC, NO_PERIOD );
		}
		else
			return new Result( R_CYCLIC, period );
	}
	
	private boolean hasGliders(Path[] soup, int fieldSize) {
		int maxLen = 0;
		for (int i = 0; i < soup.length; i++) {
			int len = soup[i].length();
			if( len > maxLen) maxLen = len;
		}
		return maxLen > fieldSize + 5;
	}

	Result[] tryRuleTimes( TotalisticRule r, int steps ){
		LinkedList<Result> res = new LinkedList<Result>();
		
		Result r0 = tryRule( r, 3 );//try rule on small radius to filter out exponentials quickly
		res.add( r0 );

		if ( r0.type != R_EXPONENTIAL ){
			for (int i = 0; i < steps; i++) {
				Result t = tryRule(r, 5);
				res.add( t );
				if (t.type == R_EXPONENTIAL) break;
			}
		}
		
		Result[] rval = new Result[res.size()];
		res.toArray(rval);
		Arrays.sort( rval );
		return rval;
	}

	private int detectCycle( Field field, TotalisticRule rule) {
		Path[] original = field.getAliveCellsArray();
		int populationLimit = original.length * 10;
		for( int i = 1; i < 24; ++ i){
			field.evaluate( rule );
			Path[] next = field.getAliveCellsArray();
			if (next.length > populationLimit)
				return NO_PERIOD;
			sortCells(next);
			if (Arrays.equals( next, original))
				return i;
		}
		return NO_PERIOD;
	}
	public static void main(String[] args) {
		RuleSearcher s = new RuleSearcher();
		s.searchAllRules();
	}
	
	boolean isInterestingReslut( Result[] res ){
		if ( res.length == 1 ) return false;
		for (int i = 0; i < res.length; i++) {
			int type = res[i].type;
			if ( type == R_CHAOTIC_GLIDERS )
				return true;
		}
		return false;
	}
	
	public void searchAllRules(){
		for( int ruleIndex = 0; ruleIndex < (1<<19); ++ ruleIndex ){
			TotalisticRule r = Rule.fromIndex(ruleIndex);
			Result[] res = tryRuleTimes(r, 20);
			//if ( isInterestingReslut(res) )
				System.out.println( "Rule:"+r+" " + Arrays.toString(res));
		}
	}
}
