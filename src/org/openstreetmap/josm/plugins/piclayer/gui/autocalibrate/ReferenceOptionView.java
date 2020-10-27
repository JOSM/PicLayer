package org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.*;

/**
 * @author rebsc
 */
public class ReferenceOptionView {

    public static int showAndChoose() {
        Object[] options = {tr("defined"), tr("manual")};
        String title = tr("AutoCalibration - choose selection type");
        String msg = tr("<html>Choose the type of selection you want to use.<br><br>"
                + "If reference points are defined in an already opened layer, <br>"
                + "choose <b>defined</b>-option and select the points.<br>"
                + "Else choose the <b>manual</b>-option and set the reference points manually.</html>");

        return JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]);
    }

}
