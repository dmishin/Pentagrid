package org.ratson.pentagrid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


/**Base abstract field.
 * Different concrete fields implement different ways of storing and updating information
 * @author dim
 *
 */
public abstract class Field implements Serializable{
	/**Replace field cells with given data. New state is 1*/
	public abstract void setCells( Iterable<Path> cells  );
	/**Replace field cells with given data. New state is 1*/
	public void setCells( Path[] cells ){
		setCells( Arrays.asList(cells) );
	}
	/**Returns array of all cells with nonzero state*/
	public abstract Path[] getAliveCellsArray();
	/**Returns array of all cells with nonzero state*/
	public abstract Iterable<Path> getAliveCells();
	/**Evaluate world, using this rule*/
	public abstract void evaluate( TotalisticRule r );	
	/**how many cells there are. For some fields, can be very slow.*/
	public abstract int population();
	/**returns cell state*/
	public abstract int getCell(Path cell);
	/***Sets cell state*/
	public abstract void setCell(Path cell, int newState);
	public abstract Field copy();
}

