package org.ratson.pentagrid;
import static java.lang.Math.sqrt;
import java.util.Arrays;
import java.util.Map;
import org.ratson.util.Pair;

///Collection of static methods, used to navigate by paths
public class PathNavigation {
	/**Calculate 10-neighborhood of a point, specified by path*/
	public static Path[] neigh10( Path p ){
		Path[] neigh = new Path[10];
		
		// neighbores of a root: simple case
		if ( p.isRoot() ){
			for( int i = 1; i <= 10; ++i ) neigh[i-1] = p.child(i);
			return neigh;
		}
		//not root
		if ( p.odd() ){
			neigh[0] = p.child(1); //1:path --n5
			neigh[1] = p.child(3); // , 3:path --n5
			neigh[2] = p.left(); // , left path --5
			neigh[3] = p.right(); // , right path --5
			neigh[4] = p.getTail(); // , rest --5
			neigh[5] = p.child(2);// , 2:path --OK
			neigh[6] = p.left().rightmostChild(); // , rightmostChild $ left path
			neigh[7] = p.right().leftmostChild(); // , leftmostChild $ right path
			neigh[8] = nextRightmostChild( p ); // , nextRightmostChild path --position: 4
			neigh[9] = nextLeftmostChild( p ); // , nextLeftmostChild path --position: 8
		}else{ //if even
			neigh[0] = p.child(1);//   1: path
			neigh[1] = p.child(3);//  , 3: path
			neigh[2] = p.child(5);//  , 5: path
			neigh[3] = p.left();//  , left path
			neigh[4] = p.right();//  , right path
			neigh[5] = p.child(2);//  , 2: path
			neigh[6] = p.child(4);//  , 4: path	
			neigh[7] = p.getTail();//  , rest
			neigh[8] = p.left().rightmostChild();//  , rightmostChild $ left path
			neigh[9] = p.right().leftmostChild();//  , leftmostChild $ right path ]
		}
		for (int i = 0; i < neigh.length; i++) assert( neigh[i] != null );
		return neigh;
	}
	
	private static Path nextLeftmostChild(Path p) {
		assert( (!p.isRoot()) && p.odd() );
		if( p.getTail().isRoot() ) 
			return p.getTail().child( org.ratson.util.Util.cycle10( p.getIndex() + 2) );
		else{ //if not root
			if (p.getIndex() + 2 <= p.getTail().maxChildIndex() ) //cycle is possible
				return p.getTail().child( p.getIndex() +  2 );
			else //no cycle is possible
				return p.getTail().right();
		}
	}
	
	private static Path nextRightmostChild(Path p) {
		assert( (!p.isRoot()) && p.odd() );
		if( p.getTail().isRoot() ) 
			return p.getTail().child( org.ratson.util.Util.cycle10( p.getIndex() - 2) );
		else{//if not root
			if (p.getIndex() > 2 ) //cycle is possible
				return p.getTail().child( p.getIndex() - 2 );
			else //no cycle is possible
				return p.getTail().left();
			
		}
	}
	
	//Miscelanneous rotations
	static final Transform TFM_EYE =     (new Transform()).setEye();
	static final Transform TFM_ROT_PI5 = (new Transform()).setRot(    Math.PI / 5 );
	static final Transform TFM_IROT_PI5 = (new Transform()).setRot( - Math.PI / 5 );
	static final Transform TFM_ROT_PI4 = (new Transform()).setRot( Math.PI / 4 );
	static final Transform TFM_IROT_PI4 =(new Transform()).setRot(  - Math.PI / 4 );
	static final Transform TFM_ROT_PI =  (new Transform()).set(new double[]{ -1,0,0, 0,-1,0, 0,0,1} );
	static final Transform TFM_IROT_2PI5 =(new Transform()).setRot( - Math.PI * 2 / 5 );
	static final Transform TFM_ROT_2PI5 =(new Transform()).setRot(    Math.PI * 2 / 5 );
	
	///----Shift length of a center line (radius of the inner circle)
	static final Transform TFM_SHIFT0 =  (new Transform()).setHShift(sqrt( 1 / sqrt(5)));
	static final Transform TFM_ISHIFT0 = (new Transform()).setHShift( - sqrt( 1 / sqrt(5)));
	
	///----SHift by the radius of a pentagon (radius of the anouther circle)
	static final Transform TFM_SHIFT1 =  (new Transform()).setHShift(sqrt( 2 / sqrt(5)));
	static final Transform TFM_ISHIFT1 = (new Transform()).setHShift( - sqrt( 2 / sqrt(5)));

