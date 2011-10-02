package org.ratson.pentagrid.gui;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.PathNavigation;
import org.ratson.pentagrid.Transform;
/**Used to draw pentagonal grids on the poincare plane*/
public class GridPainter {
	ArrayList<Transform> cellTransforms = new ArrayList<Transform>();
	public GridPainter( int levels ){
		if( levels > 0){
			walkPentagon( Path.getRoot(), levels );
			cellTransforms.trimToSize();
		}
	}
	
	/**Recursive walker, visiting all cells of the pentagonal field within several levels.*/
	private void walkPentagon(Path root, int levels) {
		cellTransforms.add( PathNavigation.getTransformation(root));
		if (levels <= 1) return;
		for( int childIndex = 1; childIndex <= root.maxChildIndex(); ++ childIndex ){
			walkPentagon( root.child(childIndex), levels-1);
		}
	}
	
	/**Creates a shape for the cells */
	public Shape createShape( Transform centralCellTfm ){
		GeneralPath path = new GeneralPath();
		for (int i = 0; i < cellTransforms.size(); i++) {
			Shape cellShape = PoincareGraphics.renderPoincarePolygon(
					centralCellTfm.mul(cellTransforms.get(i)), 
					pentagonPoints, 
					true);
			if ( cellShape != null) path.append( cellShape, false);
		}
		return path;		
	}
	
	/**Precalculated coordinates of a pentagon*/
	static double[] pentagonPoints = new double[10];
	static{
		double r = Math.sqrt (2 / Math.sqrt (5));
		for (int i = 0; i < 10; i+=2) {
			double angle = Math.PI / 5 * i;
			pentagonPoints[i] = Math.cos(angle)*r;
			pentagonPoints[i+1] = Math.sin(angle)*r;
		}			
	}
	
}
