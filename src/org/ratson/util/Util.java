package org.ratson.util;

public class Util {

	public static int cycle10(int i) {
		return Util.mod( i-1, 10 )+1;
	}

	/**Mathematical module*/
	public static int mod( int i, int d ){
		int rval = i % d;
		if ( rval >= 0 ) return rval;
		return rval + d;
	}

	/**Square*/
	public static double sqr(double d) {
		return d*d;
	}

	public static double quickAtan2( double x, double y ){
		boolean pp = x >= -y;
		boolean pq = x >=  y;
		if ( pp && pq ){ //OX axis, positive direction
			return y/x; // from -1 to 1
		}
		if ( pp && !pq  ){ //OY axis, positive direction
			return -x/y + 2; //from 1 to 3
		}
		if ( !pp && !pq){ //OX axis, negative directoin
			return y / x + 4; //from 3 to 5
		}
		//rest: OY axis, negative
		return -x/y + 6;//from 5 to 7
	}

}
