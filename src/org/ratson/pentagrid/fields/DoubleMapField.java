package org.ratson.pentagrid.fields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Rule;

/**Fields that uses 2 maps for storing cells. This solves problem of ading new cells while evaluating.*/
public final class DoubleMapField extends Field{
	//maps cell to the cell state
	private Map<Path, CellRecord> data1, data2; 

	private Map<Path, CellRecord> getMap( int idx ){
		switch(idx){
		case 0 : return data1;
		case 1 : return data2;
		default: throw new RuntimeException( "Wrong data index" ); 
		}
	}
	
	/**Create completely new field*/
	public void setCells( Iterable<Path> cells ){
		data1 = new HashMap<Path, CellRecord>();
		data2 = new HashMap<Path, CellRecord>();
		
		//Put items equally to both maps
		int idx = 0;
		for (Path path : cells) {
			getMap( idx ).put( path, new CellRecord(1));
			idx = 1-idx;
		}
	}
	
	/**Update sum counters from the MainFld, putting new cells to the spareFLd*/ 
	private void calcSumsPartial( Map<Path, CellRecord> mainFld, Map<Path, CellRecord> spareFld ){
		//for each non-zero cell, increase counters for all neighbores.
		for( Entry<Path, CellRecord> path_rec : mainFld.entrySet() ){
			if ( path_rec.getValue().state == 0 ) continue; //it is important here, because on the second step spare field can have cells with 0 state
			
			Path[] neighbores = PathNavigation.neigh10( path_rec.getKey() );
			for (int i = 0; i < neighbores.length; i++) {
				CellRecord neighRec = spareFld.get( neighbores[i] );
				if (neighRec == null) 
					neighRec = mainFld.get( neighbores[i] );
				if (neighRec == null){ //The neighbore is not registered yet, neither in the old, nor in the new cells.
					neighRec = new CellRecord(0);
					spareFld.put( neighbores[i], neighRec );
				}
				neighRec.sum += path_rec.getValue().state;
			}
		}
		
	}
	public void evaluate( Rule r ){
		calcSumsPartial(data1, data2);
		calcSumsPartial(data2, data1);
		updateState( data1, r );
		updateState( data2, r );
		Map<Path, CellRecord> buf = data1;
		data1 = data2;
		data2 = buf;
	}
	
	/**Calculate next state for the cells i nthe map*/
	private void updateState(Map<Path, CellRecord> data, Rule r) {
		for( Iterator<CellRecord> iRec = data.values().iterator(); iRec.hasNext();){
			CellRecord rec = iRec.next();
			int newState = r.nextState( rec.state, rec.sum);
			if (newState != 0){
				rec.state = newState;
				rec.sum = 0;
			}else{ //remove dead cell
				iRec.remove();
			}
		}
	}
	
	public Path[] getAliveCellsArray(){
		Path[] rval = new Path[ data1.size() + data2.size() ];
		int idx = 0;
		for(Path p : data1.keySet()){
			rval[idx] = p;
			idx++;
		}
		for(Path p : data2.keySet()){
			rval[idx] = p;
			idx++;
		}
		return rval;
	}

	@Override
	public int population() {
		return data1.size() + data2.size();
	}

	@Override
	public int getCell(Path cell) {
		CellRecord rec = data1.get( cell );
		if (rec == null) rec = data2.get( cell );
		if (rec == null)return 0;
		return rec.state;
	}

	@Override
	public void setCell(Path cell, int newState) {
		if (newState != 0){
			CellRecord rec = data1.get( cell );
			if (rec == null ) rec = data2.get(cell);
			if ( rec == null ){
				rec = new CellRecord(newState);
				data1.put( cell, rec );
			}else{
				rec.state = newState;
			}
		}else{
			data1.remove( cell );
			data2.remove( cell );
		}
	}

	@Override
	public Iterable<Path> getAliveCells() {
		return Arrays.asList( getAliveCellsArray() );
	}
	@Override
	public Field copy(){
		throw new RuntimeException("Not implemented");
	}
}