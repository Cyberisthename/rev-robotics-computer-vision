package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.firstinspires.ftc.teamcode.vision.AprilTagVisionManager;

import java.util.EnumSet;

/**
 * Demonstrates how to initialize {@link AprilTagVisionManager} from a LinearOpMode and stream telemetry
 * back to the driver station in real time.
 */
@TeleOp(name = "AprilTag Vision Test", group = "Vision")
public class AprilTagVisionOpMode extends LinearOpMode {

    private AprilTagVisionManager visionManager;

    @Override
    public void runOpMode() throws InterruptedException {
        AprilTagVisionManager.Config config = new AprilTagVisionManager.Config();
        config.useWebcam = true;
        config.cameraName = "Webcam 1";
        config.enableLiveView = true;
        config.tagFamilies = EnumSet.of(
                AprilTagProcessor.TagFamily.TAG_16h5,
                AprilTagProcessor.TagFamily.TAG_36h11
        );
        config.distanceUnit = DistanceUnit.METER;
        config.angleUnit = AngleUnit.DEGREES;
        config.useManualExposure = true;
        config.manualExposureMs = 12;
        config.manualGain = 16;

        visionManager = new AprilTagVisionManager(hardwareMap, telemetry, config);

        telemetry.addLine("AprilTag vision ready. Press start when ready.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive() && !isStopRequested()) {
            visionManager.pushTelemetry();
            sleep(20);
        }
    }

    @Override
    public void onStop() {
        if (visionManager != null) {
            visionManager.shutdown();
        }
    }
}
