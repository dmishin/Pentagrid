package org.ratson.pentagrid.gui;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import org.ratson.pentagrid.Transform;

/**Collection of the methods for drawing primitives in Poincare projection*/
public class PoincareGraphics {
	private PoincareGraphics(){};
	
	/**Create Bezier-approximated circular arc, that is a projection of the straight segment.
	 * Center of the poincare circle is at 0,0; radius is 1.
	 * Current position of the shape must be at x0,y0. 
	 */
	static void drawPoincareCircleTo(GeneralPath pth, double x0, double y0,
			double x1, double y1) {
		//Calculate radius of the circular arc.
		double sq_l0 = PoincarePanel.len2( x0, y0 );
		double sq_l1 = PoincarePanel.len2( x1, y1 );
		
		double k0 = (1+1/sq_l0) * 0.5;
		double k1 = (1+1/sq_l1) * 0.5;
		
		double delta2 = PoincarePanel.len2( x0*k0 - x1*k1, y0*k0 - y1*k1 );
	
		if (delta2 < 1e-4 ){ // 0.01^2 lenght of a path too small, create straight line instead to make it faster.
			pth.lineTo( x1, y1 ); 
			return;
		}
		
		double cross = (x0*y1 - x1*y0);
		double r2 = ( sq_l0 * sq_l1 * delta2 ) / (cross*cross) - 1 ; 
		
		double r = - Math.sqrt( r2 );
		if ( cross < 0 ) r = -r;
		
		if ( Math.abs(r) < 100 ){
			PoincareGraphics.drawBezierApproxArcTo( pth, x0, y0, x1, y1, r, r<0 );
		}else{
			pth.lineTo( x1, y1 );
		}
	}

	/**Bezier curve approximation of the circular arc
	 * x0, y0 - start point
	 * x1, y1 - end point
	 * r - curvature radius (can have any sign, but must be bigger than 2*distance(start, end)
	 * reverse - if true, then draw from end to start.
	 * */
	static void drawBezierApproxArcTo(GeneralPath pth, double x0, double y0,
			double x1, double y1, double r, boolean reverse) {
	    	
		double d2 = PoincarePanel.len2( x0-x1, y0-y1 );
	    double d  = Math.sqrt( d2 );
	    
	    double ct = Math.sqrt( 4*r*r - d2)*0.5;
	    if( reverse ) ct = -ct;
	
	    //Main formulas for calculating bezier points. Math was used to get them.
	    double kx = 4./3. * (r - ct)/d;
	    double ky = 8./3. * r*(ct - r)/d2 + 1./6.;
	
	    //Get the bezier control point positions
	    //vx is a perpendicular vector, vy is parallel
	    double vy_x = x1-x0, vy_y = y1-y0;
	    double vx_x = vy_y, vx_y = -vy_x; // #rotated by Pi/2
	    
	    double p11x = (x0+x1)*0.5 + vx_x * kx + vy_x * ky;
	    double p11y = (y0+y1)*0.5 + vx_y * kx + vy_y * ky;
	    
	    double p12x = (x0+x1)*0.5 + vx_x * kx - vy_x * ky;
	    double p12y = (y0+y1)*0.5 + vx_y * kx - vy_y * ky;
		
	    pth.curveTo(p11x, p11y, p12x, p12y, x1, y1);
	}

	/**Create bezier path for the Poincare projection of the polygon, inplace method*/
	public static void renderPoincarePolygon( GeneralPath pth, Transform pathTfm, double[] polygon_xy, boolean closePath ){
		//First, get projection of all point to the poincare disc.
		double xPrev=0, yPrev=0;
		double x0=0, y0=0;
	
		for (int i = 0; i < polygon_xy.length; i+=2) {
			double[] xyt = pathTfm.tfmVector( polygon_xy[i], polygon_xy[i+1]);
			if (xyt[2] > 1000 ) return; //do not draw cells that are too far
			double x =    PoincareGraphics.poincare( xyt[0], xyt[2] );
			double y =  - PoincareGraphics.poincare( xyt[1], xyt[2] );
			//now draw segment, if not the first point
			if ( i==0 ){ //first point...
				pth.moveTo( x, y );
				x0 = x; y0 = y;
			}else{//not first
				drawPoincareCircleTo( pth, xPrev, yPrev, x, y );
			}
			xPrev = x; yPrev = y;
		}		
		if (closePath){
			drawPoincareCircleTo( pth, xPrev, yPrev, x0, y0 );
			pth.closePath();
		}
	}
	
	/**Create bezier path for the Poincare projection of the polygon*/
	public static Shape renderPoincarePolygon( Transform pathTfm, double[] polygon_xy, boolean closePath ){
		//create the shape
		GeneralPath pth = new GeneralPath();
		renderPoincarePolygon( pth, pathTfm, polygon_xy, closePath );
		return pth;
	}

	/**Poincare projection*/
	static double poincare(double x, double t) {
		return x / (t+1);
	}

}
