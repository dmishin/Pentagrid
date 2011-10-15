package org.ratson.pentagrid.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JComponent;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Transform;
import org.ratson.util.Util;

/**Draws cells in the poincare projection*/
public class PoincarePanel extends JComponent {
	static class PointDbl implements Comparable<PointDbl>{
		/**Coordinates and pseudo-angle*/
		double x, y, pAngle;
		public PointDbl( double x, double y){
			double id = 1.0/Math.sqrt( x*x+y*y );
			this.x=x*id;
			this.y=y*id;
			pAngle = Util.quickAtan2(x, y);
		}
		
		@Override
		public int compareTo(PointDbl arg0) {
			if (arg0.pAngle < pAngle) return -1;
			if (arg0.pAngle == pAngle) return 0;
			return 1;
		}
		
		@Override
		public String toString() { return "("+x+";"+y+")"; }
		
		/**Distanec to anouther point*/
		public double distTo( PointDbl p ){ return Math.abs( p.pAngle - pAngle ); }
	};
	private Transform viewTfm = new Transform();
	private int viewTfmModifCounter = 0; //how many time view transform was modified.
	private int fixTransformEvery = 10;//fix view transformation matrix every n steps
	private ArrayList< PointDbl > farAwayPoints = new ArrayList<PoincarePanel.PointDbl>(); 
	
	private Field field = null;
	Shape cellsShape = null;
	public boolean antiAlias = false;
	public boolean showPopulation=false;
	public boolean showGrid = true;
	private Shape gridShape = null;
	private GridPainter gridDrawer = new GridPainter(4);
	
	public Color clrCell = Color.BLUE;
	public Color clrBorder= Color.BLACK;
	public Color clrGrid= Color.LIGHT_GRAY;
	private double maxAllowedAmplitude = 5e5;
	private int margin = 30;
	
	
	PoincarePanel( Field f){
		field = f;
		viewTfm.setEye();
	}
	
	public void setField( Field f ){
		field = f;
		rebuildCells();
		if ( isDisplayable() ) repaint();
	}
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (showPopulation && field != null){
			g.drawString("Cells:"+field.population(), 0, 20);
		}
		
		Dimension sz = getSize();
		Graphics2D g2 = (Graphics2D) g;
		if( antiAlias )
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.translate( sz.width/2, sz.height/2);
	    
