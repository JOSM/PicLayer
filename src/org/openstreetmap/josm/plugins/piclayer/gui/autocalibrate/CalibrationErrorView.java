// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * Class providing error views for calibration action.
 */
public class CalibrationErrorView {

    public final static String SELECT_LAYER_ERROR = tr("Could not select layer!");
    public final static String CALIBRATION_ERROR = tr("Calibration failed!");
    public final static String DIMENSION_ERROR = tr("<html> Calibration failed!<br>"
            + "There must be a mistake -<br>"
            + "dimension of object differ too much from original.<br>"
            + "</html>");
    private final JLabel textLabel;

    public CalibrationErrorView() {
        textLabel = new JLabel();
    }

    /**
     * Method to show dialog window
     *
     * @param errorMsg message to show on GUI
     */
    public void show(String errorMsg) {
        JEditorPane editor = new JEditorPane();
        editor.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        editor.setEditable(false);
        editor.setBackground(Color.lightGray);

        textLabel.setText(errorMsg);
        JOptionPane.showMessageDialog(null,
                textLabel,
                "PicLayer calibration error",
                JOptionPane.ERROR_MESSAGE);
    }

}
