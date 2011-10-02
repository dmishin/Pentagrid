package org.ratson.pentagrid;

import java.util.Arrays;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.ratson.util.Util;
/**Basically - 3x3 matrix*/
public final class Transform {
	double [] matrix = null;
	
	public Transform(){
		matrix = new double [9];
	}
	
	public Transform( double[] mtx ){
		set( mtx );
	}
	
	private int idx( int i, int j ){ return i * 3 + j; };
	
	public double getAt( int i, int j ){ return matrix[ idx(i,j) ]; };
	public Transform setAt( int i, int j, double v){ matrix[idx(i,j)] = v; return this; };
	
	///a.mul(b) === a * b
	public Transform mul( Transform t ){
		Transform rval = new Transform();
		for (int i = 0; i < 3; i++) {
			for( int j =0; j < 3; ++j){
				double s = 0;
				for( int k = 0; k < 3; ++k)
					s += this.getAt( i,k ) * t.getAt(k,j);
				rval.setAt( i,j, s );
			}
		}
		return rval;
	}
	
	public Transform setEye(){
		for (int i = 0; i < 3; i++) {
			for( int j =0; j < 3; ++j){
				setAt(i,j, (i==j)? 1 : 0 );
			}
		}
		return this;
	}
	
	public Transform setRot( double alpha ){
		return setRot( Math.cos(alpha), Math.sin(alpha) );
	}

	private Transform setRot(double c, double s) {
		setEye();
		setAt(0,0,c);
		setAt(0,1,s);
		setAt(1,0,-s);
		setAt(1,1,c);
		return this;
	}
	
	/**Horizontal hyperbolic shift*/
	public Transform setHShift( double sh ){
		double ch = Math.sqrt( 1 + sh*sh );
		return setHShift( sh, ch );
	}
	
	public Transform setHShift( double sh, double ch ){
		setEye();
		setAt( 0,0, ch );
		setAt( 0,2, sh );
		setAt( 2,0, sh );
		setAt( 2,2, ch );
		return this;
	}
	/**Vertical hyperbolic shift*/
	public Transform setVShift(double sh) {
		double ch = Math.sqrt( 1 + sh*sh );
		setEye();
		setAt( 1,1, ch );
		setAt( 1,2, sh );
		setAt( 2,1, sh );
		setAt( 2,2, ch );
		return this;
	}

	public double[] tfmVector( double x, double y ){
		double t = Math.sqrt( 1+x*x+y*y );
		return new double[]{
				getAt(0,0)*x+getAt(0,1)*y+getAt(0,2)*t,
				getAt(1,0)*x+getAt(1,1)*y+getAt(1,2)*t,
				getAt(2,0)*x+getAt(2,1)*y+getAt(2,2)*t,
		};
	}
	
	public double[] tfmVector( double[] xyt ){
		assert( xyt.length == 3 );
		return new double[]{
				getAt(0,0)*xyt[0]+getAt(0,1)*xyt[1]+getAt(0,2)*xyt[2],
				getAt(1,0)*xyt[0]+getAt(1,1)*xyt[1]+getAt(1,2)*xyt[2],
				getAt(2,0)*xyt[0]+getAt(2,1)*xyt[1]+getAt(2,2)*xyt[2],
		};
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for( int i = 0; i < 3; ++ i){
			buf.append( "[");
			for( int j = 0; j < 3; ++j){
				if (j!=0) buf.append(",");
				buf.append( getAt(i,j) );
			}
			buf.append("]\n");
		}
		return buf.toString();
	}
	
	/**Set matrix data, row_wise*/
	public Transform set(double[] newMatrix) {
		assert( newMatrix.length == 9);
		matrix = newMatrix;
		return this;
	}

