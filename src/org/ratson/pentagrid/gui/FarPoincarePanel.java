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

import org.ratson.pentagrid.OrientedPath;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Transform;
import org.ratson.pentagrid.Util;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.pentagrid.gui.PoincarePanel.PointDbl;
import org.ratson.pentagrid.gui.poincare_panel.PoincarePanelEvent;
import org.ratson.pentagrid.gui.poincare_panel.PoincarePanelListener;
import org.ratson.util.Function1;

public class FarPoincarePanel extends JComponent {
	private OrientedPath viewCenter = new OrientedPath(Path.getRoot(), 0);
	private ArrayList<VisibleCell> visibleCells = new ArrayList<FarPoincarePanel.VisibleCell>();
	private int visibleRadius = 5;
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
	public Color clrExportBg = Color.WHITE;
	private int margin = 30;
	private ArrayList<PoincarePanelListener> panelEventListeners = new ArrayList<PoincarePanelListener>();

	/**Represents one cell, shown on display. Stores path to this cell, its relative transformation and state*/
	static final class VisibleCell{
		Path absolutePath;
		Transform relativeTfm;
		int state=0;
		public VisibleCell( Path relative, OrientedPath origin ){
			absolutePath = origin.attach(relative).path;
			relativeTfm = PathNavigation.getTransformation(relative);
		}
		public boolean updateState( SimpleMapField fld ){
			int oldState = state;
			state = fld.getCell( absolutePath );
			return oldState != state;
		}
	}
	
	/**re-enerate array of visible cells*/
	private void rebuildVisibleCells(){
		visibleCells.clear();
		Util.forField(visibleRadius, new Function1<Path, Boolean>() {
			public Boolean call(Path relPath) {
				visibleCells.add(new VisibleCell( relPath, viewCenter ));
				return true;
			}
		});
		visibleCells.trimToSize();
	}
	
	private boolean updateCellsState(){
		boolean changed = false;
		synchronized(field){
			for ( VisibleCell c : visibleCells ) {
				changed = c.updateState(field) || changed;
			}
		}
		return changed;
	}
	
	public FarPoincarePanel( SimpleMapField f ){
		field = f;
		rebuildVisibleCells();
		updateCellsState();
	}
	
	public void setField( SimpleMapField f ){
		field = f;
		update();
	}
	
	public void setViewRadius( int r ){
		assert r >= 1;
		if ( r != visibleRadius ){
			visibleRadius = r;
			rebuildVisibleCells();
			cellsShape = null;
			update();
		}
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
			gridShape = gridDrawer.createShape(viewTransform);
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
		for ( VisibleCell c: visibleCells) {
			if ( c.state != 0 )
				createCellShape(path, viewTransform.mul( c.relativeTfm) );
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
		adjustViewCenter();
		cellsShape = null;
		gridShape = null;
		repaint();
	}
	/**Updates cell states and causes repaint*/
	public void update(){
		if ( updateCellsState() ){ //repaint only if some cells were changed
			cellsShape = null;
			repaint();
		}
	}
	/**Move view to the origin*/
	public void centerView(){
		viewTfmModifCounter = 0;
		viewCenter = new OrientedPath( Path.getRoot(), 0);
		rebuildVisibleCells();
		updateCellsState();
		setView( viewTransform.setEye() );
	}
	/**Given the point in th view coordinates, return path to the cell, containing it
	 * Path is relative to the view center*/
	public Path mouse2cellPathRel( int x, int y ){
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
		
		return PathNavigation.point2path(viewTransform.hypInverse().tfmVector(point));
	}
	/**Absolute path to the cell*/
	public Path mouse2cellPath( int x, int y ){
		Path relativePath = mouse2cellPathRel(x, y); 
		return viewCenter.attach(relativePath).path;
	}
	/**Put view center to the specified cell, and reset view offset*/
	public void setOrigin( OrientedPath newCenter ){
		viewCenter = newCenter;
		rebuildVisibleCells();
		setView( viewTransform.setEye() );
		update();
	}
	public OrientedPath getOrigin(){
		return viewCenter;
	}
	/**Shift view origin by the given offset, and adjust transformation so that view does not change*/
	public void rebaseRelative( Path offset ){
		if (offset.isRoot()) return; //nothing to do
		OrientedPath newCenter = viewCenter.attach( offset );
		PoincarePanelEvent e = new PoincarePanelEvent(viewCenter, newCenter);
		Transform offsetTfm = PathNavigation.getTransformation( offset );
		viewCenter = newCenter;
		viewTransform = viewTransform.mul( offsetTfm );
		rebuildVisibleCells();
		update();
		fireOriginChanged(e);
	}
	/**adjust view center, setting it to the cell, nearest to the geometrical center of the Poincare circle*/
	public void adjustViewCenter(){
		double [] point = new double[]{ 0,0,1 };		
		try{
			Path centerPath = PathNavigation.point2path(viewTransform.hypInverse().tfmVector(point)); //path to the cell at the geometric center
			rebaseRelative( centerPath );
		}catch( RuntimeException err ){
			System.err.println( "Failed to adjust path: "+err.getMessage() );
		}
	}
	public BufferedImage exportImage( Dimension size, boolean antiAlias ){
		BufferedImage img = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)img.getGraphics();
		g.setColor( clrExportBg );
		g.fillRect(0, 0, size.width, size.height);
		if( antiAlias )
			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate(size.width/2, size.height/2);
		paintContents( g, size );
		return img;
	}

	public void AddPoincarePanelListener(
			PoincarePanelListener listener) {
		panelEventListeners.add( listener );
	}
	
	private void fireOriginChanged( PoincarePanelEvent e ){
		for( PoincarePanelListener l: panelEventListeners ) l.originChanged(e);
	}
}
