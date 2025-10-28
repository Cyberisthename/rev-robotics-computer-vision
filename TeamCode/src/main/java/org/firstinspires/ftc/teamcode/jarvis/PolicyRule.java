package org.firstinspires.ftc.teamcode.jarvis;

/** Base class for simple hand-authored policies with helper utilities. */
public abstract class PolicyRule implements Policy {
    protected static float clamp(float value, float lo, float hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
