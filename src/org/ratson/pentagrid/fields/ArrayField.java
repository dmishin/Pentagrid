package org.ratson.pentagrid.fields;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.TotalisticRule;

/**Record, used to store neighbores count*/
final class NeighborhoodRecord implements Serializable{
	public boolean isAlive = false;
	public int numNeighbores = 0;
	public NeighborhoodRecord( boolean alive ) {
		isAlive = alive;
	}
}

/**Simpliest implmentation, not using map. It does not allows to store cell state: all cells are either dead or alive*/ 
public final class ArrayField extends Field{
	private Path[] aliveCells = new Path[0];

	@Override
	public synchronized void evaluate(TotalisticRule r) {
		aliveCells = evaluate(aliveCells, r);
		r.nextIteration();//for the state-changing rules, update the state.
	}

	@Override
	public synchronized Path[] getAliveCellsArray() {
		return aliveCells;
	}

	@Override
	public synchronized void setCells(Iterable<Path> cells) {
		ArrayList<Path> newCells = new ArrayList<Path>();
		for( Path p : cells)newCells.add( p );
		aliveCells = new Path[ newCells.size() ];
		newCells.toArray( aliveCells );
	}
	
	@Override
	public synchronized void setCells( Path[] newCells ){
		aliveCells = newCells;
	}
	/**Calculate next step*/
	private Path[] evaluate( Path[] cells, TotalisticRule rule ){
		Map<Path, NeighborhoodRecord> withNeighbores = new HashMap<Path, NeighborhoodRecord>( ); //hash map is much (x3) faster in this task
		
		/*Fill initial state, without neighbores*/
		for (int i = 0; i < cells.length; i++) {
			withNeighbores.put(cells[i], new NeighborhoodRecord(true));
		}
		
		/*Now update neighbores counters*/
		for (int i = 0; i < cells.length; i++) {			
			Path[] neighbores = PathNavigation.neigh10(cells[i]);
			
			for (int j = 0; j < neighbores.length; j++) {
				Path neigh = neighbores[j];

				NeighborhoodRecord rec = withNeighbores.get( neigh );
				if (rec == null){
					rec = new NeighborhoodRecord(false);
					withNeighbores.put( neigh, rec);
				}
				rec.numNeighbores ++;
			}
		}

		/*Calculated. Now apply rule*/
		ArrayList<Path> newAlive = new ArrayList<Path>( cells.length );
		for (Entry<Path, NeighborhoodRecord> entry : withNeighbores.entrySet()) {
			NeighborhoodRecord rec = entry.getValue();
			if (rule.nextState( rec.isAlive?1:0, rec.numNeighbores ) != 0){
				newAlive.add( entry.getKey() );
			}
		}
		/*And convert the result to the array*/
		Path[] rval = new Path[ newAlive.size() ];
		newAlive.toArray( rval );
		return rval;
	}

	@Override
	public int population() {
		return aliveCells.length;
	}

	@Override
	public synchronized int getCell(Path cell) {
		for (int i = 0; i < aliveCells.length; i++) {
			if ( aliveCells[i].equals(cell)) return 1;
		}
		return 0;
	}

	@Override
	public synchronized void setCell(Path cell, int newState) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public synchronized Iterable<Path> getAliveCells() {
		return Arrays.asList(aliveCells);
	}
	
	public synchronized Field copy(){
		ArrayField rval = new ArrayField();
		rval.setCells( aliveCells );
		return rval;
	}
}