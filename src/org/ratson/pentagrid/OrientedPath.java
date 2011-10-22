package org.ratson.pentagrid;

import java.util.Arrays;

import org.ratson.util.Function1;
import org.ratson.util.Util;

/**Oriented path stores information about Pentagrid cell and orientation.
 * 
 * @author dim
 *
 */
public final class OrientedPath {
	/**Cell, referenced by the path*/
	public final Path  path;
	/**Orientation, relative to the default orientation of the path. 
	 * If `orientation` is 0, then vector point to the 10'th neighbore.*/
	public final int orientation;
	
	/**Create a path*/
	public OrientedPath( Path p, int o ){
		assert( o >=0 && o < 10 );
		path = p;
		orientation = o;
	}
	@Override
	public String toString() {
		return "Oriented("+orientation+"):"+path;
	};
	
	/**Same path, but endpoint is rotated by some amount*/
	public OrientedPath rotate( int by ){
		return new OrientedPath( path, Util.mod( orientation + by*2, 10) );
	}
	
	/** most important: neighbores
	 *  Returns i'th neighbore, oriented so that it's vector point strictly outside (for odd) or strictly inside (for even)
	 *  Neighbore index is in [1..10] 
	 * */
	/*public final OrientedPath getNeighbore( int index ){
		System.out.println( "Getting "+index+"'th neighbore for "+this);
		OrientedPath rval = getNeighbore_(index);
		System.out.println(""+this+" --("+index+")-> "+rval);
		return rval;
	}
	*/
	private OrientedPath getRootNeigh(){
		if( path.isRoot() ) throw new RuntimeException("root has not root");
		if (path.odd()){
			Path parentPath = path.getTail();
			if (parentPath.isRoot()){
				//5+offset = this.index
				//offset = this.index - 5 (even!)
				return new OrientedPath( parentPath, Util.mod( path.getIndex() - 5, 10));
			}else if (parentPath.odd()){
				//offset = this.index + 3
				return new OrientedPath( parentPath, Util.mod( path.getIndex() + 3,10) );
			}else{//parent oath is even
				return new OrientedPath( parentPath, Util.mod( path.getIndex() - 3, 10) );
			}
		}else{
			Path parentPath = path.getTail();
			if (parentPath.isRoot()){
				//10+offset = this.index
				//offset = this.index - 10 = this.index (even!)
				return new OrientedPath( parentPath, Util.mod( path.getIndex(), 10));
			}else if (parentPath.odd()){
				return new OrientedPath( path.getTail(), Util.mod(path.getIndex()-2,10));
			}else{ // parent path is even
				return new OrientedPath( path.getTail(), Util.mod(path.getIndex()+2,10));
			}			
		}
	}
	public final OrientedPath getNeighbore( int index ){
		int absoluteNeighIndex = Util.cycle10( index + orientation ); //index of the neighbore, in absolute coordinates.
		//System.out.println("#"+this+" -> "+absoluteNeighIndex);
		
		// neighbores of a root: simple case. 
		if ( path.isRoot() ){
			return new OrientedPath( path.child( absoluteNeighIndex ), 0 );
		}
		
		//not root
		if ( path.odd() ){
			switch( absoluteNeighIndex ){
			//non-recursive cases
			case 9 : return new OrientedPath( path.child(1), 0 ); //1:path --n5
			case 10: return new OrientedPath( path.child(2), 0 );// , 2:path --OK
			case 1 : return new OrientedPath( path.child(3), 0 ); // , 3:path --n5
			case 5 : return getRootNeigh();
			//seld index is odd, so rotation is even
						//index must be such, that getting 5'th (relative) neighbore from rval would return self.
			//therefore, for child:
			//      5 + orient - 8 = self.index
			//      orient = self.index + 3
			
			//recursive cases
			case 2 : return getRootNeigh().getNeighbore(6).getNeighbore( 3 ).rotate(-2);
			case 3 : return getRootNeigh().getNeighbore(6).rotate(-2);
			case 4 : return getRootNeigh().getNeighbore(7).rotate(-2);
			case 6 : return getRootNeigh().getNeighbore(3).rotate(2);
			case 7 : return getRootNeigh().getNeighbore(4).rotate(2);
			case 8 : return getRootNeigh().getNeighbore(4).getNeighbore( 7).rotate(2);
			default:
				throw new RuntimeException( "Invalid index" );
			}
		}else{ //if even
			switch( absoluteNeighIndex ){
			//non-recursive cases
			case 3: return new OrientedPath( path.child(1),0); //child.index=neigh_index - 2
			case 4: return new OrientedPath( path.child(2),0);
			case 5: return new OrientedPath( path.child(3),0);
			case 6: return new OrientedPath( path.child(4),0);
			case 7: return new OrientedPath( path.child(5),0);
			case 10:return getRootNeigh();
			//for even children:
			// rotation must be such that 10'th child is self.
			//
			//therefore, for child:
			//      10 + orient - 2 = self.index
			//      orient = self.index - 8
			

			//recursive cases
			case 1: return getRootNeigh().getNeighbore(9).rotate(-1);
			case 2: return getRootNeigh().getNeighbore(9).getNeighbore(1).rotate(2);
			case 9: return getRootNeigh().getNeighbore(1).rotate(1);
			case 8: return getRootNeigh().getNeighbore(1).getNeighbore(9).rotate(-2);
			default:
				throw new RuntimeException( "Invalid index" );
			}
		}
	}
	
	
	private static void test_chain( Path p ){
		OrientedPath o = new OrientedPath(p, 0);
		Path[] n0 = PathNavigation.neigh10(p);
		Path[] n1 = new Path[10];
		for( int i = 0; i < 10; ++ i){
			n1[i] = o.getNeighbore(i+1).path;
		}
		Arrays.sort(n0);
		Arrays.sort(n1);
		if ( !Arrays.equals(n0, n1) ){
			System.out.println("Neighbores calculated incorrectly for "+p);
		}
	}
	public static void main(String[] args) {
		Path p = Path.fromArray( new int[]{1,2} );
		test_chain(p);
		OrientedPath o = new OrientedPath( p, 0);
		System.out.println( o.getNeighbore(1));
		System.out.println("Making big test...");
		org.ratson.pentagrid.Util.forField( 5, new Function1<Path, Boolean>() {
			public Boolean call(Path arg) {
				test_chain( arg );
				return true;
			}
		});
		System.out.println("Test done!");
	}
}
