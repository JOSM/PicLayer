// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions.transform.autocalibrate;


import java.awt.event.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.affine.MovePointAction;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.autocalibrate.utils.GeoLine;
import org.openstreetmap.josm.plugins.piclayer.actions.transform.autocalibrate.utils.ObservableArrayList;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.CalibrationErrorView;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.CalibrationWindow;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.ReferenceOptionView;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.ResultCheckView;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.SelectLayerView;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;

/**
 * Class handling connection between {@link AutoCalibratePictureAction} and GUIs.
 * Info at https://wiki.openstreetmap.org/wiki/User:Rebsc
 */
public class AutoCalibrateHandler {

    private PicLayerAbstract currentPicLayer;
    private CalibrationWindow mainWindow;
    private File referenceFile;
    private Layer referenceLayer;
    private final AutoCalibration calibration;
    private ObservableArrayList<Point2D> originPointList;       // points set on picture to calibrate, scaled in LatLon
    private ObservableArrayList<Point2D> referencePointList;    // points of reference data, scaled in LatLon
    private double distance1To2;    // in meter
    private double distance2To3;    // in meter

    public AutoCalibrateHandler() {
        this.originPointList = new ObservableArrayList<>(3);
        this.referencePointList = new ObservableArrayList<>(3);
        this.distance1To2 = 0.0;
        this.distance2To3 = 0.0;
        this.referenceFile = null;
        this.referenceLayer = null;
        this.currentPicLayer = null;
        this.mainWindow = new CalibrationWindow();
        addListenerToMainView();
        this.calibration = new AutoCalibration();
    }

    /**
     * Method adds listener to main view
     */
    private void addListenerToMainView() {
        if (this.mainWindow != null) {
            this.mainWindow.addHelpButtonListener(new HelpButtonListener());
            this.mainWindow.addEdgePointButtonListener(new EdgePointsButtonListener());
            this.mainWindow.addDistance1FieldListener(new TextField1Listener());
            this.mainWindow.addDistance2FieldListener(new TextField2Listener());
            this.mainWindow.addOpenFileButtonListener(new OpenFileButtonListener());
            this.mainWindow.addSelectLayerButtonListener(new SelectLayerButtonListener());
            this.mainWindow.addReferencePointButtonListener(new RefPointsButtonListener());
            this.mainWindow.addCancelButtonListener(new CancelButtonListener());
            this.mainWindow.addRunButtonListener(new RunButtonListener());
            this.mainWindow.addFrameWindowListener(getToolWindowListener());
        }
    }

