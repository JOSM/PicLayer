// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JOptionPane;

public class ResultCheckView {

    public static int showAndChoose() {
        Object[] options = {tr("accept"), tr("reset")};
        String title = tr("AutoCalibration - check calibration");
        String msg = tr("<html>Is the image calibrated correctly?</html>");

        return JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }


}
