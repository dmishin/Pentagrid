package org.ratson.pentagrid.fields;

import java.io.Serializable;

/**Record, storing cell state and additional information*/
public class CellRecord implements Serializable{
	private static final long serialVersionUID = 1L;
	public int state;
	public int sum;
	public CellRecord( int state ){
		this.state = state;
		sum = 0;
	}
}
