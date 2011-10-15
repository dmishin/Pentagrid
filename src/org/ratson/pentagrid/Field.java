package org.ratson.pentagrid;

import java.io.Serializable;
import java.util.Arrays;

/**Base abstract field.
 * Different concrete fields implement different ways of storing and updating information
 * @author dim
 *
 */
public abstract class Field implements Serializable{
	protected int fieldState=0;
	/**Global field state. Used by the day.night rules*/
	public final int getFieldState(){ return fieldState; };
	/**Global field state. Used by the day.night rules*/
	public final void setFieldState( int newState ){ fieldState = newState; };
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
	/**Create copy of the field*/
	public abstract Field copy();
}

