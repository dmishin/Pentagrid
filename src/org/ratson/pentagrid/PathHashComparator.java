package org.ratson.pentagrid;

import java.util.Comparator;

/**Comparator for fast sorting objects in the fixed, but undefined order*/
public class PathHashComparator implements Comparator<Path> {
	@Override
	public int compare(Path o1, Path o2) {
		int h1 = o1.hashCode();
		int h2 = o2.hashCode();
		if ( h1 < h2 ) return -1;
		if ( h1 > h2 ) return 1;
		return o1.compareTo(o2);
	}
	private static PathHashComparator instance = new PathHashComparator();
	
	public static PathHashComparator getInst(){ return instance; };
}
