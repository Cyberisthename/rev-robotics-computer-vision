package org.firstinspires.ftc.teamcode.jarvis;

/** Docking behaviour used when the robot is near the target tag. */
public class DockRule extends PolicyRule {
    @Override
    public DriveCmd decide(float[] state) {
        float x = state.length > 0 ? state[0] : 0f;
        float z = state.length > 2 ? state[2] : 0f;
        float yaw = state.length > 3 ? state[3] : 0f;

        float fwd = clamp((z - 0.20f) * 0.5f, -0.25f, 0.25f);
        float turn = clamp(-yaw / 60f, -0.3f, 0.3f);
        float strafe = clamp(-x * 0.6f, -0.2f, 0.2f);
        return new DriveCmd(fwd, strafe, turn);
    }
}
