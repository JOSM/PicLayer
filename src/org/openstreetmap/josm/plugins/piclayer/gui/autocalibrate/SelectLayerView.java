// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;

public class SelectLayerView {

    private final JList<Object> list;
    private final JFrame frame;
    private JScrollPane scrollPane;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;


    public SelectLayerView() {
        list = new JList<>(getLayerNames());

        frame = new JFrame("Layer Selector");
        frame.setSize(400, 200);
        frame.setLocationRelativeTo(frame.getOwner());

        Container contentPane = frame.getContentPane();

        setScrollPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setButtonBar();
        setOKButton();
        setCancelButton();
        contentPane.add(buttonBar, BorderLayout.SOUTH);
    }

    private static String[] getLayerNames() {
        return MainApplication.getLayerManager().getLayers().stream()
                .map(Layer::getName).toArray(String[]::new);
    }

    public void setVisible(boolean value) {
        frame.setVisible(value);
    }

    public JFrame getFrame() {
        return this.frame;
    }

    public JList<Object> getList() {
        return this.list;
    }

    // COMPONENTS

    private void setScrollPane() {
        scrollPane = new JScrollPane(list);
    }

    private void setButtonBar() {
        buttonBar = new JPanel();
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttonBar.setLayout(new GridBagLayout());
        ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
        ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};
    }

    private void setOKButton() {
        okButton = new JButton();
        okButton.setText(tr("OK"));
        buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 5), 0, 0));
    }

    private void setCancelButton() {
        cancelButton = new JButton();
        cancelButton.setText(tr("Cancel"));
        buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
    }

    // LISTENER

    public void setOkButtonListener(ActionListener l) {
        this.okButton.addActionListener(l);
    }

    public void setCancelButtonListener(ActionListener l) {
        this.cancelButton.addActionListener(l);
    }

}
