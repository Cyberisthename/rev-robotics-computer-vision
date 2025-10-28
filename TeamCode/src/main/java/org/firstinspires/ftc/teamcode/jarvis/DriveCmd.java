package org.firstinspires.ftc.teamcode.jarvis;

/** Simple drive command container for mecanum/tank control outputs. */
public class DriveCmd {
    public float fwd;
    public float strafe;
    public float turn;

    public DriveCmd(float fwd, float strafe, float turn) {
        this.fwd = fwd;
        this.strafe = strafe;
        this.turn = turn;
    }
}
