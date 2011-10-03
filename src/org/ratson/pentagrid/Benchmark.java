package org.ratson.pentagrid;

import java.util.Arrays;

import org.ratson.pentagrid.fields.ArrayField;
import org.ratson.pentagrid.fields.SimpleMapField;

public class Benchmark {

	private static int initialFldSize = 7; 
	
	private static void doBenchmark( String name, Runnable task, int attempts){
		double[] times = new double[ attempts ];
		System.out.println("Running task "+name+" for "+attempts+" times");
		for( int i = 0; i < attempts; ++ i){
			long startTime = System.currentTimeMillis();
			task.run();
			double dt = 0.001*(System.currentTimeMillis() - startTime);
			times[ i ] = dt;
			System.out.println( ""+i+") "+dt );
		}
		Arrays.sort( times );
		System.out.println("Sorted:"+Arrays.toString(times));
	}
	
	private static Runnable makeTest( final Field field, final Rule r, final int tries, final int steps ){
		return new Runnable(){ 
			@Override
			public void run(){
				for( int j = 0; j < tries; ++j){
					field.setCells( Util.randomField( initialFldSize, 0.5) );
					for (int i = 0; i < steps; ++i ){
						field.evaluate( r );
					}
				}
			}
		}; 
	}

	public static void main(String[] args) throws RuleSyntaxException {
		int nBench = 5;
		Rule r = Rule.parseRule("B3S235");
		int tries = 10;
		int steps = 100;
		
		doBenchmark( "Afld", makeTest( new ArrayField(), r, tries, steps), nBench );			
		doBenchmark( "SimpleMap", makeTest( new SimpleMapField(), r, tries, steps), nBench );			
	}
}
