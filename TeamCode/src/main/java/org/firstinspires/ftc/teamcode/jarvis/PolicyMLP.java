package org.firstinspires.ftc.teamcode.jarvis;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Lightweight multi-layer perceptron that runs entirely on the REV Control Hub.
 * Weights are loaded from a JSON file packaged in the assets folder.
 */
public class PolicyMLP implements Policy {
    private final String skillName;
    private final int inputDim;
    private final String[] activations;
    private final float[][][] weights;
    private final float[][] biases;

    public PolicyMLP(AssetManager assets, String assetPath, String skillName) {
        try {
            String json = readAsset(assets, assetPath);
            JSONObject root = new JSONObject(json);
            this.skillName = skillName;
            this.inputDim = root.getInt("in");

            JSONArray layers = root.getJSONArray("layers");
            int layerCount = layers.length();
            this.activations = new String[layerCount];
            this.weights = new float[layerCount][][];
            this.biases = new float[layerCount][];

            for (int l = 0; l < layerCount; l++) {
                JSONObject layer = layers.getJSONObject(l);
                activations[l] = layer.optString("activation", "linear");
                int units = layer.getInt("units");

                int inDim = (l == 0) ? inputDim : biases[l - 1].length;
                float[][] w = new float[inDim][units];
                float[] b = new float[units];

                JSONArray wFlat = layer.getJSONArray("W");
                if (wFlat.length() != inDim * units) {
                    throw new IllegalArgumentException("Layer " + l + " weight count mismatch");
                }
                for (int i = 0; i < inDim; i++) {
                    for (int j = 0; j < units; j++) {
                        w[i][j] = (float) wFlat.getDouble(i * units + j);
                    }
                }

                JSONArray bArr = layer.getJSONArray("b");
                if (bArr.length() != units) {
                    throw new IllegalArgumentException("Layer " + l + " bias count mismatch");
                }
                for (int j = 0; j < units; j++) {
                    b[j] = (float) bArr.getDouble(j);
                }

                weights[l] = w;
                biases[l] = b;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load MLP policy: " + e.getMessage(), e);
        }
    }

    private static String readAsset(AssetManager assets, String path) throws IOException {
        try (InputStream is = assets.open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    @Override
    public DriveCmd decide(float[] state) {
        if (state.length < inputDim) {
            throw new IllegalArgumentException(
                    "Expected state vector with at least " + inputDim + " values but received " + state.length);
        }
        float[] activation = new float[inputDim];
        System.arraycopy(state, 0, activation, 0, inputDim);
        for (int l = 0; l < weights.length; l++) {
            float[] next = new float[biases[l].length];
            for (int j = 0; j < next.length; j++) {
                float sum = biases[l][j];
                for (int i = 0; i < weights[l].length; i++) {
                    sum += activation[i] * weights[l][i][j];
                }
                next[j] = applyActivation(sum, activations[l]);
            }
            activation = next;
        }

        float fwd = clamp(activation[0]);
        float strafe = clamp(activation[1]);
        float turn = clamp(activation[2]);
        return new DriveCmd(fwd, strafe, turn);
    }

    private static float applyActivation(float value, String activation) {
        switch (activation) {
            case "relu":
                return Math.max(0f, value);
            case "tanh":
                return (float) Math.tanh(value);
            case "linear":
            default:
                return value;
        }
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    @Override
    public String name() {
        return skillName;
    }
}