		paintContents( g2, sz );
		paintFarAwayPoints( g2, sz );
	}
	
	private void paintFarAwayPoints(Graphics2D g2, Dimension sz ) {
		double scale = getScale( sz );		
		double angularStep = 2.0 / scale ; 
		g2.setColor(clrCell);
		g2.setStroke( new BasicStroke(1) );

		PointDbl oldPoint = null;
		for( PointDbl p: farAwayPoints ){
			if ( oldPoint == null || p.distTo( oldPoint ) > angularStep ) {
				oldPoint = p;
			}else
				continue;
			g2.drawLine( (int)(p.x*scale), 
					(int)(-p.y*scale), 
					(int)(p.x*(scale+margin)),
					(int)(-p.y*(scale+margin)));
		}
	}

	/**Draw grid cells*/
	private void doShowGrid( Graphics2D g2 ) {
		if ( gridShape == null ){
			//Get the transform for the central cell
			Path centralCell = PathNavigation.point2path( viewTfm.hypInverse().tfmVector( new double[]{0,0,1}));
			Transform centralTfm = PathNavigation.getTransformation(centralCell);
			Transform relativeTfm = viewTfm.mul( centralTfm );
			gridShape = gridDrawer.createShape(relativeTfm);
		}
		
		g2.setColor( clrGrid );
		g2.setStroke( new BasicStroke(0) );
		g2.draw( gridShape );
	}

	/**Move view to the origin*/
	public void centerView(){
		viewTfmModifCounter = 0;
		setView( viewTfm.setEye() );
	}
	/**Set view matrix and redraw view*/
	private void setView( Transform tfm ){
		if ( tfm.amplitude() > maxAllowedAmplitude ) return;
		viewTfm = tfm;
		viewTfmModifCounter ++;
		if (viewTfmModifCounter > fixTransformEvery ){
			viewTfmModifCounter = 0;
			viewTfm = viewTfm.fix();
		}
		cellsShape = null;
		gridShape = null;
		repaint();
	}
	
	/**Shift view*/
	public void offsetView(double x, double y){
		Transform offsetTfm = new Transform();
		offsetTfm.setShift( x, y );
		setView( offsetTfm.mul( viewTfm ) );
	}
	
	/**Rotate vies by some angle*/
	public void rotateView( double angle ){
		Transform rot = new Transform();
		rot.setRot(angle);
		setView( rot.mul( viewTfm ) );
	}
	
	/**Creates a shape, that is a projection of the field*/
	private Shape createFieldShape( Iterable<Path> list ){
		//long timeStart = System.currentTimeMillis();
		farAwayPoints.clear();
		GeneralPath path = new GeneralPath();
		for ( Path p : list) createCellShape( path, p);
		//long dt = System.currentTimeMillis() - timeStart;
		//System.out.println("Time elapsed:"+dt+"ms");
		Collections.sort(farAwayPoints);
		return path;
	}
	/**Precalculated coordinates of a pentagon*/
	private static double[] pentagonPoints = new double[10];
	static{
		double r = Math.sqrt (2 / Math.sqrt (5)) *0.9;
		for (int i = 0; i < 10; i+=2) {
			double angle = Math.PI / 5 * i;
			pentagonPoints[i] = Math.cos(angle)*r;
			pentagonPoints[i+1] = Math.sin(angle)*r;
		}			
	}

	private void createCellShape( GeneralPath path, Path cell ){
		Transform pathTfm = viewTfm.mul( PathNavigation.getTransformation(cell) );
		//double[] xyt = pathTfm.tfmVector(new double[]{0,0,1} );
		double t = pathTfm.getAt(2, 2);
		if ( t < 100 )
			PoincareGraphics.renderPoincarePolygon( path, pathTfm, pentagonPoints, true);
		else{
			//farAwayPoints.add( new PointDbl(xyt[0],xyt[1]));
			farAwayPoints.add( new PointDbl(pathTfm.getAt(0, 2), pathTfm.getAt(1, 2))); 
		}
	}
	
	static double len2( double x, double y ){
		return x*x+y*y;
	}
	private double getScale( Dimension size ){
		return Math.max( 1, 0.5 * ( Math.min( size.width, size.height ) - margin ) );
	}
	/**Given the point in th view coordinates, return path to the cell, containing it*/
	public Path mouse2cellPath( int x, int y ){
		Dimension sz = getSize();		
		double scale = getScale( sz );
		//poincare projection coordinates
		double dx = (x - sz.width/2) / scale;
		double dy = -(y - sz.height/2) / scale;
		
		//System.out.println("Dx="+dx+" Dy="+dy);
		//restore hyperbolic coordinates
		// x / (t+1);
		double d2 = len2( dx, dy );
		if ( d2 >= 1 ) return null;
		
		double s = 2/(1-d2);
		
		double [] point = new double[]{ dx*s, dy*s, 0.5*(d2+1)*s };
		
		return PathNavigation.point2path(viewTfm.hypInverse().tfmVector(point));
	}

	public void rebuildCells() {
		cellsShape = null;
		if( isDisplayable() ) repaint();
	}
	
	public BufferedImage exportImage( Dimension size, boolean antiAlias ){
		BufferedImage img = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, size.width, size.height);
		if( antiAlias )
			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		paintContents( g, size );
		return img;
	}

	private void paintContents(Graphics2D g2, Dimension size) {
		if ( cellsShape == null ){
			synchronized( field ){
				cellsShape = createFieldShape( field.getAliveCells() );				
			}
		}
		AffineTransform oldTfm = g2.getTransform();
		double scale = getScale( size );
		g2.scale(scale, scale);
		
		g2.setColor(clrCell);
		g2.fill(cellsShape);

		if ( showGrid ) doShowGrid( g2 );
		
		g2.setColor(clrBorder);
		g2.setStroke( new BasicStroke(0) );
		g2.drawOval(-1, -1, 2, 2);
		
		g2.setTransform(oldTfm);
	}
	
}
