// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.plugins.piclayer.command.TransformCommand;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.plugins.piclayer.transform.PictureTransform;

/**
 * Action to reset Calibration.
 */
public class ResetCalibrationAction extends JosmAction {

    private final PicLayerAbstract layer;

    public ResetCalibrationAction(PicLayerAbstract layer, PictureTransform transformer) {
        super(tr("Reset Calibration"), null, tr("Reset calibration"), null, false);
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        TransformCommand currentCommand = new TransformCommand(layer, tr("Calibration reset"));
        layer.resetCalibration();
        currentCommand.addIfChanged();
    }
}
