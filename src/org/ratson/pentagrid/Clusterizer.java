package org.ratson.pentagrid;

import java.util.ArrayList;
import java.util.LinkedList;

import org.ratson.util.Function1;

/**searches fr the clusters in the field*/
public class Clusterizer {
	/**Represents a cluster of cells*/
	public static final class Cluster{
		public ArrayList<Path> cells = new ArrayList<Path>();
		public void add( Path cell ){
			cells.add( cell );
		}
		public boolean has( Path cell ){
			return cells.contains(cell);
		}
		public boolean emty() {
			return cells.isEmpty();
		}
	}
	/**Adding this mask to the cell state marks cell as belonging to cluster*/
	public static final 
	int CLUSTER_MASK = 1<<31;
	
	public LinkedList<Cluster> clusters = new LinkedList<Cluster>();
	
	/**Main method for clusterization*/
	public void clusterize( Field f ){
		Cluster currentCluster = new Cluster();
		for( Path p: f.getAliveCells() ){
			findCLusterAt(f, new OrientedPath(p, 0), currentCluster);
			if (! currentCluster.emty() ){
				clusters.add( currentCluster );
				currentCluster = new Cluster();
			}
		}
		cleanClusterFlag( f );
	}
	
	
	/**Resets the bit flag, used to mark cells in the cluster*/
	private void cleanClusterFlag(Field f) {
		for( Path p: f.getAliveCells() ){
			f.setCell(p, f.getCell(p) & ~CLUSTER_MASK);
		}
	}


	/**All neighbores, used for cluster search*/
	private static ArrayList<Path> level2neigh = createLevel2Neighbores();
	
	/**Recursively filling the cluster*/
	private void findCLusterAt( Field field, OrientedPath cell, Cluster cluster ){
		int state =  field.getCell(cell.path);
		if (state == 0) return;//nothing to do here
		if ( (state & CLUSTER_MASK) != 0) return;//already in the cluster
		cluster.add( cell.path );
		field.setCell(cell.path, state | CLUSTER_MASK );
		for( Path o: level2neigh){
			findCLusterAt(field, cell.attach(o), cluster);
		}
	}
	
	/**Create list of all level 1 and 2 neighbore cells except the root*/ 
	private static ArrayList<Path> createLevel2Neighbores() {
		final 
		ArrayList<Path> rval = new ArrayList<Path>();
		Util.forField(2, new Function1<Path, Boolean>() {
			public Boolean call(Path arg) { 
				if( !arg.isRoot() ) rval.add( arg ); 
				return true; 
			}
		});
		return rval;
	}
	
	public Clusterizer( Field f ){
		clusterize( f );
	}
}
