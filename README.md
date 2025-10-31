# REV Robotics AprilTag Vision (Java)

This project provides a ready-to-deploy AprilTag detection pipeline for REV Robotics Control Hub/Expansion Hub robots using the FTC SDK vision stack. It demonstrates how to configure the on-board camera (or a USB webcam), stream frames through the FTC `VisionPortal`, and surface rich telemetry data (ID, pose, orientation) for every detected tag in real time.

## Features

- Java-based AprilTag detection powered by the FTC SDK `AprilTagProcessor`
- Support for multiple tag families (e.g. `TAG_16h5`, `TAG_36h11`) in a single pipeline
- Camera configuration helpers for the REV Control Hub phone camera or external webcams
- Optional manual exposure/gain control for low-light robustness
- Real-time pose estimation converted into selectable distance/angle units
- Structured telemetry output for debugging or subsystem consumption
- Sample `LinearOpMode` showing end-to-end usage on a robot controller

## Repository structure

```
.
├── build.gradle                # Root gradle configuration (delegates to module)
├── settings.gradle             # Declares the TeamCode module
├── TeamCode/
│   ├── build.gradle            # Android library module containing the vision sources
│   ├── src/main/AndroidManifest.xml
│   └── src/main/java/org/firstinspires/ftc/teamcode/
│       ├── opmodes/            # Sample op mode for interactive testing
│       └── vision/             # Reusable AprilTag vision components
└── README.md
```

## Building and deploying

This module is structured like a standard FTC SDK `TeamCode` module. You can either import it directly into an existing robot controller project or clone the repository as a fresh starting point.

### Option 1: Import into an existing FTC project

1. Open your FTC Android Studio project (`FtcRobotController`) and copy the contents of the `TeamCode` directory into your project's `TeamCode` module (overwrite existing files or merge as needed).
2. In your root `settings.gradle`, make sure the `TeamCode` module is included (this repository already does so).
3. Sync Gradle. The module expects FTC SDK 9.2.0+ artifacts, which are available from the official GitHub Maven registry. Supply your GitHub credentials either via the `gradle.properties` entries `gpr.user` / `gpr.token` or the environment variables `GITHUB_ACTOR` / `GITHUB_TOKEN`.
4. Deploy as usual to the REV Control Hub or Driver Station phone using Android Studio's Run button.

### Option 2: Use this repository standalone

1. Clone the repository and open it with Android Studio (Giraffe or newer).
2. When prompted, trust the Gradle files. The project configures an Android library (`TeamCode`) ready to be packaged with the FTC Robot Controller application.
3. Provide credentials for the FTC Maven registry as described above so Gradle can resolve `RobotCore`, `Vision`, and other SDK dependencies.
4. Create a minimal robot controller shell app (for example, checking out the official `FtcRobotController` repo) and include this module, or copy the sources into your active controller project.

### Gradle wrapper

FTC deployments normally rely on the SDK's Gradle wrapper. If you create a combined project, be sure to keep the wrapper files from the SDK or regenerate them with `./gradlew wrapper --gradle-version 8.2.1` (the plugin version in this repository is compatible with Gradle 8.2.1).

## Usage in an OpMode

A ready-to-run sample is provided in `AprilTagVisionOpMode`:

```java
@TeleOp(name = "AprilTag Vision Test", group = "Vision")
public class AprilTagVisionOpMode extends LinearOpMode {
    private AprilTagVisionManager visionManager;

    @Override
    public void runOpMode() throws InterruptedException {
        AprilTagVisionManager.Config config = new AprilTagVisionManager.Config();
        config.useWebcam = true;
        config.cameraName = "Webcam 1";
        config.tagFamilies = EnumSet.of(
                AprilTagProcessor.TagFamily.TAG_16h5,
                AprilTagProcessor.TagFamily.TAG_36h11
        );
        config.useManualExposure = true;
        config.manualExposureMs = 12;
        config.manualGain = 16;

        visionManager = new AprilTagVisionManager(hardwareMap, telemetry, config);

        waitForStart();

        while (opModeIsActive()) {
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
```

Deploy the op mode, select it on the Driver Station, and press **INIT**. The pipeline starts immediately and streams tag pose data back to telemetry once **START** is pressed.

## Configuration reference

`AprilTagVisionManager.Config` centralises all major pipeline parameters:

| Field | Default | Description |
| --- | --- | --- |
| `useWebcam` | `true` | Selects between USB webcam (`true`) and Control Hub built-in camera (`false`). |
| `cameraName` | `"Webcam 1"` | Hardware map name for the webcam when `useWebcam` is enabled. |
| `enableLiveView` | `false` | Keep the Control Hub live-view stream active (useful when attached to Driver Station monitors). |
| `tagFamilies` | `TAG_36h11` | Set of AprilTag families to detect simultaneously. |
| `tagSizeMeters` | `0.165` | Physical edge length of the tag (used for accurate pose estimation). |
| `distanceUnit` | `DistanceUnit.METER` | Units for range/translation values exposed to the robot. |
| `angleUnit` | `AngleUnit.DEGREES` | Units for yaw/pitch/roll. |
| `cameraResolution` | `1280x720` | Target resolution for streaming; adjust to match your camera's capabilities. |
| `useManualExposure` | `false` | Enable manual exposure/gain control. Recommended for stable detection. |
| `manualExposureMs` | `20` | Exposure time in milliseconds when manual mode is enabled. |
| `manualGain` | `15` | Sensor gain value in manual mode. |
| `lensIntrinsics` | `null` | Optional camera intrinsics (`fx`, `fy`, `cx`, `cy`) if you have calibration data. |

All processors use the FTC SDK's `AprilTagProcessor`, which implements the `VisionProcessor` interface internally. The `AprilTagVisionProcessor` wrapper keeps a cached, telemetry-friendly view of detections without blocking the camera pipeline.

## Telemetry output

`AprilTagVisionManager#pushTelemetry()` emits a summary per detected tag:

```
Detections: 2
#1 | id=3 fam=TAG_36h11 range=0.87 m yaw=5.4 deg pitch=-1.2 deg roll=0.3 deg
#2 | id=7 fam=TAG_16h5 range=1.42 m yaw=-8.1 deg pitch=0.8 deg roll=-0.4 deg
```

Use this data to align your robot, feed pose estimates into localization filters, or trigger autonomous behaviours.

## Extending the system

- **Multiple pipelines:** add or remove `TagFamily` entries in the config to tune which tag sets are active.
- **Pose consumers:** fetch the structured results via `AprilTagVisionManager#getLatestDetections()` and share them with subsystems or state machines.
- **Exposure presets:** switch between manual and auto exposure depending on match lighting conditions.
- **Telemetry sinks:** replace `pushTelemetry()` with your own logging or dashboard integration if desired.

## Troubleshooting

- Ensure USB webcams are declared in the `Robot Configuration` as `"Webcam 1"` (or update the config value).
- If Gradle cannot resolve FTC dependencies, double-check your GitHub credentials and that you have accepted the FTC EULA/MIT license.
- Manual exposure control is not supported on every webcam. If `ExposureControl` or `GainControl` comes back `null`, disable manual mode.
- When deploying to a Control Hub, keep the target SDK and build tools aligned with the FTC SDK release you are using.

## License

This project is provided under the MIT license. See the FTC SDK for its own licensing terms and third-party attributions.