	static final Transform TFM_ISHIFTY_A0 = (new Transform()).setVShift( - sqrt( (1+sqrt(5))/2 ) );
	
	static final Transform[] TFM_I = new Transform[10];
	static { 
		//initialize transformations to neighbors
		Transform tEven = TFM_IROT_PI5.mul( TFM_SHIFT0 ).mul( TFM_SHIFT0 );
		Transform tOdd = TFM_IROT_2PI5.mul( TFM_SHIFT1 ).mul( TFM_SHIFT1 ).mul( TFM_ROT_PI );
		int i = 0;
		while (true){
			TFM_I[ i ] = tEven;
			TFM_I[i+1] = tOdd;
			i += 2;
			if ( i >= 10 ) break;
			tEven = TFM_IROT_2PI5.mul( tEven );
			tOdd = TFM_IROT_2PI5.mul( tOdd );
		}
	}
	
	/**Non-caching getter for the transformation*/
	public static Transform getTransformation( Path p ){
		if( p.isRoot() ) return TFM_EYE;
		if( p.getTail().isRoot() ) return TFM_I[ p.getIndex() - 1 ];
		
		Transform nextTfm=null;
		if (p.getTail().odd()){
			assert (p.getIndex() >= 1 && p.getIndex() <= 3);
			// 1,2,3 => (9,10,1) -1
			nextTfm = TFM_I[ (p.getIndex() + 7) % 10 ];
		}else{
			assert (p.getIndex() >= 1 && p.getIndex() <= 5);
			// 1,2,3,4,5 => (3,4,5,6,7) -1
			nextTfm = TFM_I[ p.getIndex() + 1  ];
		}
		return getTransformation(p.getTail()).mul( nextTfm );				
	}
	
	/**Same as above, but with total memoization*/
	public static Transform getTransformationMem( Path p, Map<Path, Transform> mem){
		if( p.isRoot() ) return TFM_EYE;
		if( p.getTail().isRoot() ) return TFM_I[ p.getIndex() - 1 ];

		
		Transform nextTfm= mem.get(p);
		if (nextTfm != null ) return nextTfm;
		
		if (p.getTail().odd()){
			assert (p.getIndex() >= 1 && p.getIndex() <= 3);
			// 1,2,3 => (9,10,1) -1
			nextTfm = TFM_I[ (p.getIndex() + 7) % 10 ];
		}else{
			assert (p.getIndex() >= 1 && p.getIndex() <= 5);
			// 1,2,3,4,5 => (3,4,5,6,7) -1
			nextTfm = TFM_I[ p.getIndex() + 1  ];
		}
		Transform rval = getTransformationMem(p.getTail(), mem ).mul( nextTfm);				
		mem.put(p, rval);
		return rval;
	}
	
	/**Get coordinate of the cell (i.e. Path), containing the given point.*/
	public static Path point2path( double[] xyt ){
		return point2path(xyt, Path.getRoot());
	}
	
	private static Path point2path( double[] xyt, Path root ){
		if ( isInCentralPentagon( xyt ) ){
			return root;
		}
		//Not in the central pentagon: move toward the origin.
		Pair< double[], Integer> xyt1_idx = stepTowardCenter( xyt );
		int childIndex = -1;
		if (root.isRoot() ){
			childIndex = xyt1_idx.right;
		}else if( root.odd() ){
			childIndex = org.ratson.util.Util.cycle10( xyt1_idx.right + 2 ); //9,10,1 -> 1, 2, 3
			assert( childIndex >= 1 && childIndex <= 3);
		}else{  //if even
			childIndex = org.ratson.util.Util.cycle10( xyt1_idx.right - 2);//3,4,5,6,7 -> 1,2,3,4,5
			assert( childIndex >= 1 && childIndex <= 5);
		}
		return point2path( xyt1_idx.left, root.child(childIndex) );
	}

