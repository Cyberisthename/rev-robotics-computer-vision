package org.firstinspires.ftc.teamcode.vision;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.robotcore.util.ElapsedTime;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor.TagFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * High level manager that owns camera setup, {@link VisionPortal} lifecycle and exposes AprilTag detections.
 */
public class AprilTagVisionManager {

    public static class Config {
        public boolean useWebcam = true;
        public String cameraName = "Webcam 1";
        public boolean enableLiveView = false;
        public EnumSet<TagFamily> tagFamilies = EnumSet.of(TagFamily.TAG_36h11);
        public boolean drawAxes = false;
        public boolean drawCube = false;
        public boolean drawTagOutline = true;
        public boolean drawTagId = true;
        public double tagSizeMeters = 0.165; // 6.5 in default
        public DistanceUnit distanceUnit = DistanceUnit.METER;
        public AngleUnit angleUnit = AngleUnit.DEGREES;
        public @Nullable Size cameraResolution = new Size(1280, 720);
        public boolean useManualExposure = false;
        public int manualExposureMs = 20;
        public int manualGain = 15;
        public @Nullable AprilTagVisionProcessor.LensIntrinsics lensIntrinsics = null;
    }

    private final Telemetry telemetry;
    private final Config config;
    private final List<AprilTagVisionProcessor> processors = new ArrayList<>();

    private VisionPortal visionPortal;

    public AprilTagVisionManager(@NonNull HardwareMap hardwareMap,
                                 @NonNull Telemetry telemetry,
                                 @NonNull Config config) {
        this.telemetry = telemetry;
        this.config = config;
        initialise(hardwareMap);
    }

    private void initialise(HardwareMap hardwareMap) {
        VisionPortal.Builder portalBuilder = new VisionPortal.Builder();
        if (config.useWebcam) {
            WebcamName webcamName = hardwareMap.get(WebcamName.class, config.cameraName);
            portalBuilder.setCamera(webcamName);
        } else {
            portalBuilder.setCamera(VisionPortal.CameraType.BUILTIN_PHONE);
        }
        if (config.cameraResolution != null) {
            portalBuilder.setCameraResolution(config.cameraResolution);
        }
        portalBuilder.setAutoStopLiveView(!config.enableLiveView);

        for (TagFamily tagFamily : config.tagFamilies) {
            AprilTagVisionProcessor.Config processorConfig = new AprilTagVisionProcessor.Config();
            processorConfig.tagFamily = tagFamily;
            processorConfig.drawAxes = config.drawAxes;
            processorConfig.drawCube = config.drawCube;
            processorConfig.drawTagId = config.drawTagId;
            processorConfig.drawTagOutline = config.drawTagOutline;
            processorConfig.tagSizeMeters = config.tagSizeMeters;
            processorConfig.distanceUnit = config.distanceUnit;
            processorConfig.angleUnit = config.angleUnit;
            processorConfig.lensIntrinsics = config.lensIntrinsics;

            AprilTagVisionProcessor wrappedProcessor = AprilTagVisionProcessor.fromConfig(processorConfig);
            processors.add(wrappedProcessor);
            portalBuilder.addProcessor(wrappedProcessor);
        }

        visionPortal = portalBuilder.build();
        if (config.useManualExposure) {
            configureManualExposure(config.manualExposureMs, config.manualGain);
        }
    }

    public void startStreaming() {
        if (visionPortal != null) {
            visionPortal.resumeStreaming();
        }
    }

    public void stopStreaming() {
        if (visionPortal != null) {
            visionPortal.stopStreaming();
        }
    }

    public void shutdown() {
        if (visionPortal != null) {
            visionPortal.close();
            visionPortal = null;
        }
        processors.clear();
    }

    public List<AprilTagDetectionData> getLatestDetections() {
        if (processors.isEmpty()) {
            return Collections.emptyList();
        }
        List<AprilTagDetectionData> combined = new ArrayList<>();
        for (AprilTagVisionProcessor processor : processors) {
            combined.addAll(processor.getLatestDetections());
        }
        return Collections.unmodifiableList(combined);
    }

    public void pushTelemetry() {
        List<AprilTagDetectionData> detections = getLatestDetections();
        telemetry.addData("Detections", detections.size());
        int idx = 0;
        for (AprilTagDetectionData detection : detections) {
            telemetry.addLine(String.format(Locale.US,
                    "#%d | id=%d fam=%s range=%.2f %s yaw=%.1f %s pitch=%.1f %s roll=%.1f %s",
                    ++idx,
                    detection.getId(),
                    detection.getTagFamily(),
                    detection.getRange(), config.distanceUnit,
                    detection.getYaw(), config.angleUnit,
                    detection.getPitch(), config.angleUnit,
                    detection.getRoll(), config.angleUnit));
        }
        telemetry.update();
    }

    public boolean isCameraStreaming() {
        return visionPortal != null && visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING;
    }

    public void setDecimation(float decimation) {
        for (AprilTagVisionProcessor processor : processors) {
            processor.setDecimation(decimation);
        }
    }

    public void configureManualExposure(int exposureMs, int gain) {
        if (visionPortal == null) {
            return;
        }
        ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
        GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
        if (exposureControl == null || gainControl == null) {
            return;
        }

        boolean changedMode = exposureControl.getMode() != ExposureControl.Mode.Manual;
        if (changedMode) {
            exposureControl.setMode(ExposureControl.Mode.Manual);
            // Give the hardware a brief moment to settle in manual mode before configuring values.
            ElapsedTime timer = new ElapsedTime();
            while (timer.milliseconds() < 50 && !Thread.currentThread().isInterrupted()) {
                Thread.yield();
            }
        }
        exposureControl.setExposure(exposureMs, TimeUnit.MILLISECONDS);
        gainControl.setGain(gain);
    }
}
