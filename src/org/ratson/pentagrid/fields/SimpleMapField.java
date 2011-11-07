package org.ratson.pentagrid.fields;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.TotalisticRule;

/**Field, storing only living cells in one single map.*/
public final class SimpleMapField extends Field{
	private static final long serialVersionUID = 1L;
	//maps cell to the cell state
	private Map<Path, CellRecord> data = new HashMap<Path, CellRecord>();
	private Map<Path, CellRecord> newCells = new HashMap<Path, CellRecord>();
	/**Create completely new field*/
	public synchronized void setCells( Iterable<Path> cells ){
		data = new HashMap<Path, CellRecord>();
		for (Path path : cells) {
			data.put( path, new CellRecord(1));
		}
	}
	
	private void calculateSums(){
		//for each non-zero cell, increase counters for all neighbores.
		newCells.clear();
		for( Entry<Path, CellRecord> path_rec : data.entrySet() ){
			if ( path_rec.getValue().state == 0 ) continue;
			Path[] neighbores = PathNavigation.neigh10( path_rec.getKey() );
			for (int i = 0; i < neighbores.length; i++) {
				CellRecord neighRec = data.get( neighbores[i] );
				if (neighRec == null) 
					neighRec = newCells.get( neighbores[i] );
				if (neighRec == null){ //The neighbore is not registered yet, neither in the old, nor in the new cells.
					neighRec = new CellRecord(0);
					newCells.put( neighbores[i], neighRec );
				}
				neighRec.sum += path_rec.getValue().state;
			}
		}
	}
	public synchronized void evaluate( TotalisticRule r ){
		calculateSums();
		//First process all cells in the old array
		for( Iterator<CellRecord> iRec = data.values().iterator(); iRec.hasNext(); ){
			CellRecord state = iRec.next();
			int nextState = r.nextState( getFieldState(), state.state, state.sum); 
			if ( nextState != 0){
				state.state = nextState;
				state.sum = 0;
			}else{
				iRec.remove();
			}
		}
		//now all added cells
		for( Iterator<Entry<Path, CellRecord> > iRec = newCells.entrySet().iterator(); iRec.hasNext(); ){
			Entry<Path, CellRecord> path_state = iRec.next();  
			CellRecord state = path_state.getValue();
			int nextState = r.nextState( getFieldState(), state.state, state.sum); 
			if ( nextState != 0){
				state.state = nextState;
				state.sum = 0;
				data.put(path_state.getKey(), state);
			}
		}
		newCells.clear();
		setFieldState( r.nextFieldState( getFieldState() )); //update the world global state
	}
	
	public synchronized Path[] getAliveCellsArray(){
		Path[] rval = new Path[ data.size() ];
		data.keySet().toArray( rval );
		return rval;
	}

	@Override
	public int population() {
		return data.size();
	}

	@Override
	public synchronized int getCell(Path cell) {
		CellRecord rec = data.get( cell );
		if (rec == null)return 0;
		else return rec.state;
	}

	@Override
	public synchronized void setCell(Path cell, int newState) {
		if (newState != 0){
			CellRecord rec = data.get( cell );
			if ( rec == null ){
				rec = new CellRecord(newState);
				data.put( cell, rec );
			}else{
				rec.state = newState;
			}
		}else{
			data.remove( cell );
		}
	}

	@Override
	public synchronized Iterable<Path> getAliveCells() {
		return data.keySet();
	}
	@Override
	public synchronized Field copy(){
		SimpleMapField rval = new SimpleMapField();
		for( Entry<Path, CellRecord> e : data.entrySet() ){
			rval.data.put( e.getKey(), new CellRecord(e.getValue().state));
		}
		return rval;
	}
}
