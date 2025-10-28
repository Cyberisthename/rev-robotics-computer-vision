package org.firstinspires.ftc.teamcode.jarvis;

import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Router maintains a registry of available policies and selects which one should run.
 */
public class Router {
    private final Map<String, Policy> policies = new HashMap<>();
    private String current;

    public Router(AssetManager assets, String manifestPath) {
        try {
            String text = readAsset(assets, manifestPath);
            JSONObject root = new JSONObject(text);
            JSONArray skills = root.getJSONArray("skills");
            for (int i = 0; i < skills.length(); i++) {
                JSONObject skill = skills.getJSONObject(i);
                String name = skill.getString("name");
                String type = skill.getString("type");

                Policy policy;
                if ("rule".equals(type)) {
                    String clazz = skill.getString("class");
                    policy = (Policy) Class.forName(clazz).getDeclaredConstructor().newInstance();
                } else if ("mlp".equals(type)) {
                    String file = skill.getString("file");
                    policy = new PolicyMLP(assets, "policies/" + file, name);
                } else {
                    throw new IllegalArgumentException("Unknown skill type: " + type);
                }
                policies.put(name, policy);
                if (current == null) {
                    current = name;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load router manifest: " + e.getMessage(), e);
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

    public void select(String name) {
        if (policies.containsKey(name)) {
            current = name;
        }
    }

    public String current() {
        return current;
    }

    /** Example heuristic for selecting a skill based on AprilTag ID and range. */
    public String chooseSkill(int tagId, float distanceMeters) {
        if (distanceMeters < 0.30f && policies.containsKey("dock_nn")) {
            return "dock_nn";
        }
        if (distanceMeters < 0.35f && policies.containsKey("dock")) {
            return "dock";
        }
        if (policies.containsKey("approach")) {
            return "approach";
        }
        return current;
    }

    public DriveCmd run(float[] state) {
        Policy policy = policies.get(current);
        if (policy == null) {
            return new DriveCmd(0f, 0f, 0f);
        }
        return policy.decide(state);
    }
}
