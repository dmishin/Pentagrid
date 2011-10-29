package org.ratson.pentagrid.gui.poincare_panel;

import org.ratson.pentagrid.OrientedPath;

public class PoincarePanelEvent {
	public OrientedPath oldOrigin;
	public OrientedPath newOrigin;
	public PoincarePanelEvent( OrientedPath old, OrientedPath new_) {
		oldOrigin = old;
		newOrigin = new_;
	}
}
