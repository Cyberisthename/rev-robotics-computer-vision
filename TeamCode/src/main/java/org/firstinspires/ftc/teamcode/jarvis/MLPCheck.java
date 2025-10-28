package org.firstinspires.ftc.teamcode.jarvis;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "MLPCheck", group = "Cent")
public class MLPCheck extends LinearOpMode {
    @Override
    public void runOpMode() {
        Router router = new Router(hardwareMap.appContext.getAssets(), "policies/manifest.json");
        waitForStart();
        float[] state = new float[]{0f, 0f, 0.5f, 5f};
        while (opModeIsActive()) {
            DriveCmd cmd = router.run(state);
            telemetry.addData("skill", router.current());
            telemetry.addData("cmd", "f=%.3f s=%.3f t=%.3f", cmd.fwd, cmd.strafe, cmd.turn);
            telemetry.update();
            sleep(100);
        }
    }
}
