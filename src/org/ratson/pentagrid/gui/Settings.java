package org.ratson.pentagrid.gui;

import java.awt.Color;
import java.io.Serializable;

/**Stores application settings*/
@SuppressWarnings("serial")
public class Settings implements Serializable, Cloneable{
	public int randomFieldRadius = 7;
	public double randomFillPercent = 0.5;
	public int exportImageSize=512;
	public boolean exportAntiAlias = true;
	public double offsetVelocity = 0.1;
	public double rotationVelocity = 0.05;
	public Color clrGrid = Color.LIGHT_GRAY,
		clrCell = Color.BLUE,
		clrBorder = Color.BLACK;
	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException( e );
		}
	}
}
