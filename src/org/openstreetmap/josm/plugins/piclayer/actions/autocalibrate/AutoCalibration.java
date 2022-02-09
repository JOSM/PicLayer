// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions.autocalibrate;

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.plugins.piclayer.actions.autocalibrate.utils.GeoLine;
import org.openstreetmap.josm.plugins.piclayer.gui.autocalibrate.CalibrationErrorView;
import org.openstreetmap.josm.plugins.piclayer.layer.PicLayerAbstract;
import org.openstreetmap.josm.tools.Logging;


/**
 * Class for image calibration
 */
public class AutoCalibration {

    private PicLayerAbstract currentLayer;
    private List<Point2D> startPositions;    // raw data - LatLon
    private List<Point2D> endPositions;      // raw data - LatLon
    private double distance1To2;    // meter
    private double distance2To3;    // meter


    public AutoCalibration() {
        this.currentLayer = null;
        this.startPositions = new ArrayList<>(3);
        this.endPositions = new ArrayList<>(3);
        this.distance1To2 = 0.0;
        this.distance2To3 = 0.0;
    }

    /**
     * Calibrates Image with given data.
     * Sets start points to end points and corrects end points by passed distances between points.
     */
    public void calibrate() {
        // get start/end points
        List<Point2D> startPointList = currentLayer.getTransformer().getOriginPoints();                // in current layer scale
        List<Point2D> endPointList = correctedPoints(endPositions, distance1To2, distance2To3);        // in lat/lon scale

        if (currentLayer == null) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (startPointList == null) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (endPointList == null) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (startPointList.size() != 3) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (endPointList.size() != 3) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (distance1To2 == 0.0) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        if (distance2To3 == 0.0) {
            showErrorView(CalibrationErrorView.CALIBRATION_ERROR);
            return;
        }

        // calibrate
        Point2D tsPoint;        // transformed start point
        Point2D tePoint;        // transformed end point
        int index;

        // move all points to final state position
        for (Point2D endPos : endPointList) {
            index = endPointList.indexOf(endPos);
            tsPoint = startPointList.get(index);
            tePoint = translatePointToCurrentScale(endPos);
            currentLayer.getTransformer().updatePair(tsPoint, tePoint);
        }

        // check if image got distorted after calibration, if true reset and show error.
        if (!checkCalibration(startPositions, endPointList)) {
            currentLayer.getTransformer().resetCalibration();
            showErrorView(CalibrationErrorView.DIMENSION_ERROR);
        }
    }

    /**
     * Compare side ratios before/after calibration
     *
     * @param list        with ratios to compare to other list
     * @param compareList with ratios to compare to other list
     * @return true if ratios equals in range of (+-)0.5, else false
     */
    private boolean checkCalibration(List<Point2D> list, List<Point2D> compareList) {
        if (list.size() != 3 || compareList.size() != 3) return false;

        // check site ratios before and after calibration
        double dist12 = new GeoLine(list.get(0), list.get(1)).getDistance();
        double dist23 = new GeoLine(list.get(1), list.get(2)).getDistance();
        double dist13 = new GeoLine(list.get(0), list.get(2)).getDistance();
        double[] startRatio = {1, dist23 / dist12, dist13 / dist12};
        double compDist12 = new GeoLine(compareList.get(0), compareList.get(1)).getDistance();
        double compDist23 = new GeoLine(compareList.get(1), compareList.get(2)).getDistance();
        double compDist13 = new GeoLine(compareList.get(0), compareList.get(2)).getDistance();
        double[] compRatio = {1, compDist23 / compDist12, compDist13 / compDist12};
        double epsilon = 0.5;

        return compRatio[1] >= startRatio[1] - epsilon && compRatio[1] <= startRatio[1] + epsilon
                && compRatio[2] >= startRatio[2] - epsilon && compRatio[2] <= startRatio[2] + epsilon;
    }


    /**
     * Corrects points with given distances. Calculates new points on lines
     * between given points with given distances.
     *
     * @param points     need to be corrected
     * @param distance12 distance between point 1 and point 2 in meter
     * @param distance23 distance between point 2 and point 3 in meter
     * @return corrected points
     */
    private List<Point2D> correctedPoints(List<Point2D> points, double distance12, double distance23) {
        if (points != null && points.size() == 3) {
            List<Point2D> correctedList = new ArrayList<>();

            // get line between point1 and point2, point2 and point3
            GeoLine line12 = new GeoLine(points.get(0), points.get(1));
            GeoLine line23 = new GeoLine(points.get(1), points.get(2));

            // add point 0 - anchor
            correctedList.add(points.get(0));
            // add point on line12 at distance12
            correctedList.add(line12.pointOnLine(distance12));
            // get lat/lon offset of line12 point to origin point2
            double lonOffset = line12.pointOnLine(distance12).getX() - points.get(1).getX();
            double latOffset = line12.pointOnLine(distance12).getY() - points.get(1).getY();
            // get point on line23, add offset
            Point2D pointOnLine23 = line23.pointOnLine(distance23);
            Point2D correctedPointOnLine23 = new Point2D.Double(pointOnLine23.getX() + lonOffset, pointOnLine23.getY() + latOffset);
            // add point on line23 at distance23 corrected with offset from point on line12
            correctedList.add(correctedPointOnLine23);

            return correctedList;
        }
        return null;
    }

    /**
     * Method to translate {@code Point2D} to current layer scale.
     *
     * @param point to translate in LatLon
     * @return translated point in current layer scale
     */
    private Point2D translatePointToCurrentScale(Point2D point) {
        Point2D translatedPoint = null;
        LatLon ll;               // LatLon object from raw Point2D
        MapViewPoint en;         // MapViewPoint object from LatLon(ll) scaled in EastNorth(en)

        // put raw Point2D endPos into LatLon and transform LatLon into MapViewPoint (EastNorth)
        ll = new LatLon(point.getY(), point.getX());
        en = MainApplication.getMap().mapView.getState().getPointFor(ll);

        // transform EastNorth into current layer scale
        try {
            translatedPoint = currentLayer.transformPoint(new Point2D.Double(en.getInViewX(), en.getInViewY()));
        } catch (NoninvertibleTransformException e) {
            Logging.error(e);
        }

        return translatedPoint;
    }

    /**
     * Shows error view
     *
     * @param msg error msg
     */
    public void showErrorView(String msg) {
        AutoCalibrateHandler handler = new AutoCalibrateHandler();
        handler.getErrorView().show(msg);
    }

    /**
     * Set current active layer
     *
     * @param currentLayer to set active
     */
    public void setCurrentLayer(PicLayerAbstract currentLayer) {
        this.currentLayer = currentLayer;
    }

    /**
     * Set start positions scaled in Lat/Lon
     *
     * @param startPositions calibration start positions
     */
    public void setStartPositions(List<Point2D> startPositions) {
        this.startPositions = startPositions;
    }

    /**
     * Set end positions scaled in Lat/Lon
     *
     * @param endPositions calibration end positions
     */
    public void setEndPositions(List<Point2D> endPositions) {
        this.endPositions = endPositions;
    }

    /**
     * Set distance from point 1 to point 2 in start positions.
     * Scaled in meter.
     *
     * @param distance12 distance from point 1 to point 2 in start positions. Scaled in meter.
     */
    public void setDistance1To2(double distance12) {
        this.distance1To2 = distance12;
    }

    /**
     * Set distance from point 2 to point 3 in start positions.
     * Scaled in meter.
     *
     * @param distance23 distance from point 1 to point 2 in start positions. Scaled in meter.
     */
    public void setDistance2To3(double distance23) {
        this.distance2To3 = distance23;
    }
}
