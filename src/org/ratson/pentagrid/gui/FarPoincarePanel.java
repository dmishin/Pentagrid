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
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JComponent;

import org.ratson.pentagrid.OrientedPath;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Transform;
import org.ratson.pentagrid.Util;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.pentagrid.gui.PoincarePanel.PointDbl;
import org.ratson.util.Function1;

public class FarPoincarePanel extends JComponent {
	OrientedPath viewCenter = new OrientedPath(Path.getRoot(), 0);
	VisibleCell[] visibleCells = null;
	int visibleRadius = 4;
	private Transform viewTransform = (new Transform()).setEye();
	private SimpleMapField field = null;

	private Shape cellsShape = null;
	public boolean antiAlias = false;
	public boolean showGrid = true;
	private Shape gridShape = null;
	private GridPainter gridDrawer = new GridPainter( visibleRadius );

	private int viewTfmModifCounter = 0; //how many time view transform was modified.
	private int fixTransformEvery = 100;//fix view transformation matrix every n steps
	
	public Color clrCell = Color.BLUE;
	public Color clrBorder= Color.BLACK;
	public Color clrGrid= Color.LIGHT_GRAY;	
	private int margin = 30;

	
	static final class VisibleCell{
		Path relativePath;
		Path absolutePath;
		Transform relativeTfm;
		int state=0;
		public VisibleCell( Path relative, OrientedPath origin ){
			relativePath = relative;
			absolutePath = origin.attach(relative).path;
			relativeTfm = PathNavigation.getTransformation(relative);
		}
		public void updateState( SimpleMapField fld ){
			state = fld.getCell( absolutePath );
		}
		/**Change origin of the cell*/
		public void rebase( OrientedPath newOrigin ){
			absolutePath = newOrigin.attach(relativePath).path;
		}
	}
	
	/**re-enerate array of visible cells*/
	private void rebuildVisibleCells(){
		visibleCells = null;
		final
		ArrayList<VisibleCell> newCells = new ArrayList<FarPoincarePanel.VisibleCell>();
		
		Util.forField(visibleRadius, new Function1<Path, Boolean>() {
			public Boolean call(Path arg) {
				newCells.add(new VisibleCell( arg, viewCenter ));
				return true;
			}
		});
		//TODO avoid copying array back and forth.
		visibleCells = new VisibleCell[ newCells.size() ];
		visibleCells = newCells.toArray( visibleCells );
	}
	
	private void updateCellsState(){
		for (int i = 0; i < visibleCells.length; i++) {
			visibleCells[i].updateState(field);
		}
	}
	
	public FarPoincarePanel( SimpleMapField f ){
		field = f;
		rebuildVisibleCells();
		updateCellsState();
	}
	
	public void setField( SimpleMapField f ){
		field = f;
		updateCellsState();
	}
	
	public void setViewRadius( int r ){
		assert r >= 1;
		visibleRadius = r;
		rebuildVisibleCells();
		updateCellsState();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	
		Dimension sz = getSize();
		Graphics2D g2 = (Graphics2D) g;
		if( antiAlias )
			g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.translate( sz.width/2, sz.height/2);
	    
		paintContents( g2, sz );
	}
	/**Draw grid cells*/
	private void doShowGrid( Graphics2D g2 ) {
		if ( gridShape == null ){
			//Get the transform for the central cell
			Path centralCell = PathNavigation.point2path( viewTransform.hypInverse().tfmVector( new double[]{0,0,1}));
			Transform centralTfm = PathNavigation.getTransformation(centralCell);
			Transform relativeTfm = viewTransform.mul( centralTfm );
			gridShape = gridDrawer.createShape(relativeTfm);
		}
		
		g2.setColor( clrGrid );
		g2.setStroke( new BasicStroke(0) );
		g2.draw( gridShape );
	}
	/**Shift view*/
	public void offsetView(double x, double y){
		Transform offsetTfm = new Transform();
		offsetTfm.setShift( x, y );
		setView( offsetTfm.mul( viewTransform ) );
	}
	
	/**Rotate vies by some angle*/
	public void rotateView( double angle ){
		Transform rot = new Transform();
		rot.setRot(angle);
		setView( rot.mul( viewTransform) );
	}
	
	/**Creates a shape, that is a projection of the field*/
	private Shape createFieldShape(){
		GeneralPath path = new GeneralPath();
		for (int i = 0; i < visibleCells.length; i++) {
			if ( visibleCells[i].state != 0 )
				createCellShape(path, viewTransform.mul( visibleCells[i].relativeTfm) );
		}
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

	private void createCellShape( GeneralPath path, Transform pathTfm ){
		//double[] xyt = pathTfm.tfmVector(new double[]{0,0,1} );
		double t = pathTfm.getAt(2, 2);
		if ( t < 100 )
			PoincareGraphics.renderPoincarePolygon( path, pathTfm, pentagonPoints, true);
	}
	
	static double len2( double x, double y ){
		return x*x+y*y;
	}
	private double getScale( Dimension size ){
		return Math.max( 1, 0.5 * ( Math.min( size.width, size.height ) - margin ) );
	}

	private void paintContents(Graphics2D g2, Dimension size) {
		if ( cellsShape == null ){
				cellsShape = createFieldShape();				
		}
		//AffineTransform oldTfm = g2.getTransform();
		double scale = getScale( size );
		g2.scale(scale, scale);
		
		g2.setColor(clrCell);
		g2.fill(cellsShape);

		if ( showGrid ) doShowGrid( g2 );
		
		g2.setColor(clrBorder);
		g2.setStroke( new BasicStroke(0) );
		g2.drawOval(-1, -1, 2, 2);
		
		//g2.setTransform(oldTfm);
	}

	/**Set view matrix and redraw view*/
	private void setView( Transform tfm ){
		viewTransform = tfm;
		viewTfmModifCounter ++;
		if (viewTfmModifCounter > fixTransformEvery ){
			viewTfmModifCounter = 0;
			viewTransform= viewTransform.fix();
		}
		cellsShape = null;
		gridShape = null;
		repaint();
	}
	/**Updates cell states and causes repaint*/
	public void update(){
		synchronized(field){
			updateCellsState();
		}
		cellsShape = null;
		repaint();
	}
	/**Move view to the origin*/
	public void centerView(){
		viewTfmModifCounter = 0;
		setView( viewTransform.setEye() );
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
		
		Path relativePath = PathNavigation.point2path(viewTransform.hypInverse().tfmVector(point));
		return viewCenter.attach(relativePath).path;
	}
	public void rebase( OrientedPath newCenter ){
		viewCenter = newCenter;
		rebuildVisibleCells();
		update();
		centerView();
	}
}
