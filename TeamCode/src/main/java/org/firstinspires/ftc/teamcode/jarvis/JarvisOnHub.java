package org.firstinspires.ftc.teamcode.jarvis;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

/**
 * FTC LinearOpMode that wires the router to the AprilTag processor running directly on the Control Hub.
 */
@TeleOp(name = "JarvisOnHubJava", group = "Centennial")
public class JarvisOnHub extends LinearOpMode {
    private DcMotor leftFront;
    private DcMotor rightFront;
    private DcMotor leftBack;
    private DcMotor rightBack;

    private AprilTagProcessor tagProcessor;
    private VisionPortal visionPortal;
    private Router router;

    @Override
    public void runOpMode() throws InterruptedException {
        initDrive();
        initVision();
        router = new Router(hardwareMap.appContext.getAssets(), "policies/manifest.json");

        waitForStart();

        long lastSeenMs = System.currentTimeMillis();
        while (opModeIsActive()) {
            List<AprilTagDetection> detections = tagProcessor.getDetections();
            boolean seen = detections != null && !detections.isEmpty();

            float x = 0f;
            float y = 0f;
            float z = 0f;
            float yaw = 0f;
            int tagId = -1;

            if (seen) {
                AprilTagDetection detection = detections.get(0);
                x = (float) detection.ftcPose.x;
                y = (float) detection.ftcPose.y;
                z = (float) detection.ftcPose.z;
                yaw = (float) detection.ftcPose.yaw;
                tagId = detection.id;

                String next = router.chooseSkill(tagId, z);
                router.select(next);
                lastSeenMs = System.currentTimeMillis();
            }

            float[] state = new float[]{x, y, z, yaw};
            boolean recentVision = (System.currentTimeMillis() - lastSeenMs) < 300;
            DriveCmd cmd = (seen || recentVision) ? router.run(state) : new DriveCmd(0f, 0f, 0f);
            applyDrive(cmd);

            telemetry.addData("skill", router.current());
            telemetry.addData("seen", seen);
            telemetry.addData("pose", "x=%.3f y=%.3f z=%.3f yaw=%.1f id=%d", x, y, z, yaw, tagId);
            telemetry.update();
            idle();
        }

        if (visionPortal != null) {
            visionPortal.close();
        }
    }

    private void initDrive() {
        leftFront = hardwareMap.get(DcMotor.class, "lf");
        rightFront = hardwareMap.get(DcMotor.class, "rf");
        leftBack = hardwareMap.get(DcMotor.class, "lb");
        rightBack = hardwareMap.get(DcMotor.class, "rb");

        rightFront.setDirection(DcMotor.Direction.REVERSE);
        rightBack.setDirection(DcMotor.Direction.REVERSE);
    }

    private void initVision() {
        tagProcessor = new AprilTagProcessor.Builder()
                .setDrawAxes(false)
                .setDrawCube(false)
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setTagLibrary(AprilTagProcessor.TagLibrary.APRILTAG_36h11_DEFAULT)
                .setOutputUnits(AprilTagProcessor.OutputUnits.METERS)
                .setLensIntrinsics(700, 700, 320, 240)
                .setTagSize(0.048)
                .build();

        VisionPortal.Builder builder = new VisionPortal.Builder();
        WebcamName webcam = hardwareMap.tryGet(WebcamName.class, "Webcam 1");
        if (webcam != null) {
            builder.setCamera(webcam);
        }
        visionPortal = builder.addProcessor(tagProcessor).build();
    }

    private void applyDrive(DriveCmd cmd) {
        float lfPower = cmd.fwd + cmd.strafe + cmd.turn;
        float rfPower = cmd.fwd - cmd.strafe - cmd.turn;
        float lbPower = cmd.fwd - cmd.strafe + cmd.turn;
        float rbPower = cmd.fwd + cmd.strafe - cmd.turn;

        float max = Math.max(1f, Math.max(Math.abs(lfPower), Math.max(Math.abs(rfPower), Math.max(Math.abs(lbPower), Math.abs(rbPower)))));

        leftFront.setPower(lfPower / max);
        rightFront.setPower(rfPower / max);
        leftBack.setPower(lbPower / max);
        rightBack.setPower(rbPower / max);
    }
}
