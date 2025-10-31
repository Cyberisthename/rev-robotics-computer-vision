package org.firstinspires.ftc.teamcode.vision;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCalibration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.opencv.core.Mat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thin wrapper around {@link AprilTagProcessor} that caches detection data in a telemetry-friendly format
 * while still implementing FTC's {@link VisionProcessor} interface.
 */
public class AprilTagVisionProcessor implements VisionProcessor {

    public static class LensIntrinsics {
        public final double fx;
        public final double fy;
        public final double cx;
        public final double cy;

        public LensIntrinsics(double fx, double fy, double cx, double cy) {
            this.fx = fx;
            this.fy = fy;
            this.cx = cx;
            this.cy = cy;
        }
    }

    public static class Config {
        public AprilTagProcessor.TagFamily tagFamily = AprilTagProcessor.TagFamily.TAG_36h11;
        public boolean drawTagId = true;
        public boolean drawTagOutline = true;
        public boolean drawAxes = false;
        public boolean drawCube = false;
        public double tagSizeMeters = 0.165; // 6.5 inches default
        public DistanceUnit distanceUnit = DistanceUnit.METER;
        public AngleUnit angleUnit = AngleUnit.RADIANS;
        public @Nullable LensIntrinsics lensIntrinsics = null;
    }

    private final AprilTagProcessor delegate;
    private final DistanceUnit distanceUnit;
    private final AngleUnit angleUnit;

    private volatile List<AprilTagDetectionData> latestDetections = Collections.emptyList();

    private AprilTagVisionProcessor(@NonNull AprilTagProcessor delegate,
                                    @NonNull DistanceUnit distanceUnit,
                                    @NonNull AngleUnit angleUnit) {
        this.delegate = delegate;
        this.distanceUnit = distanceUnit;
        this.angleUnit = angleUnit;
    }

    public static AprilTagVisionProcessor fromConfig(@NonNull Config config) {
        AprilTagProcessor.Builder builder = new AprilTagProcessor.Builder();
        builder.setDrawTagID(config.drawTagId);
        builder.setDrawTagOutline(config.drawTagOutline);
        builder.setTagFamily(config.tagFamily);
        builder.setOutputUnits(config.distanceUnit, config.angleUnit);

        if (config.drawAxes) {
            invokeOptionalBuilderMethod(builder, "setDrawAxes", true);
        }
        if (config.drawCube) {
            invokeOptionalBuilderMethod(builder, "setDrawCube", true);
        }
        if (config.lensIntrinsics != null) {
            builder.setLensIntrinsics(
                    config.lensIntrinsics.fx,
                    config.lensIntrinsics.fy,
                    config.lensIntrinsics.cx,
                    config.lensIntrinsics.cy
            );
        }
        invokeOptionalBuilderMethod(builder, "setTagSize", config.tagSizeMeters);

        AprilTagProcessor processor = builder.build();
        return new AprilTagVisionProcessor(processor, config.distanceUnit, config.angleUnit);
    }

    @Override
    public void init(int width, int height, CameraCalibration calibration) {
        delegate.init(width, height, calibration);
    }

    @Override
    public Object processFrame(Mat frame, long captureTimeNanos) {
        Object context = delegate.processFrame(frame, captureTimeNanos);
        latestDetections = convert(delegate.getDetections());
        return context;
    }

    @Override
    public void onDrawFrame(Canvas canvas,
                            int onscreenWidth,
                            int onscreenHeight,
                            float scaleBmpPxToCanvasPx,
                            float scaleCanvasDensity,
                            Object userContext) {
        delegate.onDrawFrame(canvas, onscreenWidth, onscreenHeight, scaleBmpPxToCanvasPx, scaleCanvasDensity, userContext);
    }

    @NonNull
    public List<AprilTagDetectionData> getLatestDetections() {
        return latestDetections;
    }

    public AprilTagProcessor getDelegate() {
        return delegate;
    }

    public void setDecimation(float decimation) {
        invokeOptionalProcessorMethod("setDecimation", decimation);
    }

    private List<AprilTagDetectionData> convert(List<AprilTagDetection> detections) {
        if (detections == null || detections.isEmpty()) {
            return Collections.emptyList();
        }
        List<AprilTagDetectionData> results = new ArrayList<>(detections.size());
        for (AprilTagDetection detection : detections) {
            results.add(AprilTagDetectionData.from(detection, distanceUnit, angleUnit));
        }
        return Collections.unmodifiableList(results);
    }

    private void invokeOptionalProcessorMethod(String methodName, float value) {
        try {
            Method method = delegate.getClass().getMethod(methodName, float.class);
            method.invoke(delegate, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // No-op: feature is optional on older SDK releases
        }
    }

    private static void invokeOptionalBuilderMethod(AprilTagProcessor.Builder builder, String methodName, boolean value) {
        try {
            Method method = builder.getClass().getMethod(methodName, boolean.class);
            method.invoke(builder, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Optional metadata, ignore if unavailable
        }
    }

    private static void invokeOptionalBuilderMethod(AprilTagProcessor.Builder builder, String methodName, double value) {
        try {
            Method method = builder.getClass().getMethod(methodName, double.class);
            method.invoke(builder, value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            // Optional metadata, ignore if unavailable
        }
    }
}