	/**Inverse transform, using the fact, that this transform is hyperbolic (has property T'JT = J)
	 * @return inverse transform.
	 */
	public Transform hypInverse(){
		return new Transform(new double[]{
				 getAt(0,0),  getAt(1,0), -getAt(2,0),
				 getAt(0,1),  getAt(1,1), -getAt(2,1),
				 -getAt(0,2), -getAt(1,2), getAt(2,2),
				 });
	}
	private void inv_swapRows( int i1, int i2 ){
		if (i1==i2) return;
		for( int j =0; j<3;++j){
			double a1,a2;
			a1 = getAt(i1,j); a2 = getAt( i2,j);
			setAt(i1,j, a2);
			setAt(i2,j, a1);
		}
	}
	private int inv_greatestElemInCol( int col ){
		double gr = Math.abs(getAt( 0, col ));
		int iMax = 0;
		for( int i = 1; i < 3; ++ i ){
			double ei = Math.abs( getAt(i, col) );
			if ( ei > gr ){
				gr = ei;
				iMax = i;
			}
		}
		return iMax;
	}
	private void inv_scaleRow( int i, double k ){
		for( int j =0; j<3; ++j){
			setAt(i,j,k*getAt(i,j));
		}
	}
	private void inv_subtractRow(int row, int from, double k) {
		for (int j = 0; j < 3; ++j){
			setAt( from, j, getAt( from,j) - k * getAt(row,j) );
		}
	}

	public Transform inverse(){
		Transform rval = (new Transform()).setEye();
		Transform orig = copy();
		for( int i = 0; i < 3; ++ i){
			int jMax = orig.inv_greatestElemInCol(i);
			orig.inv_swapRows(i, jMax);
			rval.inv_swapRows(i, jMax);
			
			//scale row i
			double a_ii = orig.getAt( i,i );
			orig.inv_scaleRow( i, 1/a_ii );
			rval.inv_scaleRow( i, 1/a_ii );
			
			//subtract from other rows
			for ( int i1 = 0; i1 < 3; ++ i1){
				if (i1 != i){
					double k = orig.getAt( i1,i );
					orig.inv_subtractRow( i, i1, k );
					rval.inv_subtractRow( i, i1, k );
				}
			}
		}
		Transform I = mul(rval);
		double s = 0;
		for( int i =0;i<3;++i){
			for(int j =0; j<3;++j){
				s += Math.abs( I.getAt(i,j) - ((i==j)?1:0) );
			}
		}
		System.out.println("####INV error:"+s);
		return rval;
	}
	public Transform copy() {
		return new Transform(Arrays.copyOf(matrix,9));
	}
	/**Fixes errors, accumulated in the transform, making it valid hyperbolic movement*/
	public Transform fix(){
		double x,y;
		x = getAt( 0, 2); //point 0,0 is moved here
		y = getAt(1,2);
		
		Transform iOffset = new Transform(); iOffset.setShift(-x, -y);
		
		Transform rot = iOffset.mul( this ); //this must become pure rotation.
		
		double cos, sin;
		cos = rot.getAt(0,0)+rot.getAt(1,1);
		sin = -(rot.getAt(1,0) - rot.getAt(0,1));
		double norm = Math.sqrt( cos*cos+sin*sin);
		if ( norm < 1e-6){
			cos = 1; sin = 0;
		}else{
			cos /= norm; sin /= norm;
		}
		rot.setRot(cos, sin);
		return iOffset.hypInverse().mul( rot );
	}

	public static void main(String[] args) {
		Transform t1 = (new Transform()).setHShift(0.5);
		Transform t2 = (new Transform()).setRot(0.5).mul(t1);
		System.out.println("=== Fixing ===");
		System.out.println( t2 );
		System.out.println( t2.fix() );
	}

	/**Set both vertical and horizontal shift*/
	public Transform setShift(double x, double y) {
		double t = Math.sqrt( 1 + x*x + y*y );
		return setShift( x,y,t );
	}
	/**Set both vertical and horizontal shift*/
	public Transform setShift(double x, double y, double t) {
		set( new double[]{
				x*x/(t+1)+1,  x*y/(t+1),   x,
				x*y/(t+1),    y*y/(t+1)+1, y,
				x,            y,           t
		} );
		return this;
	}

	/**Maximal absolute value of the matrix element*/
	public double amplitude() {
		double amp = 0;
		for (int i = 0; i < matrix.length; i++) {
			double a = Math.abs(matrix[i]);
			if ( a > amp ) amp = a;
		}
		return amp;
	}
}
