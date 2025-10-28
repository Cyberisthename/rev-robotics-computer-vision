package org.firstinspires.ftc.teamcode.jarvis;

/** Simple approach behaviour that centers on the AprilTag and maintains distance. */
public class ApproachRule extends PolicyRule {
    @Override
    public DriveCmd decide(float[] state) {
        float x = state.length > 0 ? state[0] : 0f;
        float y = state.length > 1 ? state[1] : 0f;
        float z = state.length > 2 ? state[2] : 0f;
        float yaw = state.length > 3 ? state[3] : 0f;

        float fwd = clamp(z - 0.40f, -0.4f, 0.4f);
        float turn = clamp(-yaw / 45f, -0.5f, 0.5f);
        float strafe = clamp(-x, -0.3f, 0.3f);
        return new DriveCmd(fwd, strafe, turn);
    }
}
