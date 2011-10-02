package org.ratson.pentagrid;
import java.io.Serializable;

@SuppressWarnings("serial")
public final class Path implements Comparable<Path>, Serializable{
	private static final int ROOT_INDEX = -1;
	private static final Path root = new Path( null, ROOT_INDEX );
	private final int index;
	private final int precalculatedHash;
	private final Path tail;
	
	/**User should not call this constructor, use getChild and createRoot*/
	private Path( Path tail, int idx ){
		this.tail = tail;
		index = idx;
		precalculatedHash = calcHashCode();
	}
	
	public boolean odd(){ return (index & 1) == 1; }
	public boolean even(){ return ! odd(); }
	
	/**Check path correctness*/
	public boolean isCorrect(){
		if ( isRoot() ){
			return index == ROOT_INDEX;
		}else{
			return (index >=1 ) && (index <= tail.maxChildIndex() ) && tail.isCorrect();
		}
	}
	
	/**True for [] path, referencing the central cell*/
	public boolean isRoot(){ return tail == null;}
	
	public static Path getRoot(){ return root; }
	
	/**Recursively calculates hash code for the path*/
	private int calcHashCode(){
		if ( isRoot() ) return 1;
		else return index + 31 * tail.hashCode();
	}
	
	/**Child paths can have indices from 1 to this value*/
	public int maxChildIndex(){
		if (isRoot()){
			return 10; //root has 10 children
		}else{
			if (odd()){
				return 3;
			}else{
				return 5;
			}
		}
	}
	/**One of this child paths. Index is not checked, but must be between 1 and maxChildIndex()*/
	public Path child( int index ){ return new Path( this, index ); }
	
	@Override
	public boolean equals(Object arg) {
		if ( arg == this ) return true;
		if ( ! (arg instanceof Path) ) return false;
		Path p = (Path)arg;
		
		if (isRoot())
			return p.isRoot();
		else{
			if ( p.isRoot() ) return false;
			if ( p.hashCode() != hashCode() ) return false;
			return (this.index == p.index) && tail.equals(p.tail);
		}
	}
	
	@Override
	public int hashCode() { return precalculatedHash; }
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer("[");
		if (! isCorrect() )
			buf.append( "ERR" );
		Path cur = this;
		while( !cur.isRoot()){
			if ( cur != this ) buf.append(",");
			buf.append(cur.index);
			cur = cur.tail;
		}
		buf.append("]");
		return buf.toString();
	}
	
	///////////// Path transformations /////////////////
	Path leftmostChild(){ return child( 1 ); }
	Path rightmostChild(){ return child( maxChildIndex() ); }
	
	public Path left(){
		// []
		if (isRoot()) return this;
		// [a]
		else if (tail.isRoot()){
			return tail.child( org.ratson.util.Util.cycle10( index - 1 ) );
		}
		// (h:rest)
		else{
			if ( index >= 2 ) return tail.child( index - 1 );
			else return tail.left().rightmostChild();
		}
	}
	
	public Path right(){
		if ( isRoot()) return this;
		else if ( tail.isRoot() ){
			return tail.child( org.ratson.util.Util.cycle10(index+1));
		}else{
			if (index + 1 <= tail.maxChildIndex())
				return tail.child( index+1 );
			else
				return tail.right().leftmostChild();
		}
	}
	
	public static void main(String[] args) {
		Path r = getRoot();
		System.out.println("Root:" + r );
		
		Path c = r.child(1).child(3).child(1).child(1);
		System.out.println( "Child:" + c);
		System.out.println( "Left:" + c.left());
		System.out.println( "right:" + c.right());
		//////////////////////////////////////////////////////
		System.out.println( " Measuring cycle lenght... ");
		int idx = 1;
		Path c1 = c.left();
		while ( ! c1.equals(c) ){
			c1 = c1.left();
			idx += 1;
		}
		System.out.println( "   Length is " + idx);
		
		System.out.println(" ===== Paths ====== ");
		System.out.println( "Root transform is:");
		System.out.println( PathNavigation.getTransformation( r ));
		System.out.println( "Child transform is:");
		System.out.println( PathNavigation.getTransformation( c ));
	}
	public Path getTail() {
		return tail;
	}
	public int getIndex() {
		return index;
	}
	/**Returns path length. Approximately relates to the distance to center.*/
	public int length() {
		if (isRoot()) return 0;
		return 1 + getTail().length(); 
	}

	/**Slow recursive comparison. */
	@Override
	public int compareTo(Path o) {
		if ( o == this ) return 0;
		if ( isRoot() ){
			if (o.isRoot()) return 0;
			return -1;
		}else{
			//this is not root.
			if (o.isRoot() ) return 1;
			//both are not roots
			if (index < o.index ) return -1;
			else if (index > o.index ) return 1;
			else return tail.compareTo(o.tail);
		}
	}
}
