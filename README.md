# FTC Jarvis – All-Java AprilTag Router

This repository packages an FTC-ready, **Control Hub native** AprilTag workflow written entirely in Java. It combines the FTC SDK's `VisionPortal` + `AprilTagProcessor` with a hot-swappable router that can load either hand-authored rules or tiny neural-network skills without any Python, TFLite, or co-processor dependencies.

## Highlights

- **All on-hub Java** – deploy under `TeamCode/` and run straight from the REV Control Hub.
- **VisionPortal pose** – leverage the official FTC `AprilTagProcessor` for robust tag localization.
- **Runtime skill routing** – register multiple adapters (rules or MLP policies) and hot-swap them during a match.
- **Tiny neural nets in JSON** – export weights from your preferred trainer and ship them as small JSON files the hub can read.

## Project Layout

```
TeamCode/
└─ src/main/
   ├─ java/org/firstinspires/ftc/teamcode/jarvis/
   │  ├─ DriveCmd.java        # simple mecanum/tank command container
   │  ├─ Policy.java          # policy interface (state → DriveCmd)
   │  ├─ PolicyRule.java      # helper base for rule adapters
   │  ├─ ApproachRule.java    # example approach behaviour
   │  ├─ DockRule.java        # example docking behaviour
   │  ├─ PolicyMLP.java       # JSON-loaded MLP policy implementation
   │  ├─ Router.java          # loads manifest + chooses active policy
   │  └─ JarvisOnHub.java     # FTC LinearOpMode that wires everything together
   └─ assets/policies/
      ├─ manifest.json        # declares available skills
      └─ dock_mlp.json        # sample MLP weights (replace with your export)
```

## Getting Started

1. Drop the `TeamCode` folder into your FTC SDK project (replace or merge with your existing module).
2. Open Android Studio, sync Gradle, and deploy the `JarvisOnHubJava` TeleOp to your Control Hub.
3. Configure the robot configuration with drive motors named `lf`, `rf`, `lb`, and `rb`, plus a webcam named `Webcam 1`.
4. Run the TeleOp. Jarvis will detect AprilTags, choose a policy from the router, and drive based on the active adapter.

## Customising Skills

### Rule-Based Adapter

Create a new class that implements `Policy` (or extend `PolicyRule`) inside `jarvis/`. Add its fully-qualified class name to `assets/policies/manifest.json` with `"type": "rule"`.

### Neural Adapter (JSON MLP)

1. Train a compact MLP offline (e.g., 4→16→3) and export weights in the format demonstrated in `dock_mlp.json`.
2. Copy the JSON file into `assets/policies/`.
3. Register it in `manifest.json` with `"type": "mlp"` and the JSON filename.

At runtime the router loads every declared skill, instantiates the corresponding adapter, and exposes a `chooseSkill(tagId, distanceMeters)` hook so you can implement match-specific logic.

## Vision Tuning

- Update `setLensIntrinsics(fx, fy, cx, cy)` in `JarvisOnHub` with calibration values for your camera.
- Set the correct tag size in meters via `.setTagSize()`.
- Extend the telemetry and state vector if you need additional FTC pose components (roll, pitch, etc.).

## Safety Notes

- Outputs are clamped to ±1 before being mixed into mecanum wheel powers.
- A 300 ms watchdog zeros the drivetrain if tags disappear, preventing runaway commands.
- Remember to cap wheel power or add additional checks if you adapt this to other drivetrains.

Enjoy running Jarvis with infinite-capacity skills directly on your Control Hub—all without leaving Java.

## Java Weight Exporters

You can generate JSON weight files without leaving Java by using the supplied desktop tool or the Android-local unit test harness. Both paths emit JSON compatible with `PolicyMLP`.

### Desktop exporter (`tools/ExportMLP.java`)

1. Adjust the matrix/weight definitions inside `main` (or load them from files).
2. Compile and run from your workstation:

   ```bash
   javac tools/ExportMLP.java && java ExportMLP TeamCode/src/main/assets/policies/dock_mlp.json
   ```

3. Copy the resulting JSON into your FTC project's assets folder if you generated it elsewhere.

### Android unit-test exporter (`ExportDockWeightsTest`)

Run the `ExportDockWeightsTest` local unit test inside Android Studio to emit a JSON file under `TeamCode/build/`. Move that file into `TeamCode/src/main/assets/policies/` and register it in the manifest when you're happy with the numbers.

## Skill Installer Helper

The `tools/InstallSkill.java` script automates adding skills to the manifest and (for MLP skills) writes JSON files for you:

```bash
javac tools/InstallSkill.java && \
java InstallSkill --name dock_nn --type mlp --in 4 --layers 32,3 --acts relu,tanh --random
```

Key options:

- `--type rule --ruleClass <FQCN>` to add a rule skill without touching JSON.
- `--Wcsv` / `--Bcsv` to populate weights from CSVs (row-major per layer).
- `--out <fileName.json>` to override the generated JSON filename.

Run the command again with different arguments to append additional skills; the manifest is updated in-place with a minimal JSON writer tailored for this project layout.