    /**
     * Help button listener
     */
    private static class HelpButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String topic = "Plugin/PicLayer";
            // open help browser
            HelpBrowser.setUrlForHelpTopic(Optional.of(topic).orElse("/"));
        }
    }

    /**
     * Open file button listener
     */
    private class OpenFileButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            mainWindow.setVisible(false);
            JButton openButton = mainWindow.getOpenButton();
            JFileChooser fileChooser = mainWindow.getFileChooser();

            if (event.getSource() == openButton) {
                int openValue = fileChooser.showOpenDialog(mainWindow);
                if (openValue == JFileChooser.APPROVE_OPTION) {
                    referenceFile = fileChooser.getSelectedFile();
                    addFileInNewLayer(referenceFile);
                }
            }
            if (referenceFile != null) {
                mainWindow.setReferenceFileNameValue(referenceFile.getName());
                mainWindow.setVisible(true);
            }
            mainWindow.setVisible(true);
        }

        private void addFileInNewLayer(File file) {
            List<File> files = new ArrayList<>();
            files.add(file);
            OpenFileAction.openFiles(files);
        }
    }

    /**
     * Select layer button listener
     */
    private class SelectLayerButtonListener implements ActionListener {
        private SelectLayerView selector;

        @Override
        public void actionPerformed(ActionEvent event) {
            mainWindow.setVisible(false);

            selector = new SelectLayerView();
            selector.setVisible(true);

            selector.setOkButtonListener(new SelectorOkButtonListener());
            selector.setCancelButtonListener(new SelectorCancelButtonListener());
        }

        private class SelectorCancelButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                selector.getFrame().dispatchEvent(new WindowEvent(selector.getFrame(), WindowEvent.WINDOW_CLOSING));
                mainWindow.setVisible(true);
            }
        }

        private class SelectorOkButtonListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = (String) selector.getList().getSelectedValue();

                if (filename != null) {
                    for (Layer l : MainApplication.getLayerManager().getLayers()) {
                        if (l.getName().equals(filename)) {
                            referenceLayer = l;
                            MainApplication.getLayerManager().setActiveLayer(l);
                        }
                    }
                }

                if (referenceLayer != null) {
                    mainWindow.setReferenceFileNameValue(filename);
                } else calibration.showErrorView(CalibrationErrorView.SELECT_LAYER_ERROR);

                selector.setVisible(false);
                mainWindow.setVisible(true);
            }
        }
    }

    /**
     * Cancel button listener
     */
    private class CancelButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            reset();
            removeListChangedListener();
        }
    }

    /**
     * Run button listener
     */
    private class RunButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // calibrate
            callCalibration();
            currentPicLayer.resetDrawReferencePoints();
            currentPicLayer.invalidate();
            MainApplication.getLayerManager().setActiveLayer(currentPicLayer);
            mainWindow.setVisible(false);
            // let user check calibration
            int selectedValue = ResultCheckView.showAndChoose();
            if (selectedValue == 1) {
                currentPicLayer.getTransformer().resetCalibration();
                currentPicLayer.invalidate();
            }
            reset();
        }

        /**
         * Method to call calibrating method for given image.
         */
        private void callCalibration() {
            if (currentPicLayer != null && !originPointList.isEmpty() && !referencePointList.isEmpty()
                    && distance1To2 != 0.0 && distance2To3 != 0.0) {
                calibration.setCurrentLayer(currentPicLayer);
                calibration.setStartPositions(originPointList);
                calibration.setEndPositions(referencePointList);
                calibration.setDistance1To2(distance1To2);
                calibration.setDistance2To3(distance2To3);
                calibration.calibrate();
            } else calibration.showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
        }
    }

    /**
     * Edge button listener
     */
    private class EdgePointsButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainWindow.setVisible(false);
            MainApplication.getLayerManager().setActiveLayer(currentPicLayer);
            // switch to select mode
            MovePointAction selectPointMode = new MovePointAction();
            MainApplication.getMap().selectMapMode(selectPointMode);
        }
    }

    /**
     * Method to get windowListener for main window
     *
     * @return adapter
     */
    private WindowAdapter getToolWindowListener() {
        return new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent wEvt) {
                ((JFrame) wEvt.getSource()).toFront();
            }

            @Override
            public void windowClosing(WindowEvent wEvt) {
                reset();
                removeListChangedListener();
            }
        };
    }

    /**
     * Origin points list listener
     */
    private class OriginSizePropertyListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            int size = (int) event.getNewValue();
            if (currentPicLayer.getTransformer().getLatLonOriginPoints() != null) {
                originPointList.clear();
                originPointList.addAll(currentPicLayer.getTransformer().getLatLonOriginPoints());
                mainWindow.setOriginPoints(originPointList);
            }
            if (size == 3) {
                mainWindow.setVisible(true);
                currentPicLayer.getTransformer().getLatLonOriginPoints().removePropertyChangeListener(this);
            }
        }
    }

    /**
     * Reference points list listener
     */
    private class RefSizePropertyListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            int size = (int) event.getNewValue();
            mainWindow.setReferencePoints(referencePointList);
            if (size == 3) {
                mainWindow.setVisible(true);
                referencePointList.removePropertyChangeListener(this);
            }
        }
    }

    /**
     * Distance point 1 to point 2 field listener
     */
    private class TextField1Listener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            currentPicLayer.setDrawOrigin1To2Line(true);
            currentPicLayer.invalidate();
            mainWindow.setDistance1Field("");
        }

        @Override
        public void focusLost(FocusEvent e) {
            currentPicLayer.setDrawOrigin1To2Line(false);
            currentPicLayer.invalidate();

            String value = mainWindow.getDistance1FieldText().replace(",", ".");
            if (validValue(value)) {
                mainWindow.getDistance1Field().selectAll();
                mainWindow.setDistance1Value(value);
                mainWindow.refresh();
                distance1To2 = Double.parseDouble(value);
            }
        }
    }

    /**
     * Distance point 2 to point 3 field listener
     */
    private class TextField2Listener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            currentPicLayer.setDrawOrigin2To3Line(true);
            currentPicLayer.invalidate();
            mainWindow.setDistance2Field("");
        }

        @Override
        public void focusLost(FocusEvent e) {
            currentPicLayer.setDrawOrigin2To3Line(false);
            currentPicLayer.invalidate();

            String value = mainWindow.getDistance2FieldText().replace(",", ".");
            if (validValue(value)) {
                mainWindow.getDistance2Field().selectAll();
                mainWindow.setDistance2Value(value);
                mainWindow.refresh();
                distance2To3 = Double.parseDouble(value);
            }
        }
    }

    /**
     * Reference add points button listener
     */
    private class RefPointsButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainWindow.setVisible(false);
            int selectedValue = ReferenceOptionView.showAndChoose();

            if (selectedValue == 0) {    // defined
                MainApplication.getMap().mapView.addMouseListener(new RefDefinedPointsMouseListener());
            } else if (selectedValue == 1) {    // manual
                MainApplication.getMap().mapView.addMouseListener(new RefManualPointsMouseListener());
                MainApplication.getMap().mapView.addMouseMotionListener(new RefManualPointsMouseMotionListener());
            }
        }
    }

    /**
     * Mouse listener for manual reference selection option
     */
    private class RefManualPointsMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (referenceFile == null && referenceLayer == null) {
                MainApplication.getMap().mapView.removeMouseListener(this);
                return;
            }

            LatLon latLonPoint = MainApplication.getMap().mapView.getLatLon(e.getPoint().getX(), e.getPoint().getY());
            Point2D llPoint = latLonToPoint2D(latLonPoint);
            if (referencePointList.isEmpty()) {
                referencePointList.add(llPoint);
                currentPicLayer.setDrawReferencePoints(true);
                currentPicLayer.getTransformer().addLatLonRefPoint(e.getPoint());

            } else if (referencePointList.size() == 1) {
                Point2D currentValidPoint = currentPicLayer.getRefLine1To2().getEndPoint();
                referencePointList.add(currentValidPoint);
                currentPicLayer.setDrawReferencePoints(true);
                currentPicLayer.getTransformer().addLatLonRefPoint(currentValidPoint);

            } else if (referencePointList.size() == 2) {
                Point2D currentValidPoint = currentPicLayer.getRefLine2To3().getEndPoint();
                referencePointList.add(currentValidPoint);
                currentPicLayer.setDrawReferencePoints(true);
                currentPicLayer.getTransformer().addLatLonRefPoint(currentValidPoint);
            }
            currentPicLayer.invalidate();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // do nothing
        }
    }

    /**
     * Mouse motion listener for manual reference selection option
     */
    private class RefManualPointsMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (referenceFile == null && referenceLayer == null) {
                MainApplication.getMap().mapView.removeMouseMotionListener(this);
                return;
            }
            // get mouse position
            LatLon latLonPoint = MainApplication.getMap().mapView.getLatLon(e.getPoint().getX(), e.getPoint().getY());
            if (referencePointList.size() == 1) {
                // Show line between point 1 and possible points 2
                GeoLine line = new GeoLine(referencePointList.get(0), latLonToPoint2D(latLonPoint));
                Point2D trStart = referencePointList.get(0);
                Point2D trEnd = line.pointOnLine(distance1To2);
                currentPicLayer.setDrawRef1To2Line(trStart, trEnd);
            } else if (referencePointList.size() == 2) {
                // Show line between point 2 and possible points 3
                GeoLine line = new GeoLine(referencePointList.get(1), latLonToPoint2D(latLonPoint));
                Point2D trStart = referencePointList.get(1);
                Point2D trEnd = line.pointOnLine(distance2To3);
                currentPicLayer.setDrawRef2To3Line(trStart, trEnd);
                currentPicLayer.unsetDrawRef1ToRef2Line();
            } else {
                currentPicLayer.unsetDrawRef1ToRef2Line();
                currentPicLayer.unsetDrawRef2ToRef3Line();
            }
            currentPicLayer.invalidate();
        }
    }

    /**
     * Mouse listener for defined reference selection option
     */
    private class RefDefinedPointsMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (referenceFile == null && referenceLayer == null) {
                MainApplication.getMap().mapView.removeMouseListener(this);
                return;
            }

            if (referencePointList.size() < 3) {
                // get point in lat/lon
                LatLon latLonPoint = MainApplication.getMap().mapView.getLatLon(e.getPoint().getX(), e.getPoint().getY());
                double latY = latLonPoint.getY();
                double lonX = latLonPoint.getX();
                Point2D llPoint = new Point2D.Double(lonX, latY);

                // get current data set and find closest point
                Point2D closestPoint = null;
                double shortestDistance = 1000000.0;    // default value
                double tmpDistance;
                DataSet data = MainApplication.getLayerManager().getEditDataSet();

                for (Node node : data.getNodes()) {
                    tmpDistance = llPoint.distance(node.lon(), node.lat());

                    if (tmpDistance < shortestDistance) {
                        closestPoint = new Point2D.Double(node.lon(), node.lat());
                        shortestDistance = tmpDistance;
                    }
                }

                if (closestPoint != null) {
                    // add closest point to reference list
                    referencePointList.add(closestPoint);
                    // draw point
                    currentPicLayer.setDrawReferencePoints(true);
                    currentPicLayer.getTransformer().addLatLonRefPoint(closestPoint);
                    currentPicLayer.invalidate();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // do nothing
        }
    }

    public CalibrationWindow getMainWindow() {
        return this.mainWindow;
    }

    public CalibrationErrorView getErrorView() {
        return new CalibrationErrorView();
    }

    public void prepare(PicLayerAbstract layer) {
        this.currentPicLayer = layer;
        addListChangedListenerToPointLists();
        // if origin points, add them
        ObservableArrayList<Point2D> list = layer.getTransformer().getLatLonOriginPoints();
        if (list != null && list.size() == 3) {
            this.originPointList = list;
            this.mainWindow.setOriginPoints(originPointList);
        } else {
            resetLists();
        }
    }

    private void reset() {
        originPointList = new ObservableArrayList<>(3);
        referencePointList = new ObservableArrayList<>(3);
        distance1To2 = 0.0;
        distance2To3 = 0.0;
        this.referenceFile = null;
        this.referenceLayer = null;
        resetLists();
        currentPicLayer.resetMarkersAndUsabilityValues();
        currentPicLayer.invalidate();
        mainWindow.setVisible(false);
        mainWindow = new CalibrationWindow();
        addListenerToMainView();
    }

    private void resetLists() {
        currentPicLayer.getTransformer().clearOriginPoints();
        currentPicLayer.getTransformer().clearLatLonOriginPoints();
        currentPicLayer.getTransformer().clearLatLonRefPoints();
    }

    private void addListChangedListenerToPointLists() {
        OriginSizePropertyListener originListener = new OriginSizePropertyListener();
        currentPicLayer.getTransformer().getLatLonOriginPoints().addPropertyChangeListener(originListener);
        RefSizePropertyListener refListener = new RefSizePropertyListener();
        this.referencePointList.addPropertyChangeListener(refListener);
    }

    private void removeListChangedListener() {
        currentPicLayer.getTransformer().getLatLonOriginPoints().removeAllListener();
        referencePointList.removeAllListener();
    }

    private boolean validValue(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NullPointerException | NumberFormatException ex) {
            return false;
        }
    }

    private Point2D latLonToPoint2D(LatLon ll) {
        ICoordinateFormat mCoord = CoordinateFormatManager.getDefaultFormat();
        double latY = Double.parseDouble(mCoord.latToString(ll));
        double lonX = Double.parseDouble(mCoord.lonToString(ll));
        return new Point2D.Double(lonX, latY);
    }
}
