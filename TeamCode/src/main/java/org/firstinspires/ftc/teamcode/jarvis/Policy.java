package org.firstinspires.ftc.teamcode.jarvis;

/**
 * Core policy interface. Implementations map the robot state vector to a DriveCmd.
 * The default state vector is [x, y, z, yaw] in FTC coordinate space.
 */
public interface Policy {
    DriveCmd decide(float[] state);

    default String name() {
        return getClass().getSimpleName();
    }
}
