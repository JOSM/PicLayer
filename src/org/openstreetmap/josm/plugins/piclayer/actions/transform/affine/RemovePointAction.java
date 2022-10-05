// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions.transform.affine;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.MouseEvent;

import org.openstreetmap.josm.plugins.piclayer.actions.GenericPicTransformAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Remove point on the picture
 */
public class RemovePointAction extends GenericPicTransformAction {

    /**
     * Create a new action to remove points
     */
    public RemovePointAction() {
        super(tr("PicLayer Remove point"), tr("Point removed"), "removepoint", tr("Remove point on the picture"),
                ImageProvider.getCursor("crosshair", null));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (currentLayer == null)
            return;

        if (selectedPoint != null) {
            currentLayer.getTransformer().removeLatLonOriginPoint(selectedPoint);
            currentLayer.getTransformer().removeOriginPoint(selectedPoint);
            selectedPoint = null;
        }
        if (currentCommand != null) {
            currentCommand.addIfChanged();
        }
    }

    @Override
    protected void doAction(MouseEvent e) {
        // Do nothing -- only remove points on deliberate clicks
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