	/**Moves the point one step toward the central pentagon, and returns index of the sub-area, the point previously resided
	 * Prerequisite: point is *not* in the central pentagon;
	 *               point has normalized coordinates
	 * Returns: new point and sub-area index 
	 * */
	private static Pair<double[], Integer >stepTowardCenter( double[] xyt) {
		assert( ! isInCentralPentagon(xyt) );
		int index = getSubAreaIndex(xyt);
		double[] xyt1 = TFM_I[index-1].hypInverse().tfmVector(xyt);
		return new Pair<double[], Integer>( xyt1, index );
	}
	private static double pointInv( double[] p ){
		return org.ratson.util.Util.sqr(p[2]) - org.ratson.util.Util.sqr(p[0]) - org.ratson.util.Util.sqr(p[1]);
	}
	/**Returns index if the sub-area (i.e. next value in the item path), where the point is located. 
	 * Prerequisite: point is *not* in the central pentagon;
	 *               point has normalized coordinates 
	 * */
	private static int getSubAreaIndex( double[] xyt ){
		
		int rval;
	    rval = isInFirstSubArea( xyt );
	    if ( rval != 0) return rval;

	    xyt = TFM_ROT_2PI5.tfmVector( xyt );
	    rval = isInFirstSubArea( xyt );
	    if ( rval != 0 ) return rval+2;

	    xyt = TFM_ROT_2PI5.tfmVector( xyt );
	    rval = isInFirstSubArea( xyt );
	    if ( rval != 0 ) return rval+4;

	    xyt = TFM_ROT_2PI5.tfmVector( xyt );
	    rval = isInFirstSubArea( xyt );
	    if ( rval != 0 ) return rval+6;

	    xyt = TFM_ROT_2PI5.tfmVector( xyt );
	    rval = isInFirstSubArea( xyt );
	    if ( rval != 0 ) return rval+8;
	    
	    throw new RuntimeException( "Ups. Not in the sub-area" );
	}

	/**Returns 0, if not in the first sub-area, 
	   1, if in the sub-area of the `1` cell
	   2, if in the sub-area of the `2` cell
	*/
	private static int isInFirstSubArea( double[] xyt )
	{
		//First shift so that the vertex shift to the (0,0,1)
	    xyt = TFM_ISHIFT1.tfmVector(xyt);
	    if ( xyt[1] >= xyt[0] && xyt[1] >= -xyt[0] ){ //if in the V-shaped area
			//sub-area 1 or 2
			//rotate by 45 and move by A (length of the side) along Y axis.
			xyt = TFM_ISHIFTY_A0.tfmVector( TFM_ROT_PI4.tfmVector( xyt ) );
			if ( xyt[1] > 0 )
				return 2;
			else 
				return 1;
	    } else{
			//no, not matching at all: outside of the V-shaped area.
			return 0;
	    }
	}

	/**Returns true, if point belongs to the central pentagon. Point must be normalized*/
	private static boolean isInCentralPentagon(double[] xyt) {
		//OK
		//quick check: if it is near enough?
		// x^2 + y^2 <= r1 ^2
		// t^2 <= r1^2 + 1 ; where r1 = sqrt( 2 / sqrt(5) )
		if ( org.ratson.util.Util.sqr(xyt[2]) > 2/sqrt(5) + 1 ) return false;
		
		if ( isInFirstSegment(xyt)) return true;
		xyt = TFM_ROT_2PI5.tfmVector(xyt);
		if ( isInFirstSegment(xyt)) return true;
		xyt = TFM_ROT_2PI5.tfmVector(xyt);
		if ( isInFirstSegment(xyt)) return true;
		xyt = TFM_ROT_2PI5.tfmVector(xyt);
		if ( isInFirstSegment(xyt)) return true;
		xyt = TFM_ROT_2PI5.tfmVector(xyt);
		if ( isInFirstSegment(xyt)) return true;
		
		//no 
		return false;
	}
	/**Returns true, if point is in the first segment of the central pentagon.*/
	private static boolean isInFirstSegment( double[] xyt ){
		//OK
		double tan_2pi5 = sqrt( 5 + 2*sqrt(5));
		double x = xyt[0], y = xyt[1], t = xyt[2];
		
		// t0*x - r0*t  -> x1
		// y0           ->y1
		// -r0*x + t0*t ->t1
		// x1+y1 < 0
		// t0*x + y0 - r0*t 
		double kx =  sqrt(1 + 2/sqrt(5));
		double kt = -sqrt(2/sqrt(5));;		
		return (y >= 0) && //bottom side 
	           (x >= 0) && //not required, but can increase speed (or can not).		
			   (x*kx + y + t*kt <= 0) && //right side
			   (y - x *tan_2pi5 <= 0); //top side
	}
	
	public static double[] normalizedPoint( double x, double y ){
		return new double[]{ x, y, Math.sqrt(x*x+y*y+1) };
	}
	
	public static void main(String[] args) {

		
		System.out.println( Arrays.toString(TFM_I[1-1].tfmVector( normalizedPoint(-1.5627, 0.668))));
		double r1 = sqrt( 1 / sqrt(5));
		System.out.println( Arrays.toString(TFM_I[1-1].tfmVector( normalizedPoint(-r1, 0))));
		
	}
}
