// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions.transform.affine;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.piclayer.actions.GenericPicTransformAction;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;


/**
 * old version - ctrl move point (not transforming picture)
 */
public class MovePointAction extends GenericPicTransformAction {

    /**
     * Create a new move point action
     */
    public MovePointAction() {
        super(tr("PicLayer Move point"), tr("Point added/moved"), "movepoint", tr("Drag or create point on the picture"),
                ImageProvider.getCursor("crosshair", null));
    }

    @Override
    protected void doAction(MouseEvent e) {
        try {
            Point2D pressed = currentLayer.transformPoint(e.getPoint());
            if (selectedPoint != null) {
                currentLayer.getTransformer().replaceOriginPoint(selectedPoint, pressed);
                selectedPoint = pressed;
            }
        } catch (NoninvertibleTransformException e1) {
            Logging.error(e1);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (currentLayer == null)
            return;

        try {
            Point2D pressed = currentLayer.transformPoint(e.getPoint());
            if (selectedPoint == null) {
                currentLayer.getTransformer().addOriginPoint(pressed);

                // add untransformed lat/lon for AutoCalibration action
                LatLon latLonPoint = MainApplication.getMap().mapView.getLatLon(e.getPoint().getX(), e.getPoint().getY());
                double latY = latLonPoint.getY();
                double lonX = latLonPoint.getX();
                currentLayer.getTransformer().addLatLonOriginPoint(new Point2D.Double(lonX, latY));
            }
            if (currentCommand != null) {
                currentCommand.addIfChanged();
            }
        } catch (NoninvertibleTransformException e1) {
            Logging.error(e1);
        }
    }

    @Override
    public void enterMode() {
        super.enterMode();
        updateDrawPoints(true);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        updateDrawPoints(false);
    }

}
