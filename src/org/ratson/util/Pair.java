package org.ratson.util;
/**Just generic pair. StackOverflow says that everybody must write it. So here is mine*/
public class Pair< A, B> {
	public A left;
	public B right;
	public Pair( A l, B r ){
		left = l;
		right = r;
	}
	public Pair(){
		left = null;
		right = null;
	}
	@Override
	public String toString() {
		return "("+left+", "+right+")";
	}
}
