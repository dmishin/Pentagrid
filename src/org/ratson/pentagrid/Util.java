package org.ratson.pentagrid;

import java.util.LinkedList;

import org.ratson.util.Function1;

/**Some utility functions, I don't know where to put to.*/
public class Util {
	/**Create random pentagonal field*/
	public static Path[] randomField( int radius, final double p) {
		final LinkedList<Path> cells = new LinkedList<Path>();

		forField( radius, new Function1<Path, Boolean>() {
			public Boolean call(Path path) {
				if (Math.random() <= p ) cells.add( path );
				return true;
			}
		});
		
		//convert result to array
		Path[] rval = new Path[ cells.size() ];
		cells.toArray( rval );
		return rval;
	}
	/**Iterate over the field of radius R, in funcitonal style*/
	public static void forField( int radius, Function1<Path, Boolean>callback){
		Path root = Path.getRoot();
		for( int r = 0 ; r < radius; ++r ){
			Path cur = root;
			do{
				if ( ! callback.call( cur ) )
					return;
				cur = cur.left();
			}while( !cur.equals(root) );
			root = root.child(1);
		}
	}
	/**Given a field and cell, returns new field with one cell toggled*/
	public static Path[] toggleCell( Path[] cells, Path cell ){
		LinkedList<Path> new_cells = new LinkedList<Path>();
		boolean found = false;
		for (int i = 0; i < cells.length; i++) {
			if ( ! cells[i].equals(cell) )
				new_cells.add( cells[i] );
			else{
				found  = true;
			}
		}
		if (! found )
			new_cells.add( cell );
		Path[] rval = new Path[ new_cells.size()];
		new_cells.toArray( rval );
		return rval;
	}
}
