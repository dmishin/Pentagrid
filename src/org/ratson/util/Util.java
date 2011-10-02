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

}
