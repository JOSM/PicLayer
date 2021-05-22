// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.piclayer.actions.transform.autocalibrate.helper;

import org.openstreetmap.gui.jmapviewer.OsmMercator;

import java.awt.geom.Point2D;

/**
 * Class representing a GeoLine
 *
 * @author rebsc
 */
public class GeoLine {

    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;
    private final double distance;    // in meter

    public GeoLine(Point2D startPoint, Point2D endPoint) {
        this.lat1 = startPoint.getY();
        this.lon1 = startPoint.getX();
        this.lat2 = endPoint.getY();
        this.lon2 = endPoint.getX();
        this.distance = getDistance();
    }

    public GeoLine(double startLat, double startLon, double endLat, double endLon) {
        this.lat1 = startLat;
        this.lon1 = startLon;
        this.lat2 = endLat;
        this.lon2 = endLon;
        this.distance = getDistance();
    }

    public Point2D getStartPoint() {
        return new Point2D.Double(lon1, lat1);
    }

    public Point2D getEndPoint() {
        return new Point2D.Double(lon2, lat2);
    }

    /**
     * Method to get point on line at given distance from start point on.
     *
     * @param distanceFromStart distance from start point on. Distance in meter.
     * @return new point on line.
     */
    public Point2D pointOnLine(double distanceFromStart) {
        double newLat = lat1 + (lat2 - lat1) * (distanceFromStart / distance);
        double newLon = lon1 + (lon2 - lon1) * (distanceFromStart / distance);
        return new Point2D.Double(newLon, newLat);
    }

    /**
     * Haversine formula
     *
     * @return distance in meter
     */
    public double getDistance() {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return OsmMercator.EARTH_RADIUS * c;
    }

}
