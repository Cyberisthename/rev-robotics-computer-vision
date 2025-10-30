package org.firstinspires.ftc.teamcode.vision;

import androidx.annotation.NonNull;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;

/**
 * Immutable container that exposes the most relevant information for a detected AprilTag.
 */
public final class AprilTagDetectionData {

    private final int id;
    private final String tagFamily;
    private final double centerX;
    private final double centerY;
    private final double range;
    private final double bearing;
    private final double elevation;
    private final double translationX;
    private final double translationY;
    private final double translationZ;
    private final double yaw;
    private final double pitch;
    private final double roll;
    private final DistanceUnit distanceUnit;
    private final AngleUnit angleUnit;

    private AprilTagDetectionData(int id,
                                  String tagFamily,
                                  double centerX,
                                  double centerY,
                                  double range,
                                  double bearing,
                                  double elevation,
                                  double translationX,
                                  double translationY,
                                  double translationZ,
                                  double yaw,
                                  double pitch,
                                  double roll,
                                  DistanceUnit distanceUnit,
                                  AngleUnit angleUnit) {
        this.id = id;
        this.tagFamily = tagFamily;
        this.centerX = centerX;
        this.centerY = centerY;
        this.range = range;
        this.bearing = bearing;
        this.elevation = elevation;
        this.translationX = translationX;
        this.translationY = translationY;
        this.translationZ = translationZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.distanceUnit = distanceUnit;
        this.angleUnit = angleUnit;
    }

    @NonNull
    public static AprilTagDetectionData from(@NonNull AprilTagDetection detection,
                                             @NonNull DistanceUnit distanceUnit,
                                             @NonNull AngleUnit angleUnit) {
        AprilTagPoseFtc pose = detection.ftcPose;

        return new AprilTagDetectionData(
                detection.id,
                detection.metadata != null ? detection.metadata.tagFamily : "unknown",
                detection.center.x,
                detection.center.y,
                pose != null ? distanceUnit.fromMeters(pose.range) : Double.NaN,
                pose != null ? angleUnit.fromRadians(pose.bearing) : Double.NaN,
                pose != null ? angleUnit.fromRadians(pose.elevation) : Double.NaN,
                pose != null ? distanceUnit.fromMeters(pose.x) : Double.NaN,
                pose != null ? distanceUnit.fromMeters(pose.y) : Double.NaN,
                pose != null ? distanceUnit.fromMeters(pose.z) : Double.NaN,
                pose != null ? angleUnit.fromRadians(pose.yaw) : Double.NaN,
                pose != null ? angleUnit.fromRadians(pose.pitch) : Double.NaN,
                pose != null ? angleUnit.fromRadians(pose.roll) : Double.NaN,
                distanceUnit,
                angleUnit
        );
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getTagFamily() {
        return tagFamily;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRange() {
        return range;
    }

    public double getBearing() {
        return bearing;
    }

    public double getElevation() {
        return elevation;
    }

    public double getTranslationX() {
        return translationX;
    }

    public double getTranslationY() {
        return translationY;
    }

    public double getTranslationZ() {
        return translationZ;
    }

    public double getYaw() {
        return yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    @NonNull
    public DistanceUnit getDistanceUnit() {
        return distanceUnit;
    }

    @NonNull
    public AngleUnit getAngleUnit() {
        return angleUnit;
    }
}
