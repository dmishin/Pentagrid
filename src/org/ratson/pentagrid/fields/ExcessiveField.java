package org.ratson.pentagrid.fields;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Rule;
import org.ratson.util.LazyResult;

class CellRecordWithCounter extends CellRecord implements Serializable{
	public int deadAgeCounter=0;
	public CellRecordWithCounter( int state ){
		super( state );
	}
}

/**This field implementation constantly stores cells in the zero state, that have alive neighbores.
 * Hopefully this should increase performance. Not works really.
 */
public final class ExcessiveField extends Field{
	Map<Path, CellRecordWithCounter> data = new HashMap<Path, CellRecordWithCounter>();
	public int deadAgeLimit = 1;
	@Override
	public synchronized void evaluate(Rule r) {
		//put all neighbores
		Map<Path, CellRecordWithCounter> newCells = new HashMap<Path, CellRecordWithCounter>();
		for( Entry<Path, CellRecordWithCounter> e : data.entrySet()){
			if ( e.getValue().state == 0 ) continue;
			Path[] neigh = PathNavigation.neigh10( e.getKey() );
			for (int i = 0; i < neigh.length; i++) {
				Path n = neigh[i];
				CellRecordWithCounter rec = data.get( n );
				if (rec == null) rec = newCells.get( n );
				if (rec == null ){
					rec = new CellRecordWithCounter(0);
					newCells.put( n, rec );
				}
				rec.sum += e.getValue().state;
			}
		}
		//neighbores updated. Now put all new cells to the main dictionary
		data.putAll(newCells);
		newCells = null;
		//now update cell state, and ermove all dead cells that are too old
		for( Iterator<Entry<Path, CellRecordWithCounter>> iCell = data.entrySet().iterator(); iCell.hasNext(); ){
			Entry<Path, CellRecordWithCounter> e = iCell.next();
			CellRecordWithCounter rec = e.getValue();
			rec.state = r.nextState(rec.state, rec.sum);
			rec.sum = 0;
			if (rec.state != 0){
				rec.deadAgeCounter = 0;
				continue;//cell is alive, nothing more to do.
			}
			//cell is dead.
			if (rec.sum > 0) //it has alive nieghbores
				rec.deadAgeCounter = 0;
			else{
				rec.deadAgeCounter ++;
				if( rec.deadAgeCounter > deadAgeLimit ) //this cell is being dead too long. Bye-bye.
					iCell.remove();
			}
		}
	}

	@Override
	public Path[] getAliveCellsArray() {
		ArrayList<Path> cells = new ArrayList<Path>();
		for( Entry<Path, CellRecordWithCounter> e : data.entrySet()){
			if (e.getValue().state != 0)
				cells.add(e.getKey());
		}
		return cells.toArray(new Path[0]);
	}

	@Override
	public void setCells(Iterable<Path> cells) {
		data = new HashMap<Path, CellRecordWithCounter>();
		for( Path p : cells) {
			data.put( p, new CellRecordWithCounter(1));
		}
	}

	@Override
	public int population() {
		int cnt = 0;
		for( CellRecordWithCounter r : data.values()){
			if (r.state != 0) cnt ++;
		}
		return cnt;
	}
	
	@Override
	public int getCell(Path cell) {
		CellRecord rec = data.get( cell );
		if (rec == null)return 0;
		else return rec.state;
	}

	@Override
	public void setCell(Path cell, int newState) {
		CellRecordWithCounter rec = data.get( cell );
		if ( rec == null ){
			rec = new CellRecordWithCounter(newState);
			data.put( cell, rec );
		}else{
			rec.state = newState;
		}
	}

	@Override
	public synchronized Iterable<Path> getAliveCells() {
		return Arrays.asList(getAliveCellsArray());
	}
	@Override
	public Field copy() {
		throw new RuntimeException("Not implemented");
	}
}