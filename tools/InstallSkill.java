import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class InstallSkill {
    private static final class Args {
        String projectRoot = ".";
        String skillName;
        String type = "mlp";
        int inDim = 4;
        int[] layers = new int[]{32, 3};
        String[] activations = new String[]{"relu", "tanh"};
        String[] weightCsv;
        String[] biasCsv;
        String ruleClass;
        boolean randomWeights = false;
        String outJson;
    }

    public static void main(String[] argv) throws Exception {
        Args args = parse(argv);
        if (args.skillName == null || args.skillName.isEmpty()) {
            fail("--name <skillName> is required");
        }
        Path assetsDir = Paths.get(args.projectRoot, "TeamCode", "src", "main", "assets", "policies");
        Files.createDirectories(assetsDir);
        Path manifest = assetsDir.resolve("manifest.json");
        if (!Files.exists(manifest)) {
            Files.writeString(manifest, "{\n  \"skills\": []\n}\n", StandardCharsets.UTF_8);
        }

        switch (args.type) {
            case "rule":
                if (args.ruleClass == null) {
                    fail("--ruleClass is required for type=rule");
                }
                addRule(manifest, args.skillName, args.ruleClass);
                System.out.println("Added RULE skill '" + args.skillName + "' → manifest.json");
                break;
            case "mlp":
                String json = buildMlpJson(args);
                String fileName = args.outJson != null ? args.outJson : args.skillName + ".json";
                Path out = assetsDir.resolve(fileName);
                Files.write(out, json.getBytes(StandardCharsets.UTF_8));
                addMlp(manifest, args.skillName, fileName);
                System.out.println("Wrote " + out.toAbsolutePath());
                System.out.println("Added MLP skill '" + args.skillName + "' → manifest.json");
                break;
            default:
                fail("Unknown --type: " + args.type);
        }

        System.out.println("✅ Done. Build & deploy your app.");
    }

    private static Args parse(String[] argv) {
        Args args = new Args();
        for (int i = 0; i < argv.length; i++) {
            switch (argv[i]) {
                case "--root":
                    args.projectRoot = argv[++i];
                    break;
                case "--name":
                    args.skillName = argv[++i];
                    break;
                case "--type":
                    args.type = argv[++i];
                    break;
                case "--in":
                    args.inDim = Integer.parseInt(argv[++i]);
                    break;
                case "--layers":
                    args.layers = Arrays.stream(argv[++i].split(",")).mapToInt(Integer::parseInt).toArray();
                    break;
                case "--acts":
                    args.activations = argv[++i].split(",");
                    break;
                case "--Wcsv":
                    args.weightCsv = argv[++i].split(",");
                    break;
                case "--Bcsv":
                    args.biasCsv = argv[++i].split(",");
                    break;
                case "--ruleClass":
                    args.ruleClass = argv[++i];
                    break;
                case "--random":
                    args.randomWeights = true;
                    break;
                case "--out":
                    args.outJson = argv[++i];
                    break;
                default:
                    fail("Unknown arg: " + argv[i]);
            }
        }
        if (args.activations.length != args.layers.length) {
            fail("--acts must match --layers count");
        }
        if ("mlp".equals(args.type)) {
            if (args.weightCsv != null && args.weightCsv.length != args.layers.length) {
                fail("--Wcsv count must match layers count");
            }
            if (args.biasCsv != null && args.biasCsv.length != args.layers.length) {
                fail("--Bcsv count must match layers count");
            }
        }
        return args;
    }

    private static void addRule(Path manifest, String name, String cls) throws IOException {
        Map<String, Object> root = readManifest(manifest);
        List<Map<String, Object>> skills = getSkills(root);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("type", "rule");
        entry.put("class", cls);
        skills.add(entry);
        writeManifest(manifest, root);
    }

    private static void addMlp(Path manifest, String name, String file) throws IOException {
        Map<String, Object> root = readManifest(manifest);
        List<Map<String, Object>> skills = getSkills(root);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("type", "mlp");
        entry.put("file", file);
        skills.add(entry);
        writeManifest(manifest, root);
    }

    private static Map<String, Object> readManifest(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8).trim();
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> skills = new ArrayList<>();
        root.put("skills", skills);
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return root;
        }
        String body = text.substring(start + 1, end).trim();
        if (body.isEmpty()) {
            return root;
        }
        String[] objects = body.split("\\},\\s*\\{");
        for (String obj : objects) {
            String trimmed = obj.trim();
            if (!trimmed.startsWith("{")) {
                trimmed = "{" + trimmed;
            }
            if (!trimmed.endsWith("}")) {
                trimmed = trimmed + "}";
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String kv : inner.split(",")) {
                    String[] parts = kv.split(":", 2);
                    if (parts.length < 2) {
                        continue;
                    }
                    String key = strip(parts[0]);
                    String value = strip(parts[1]);
                    entry.put(key, value);
                }
            }
            skills.add(entry);
        }
        return root;
    }

    private static List<Map<String, Object>> getSkills(Map<String, Object> root) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) root.get("skills");
        if (skills == null) {
            skills = new ArrayList<>();
            root.put("skills", skills);
        }
        return skills;
    }

    private static void writeManifest(Path path, Map<String, Object> root) throws IOException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skills = (List<Map<String, Object>>) root.get("skills");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"skills\": [\n");
        for (int i = 0; i < skills.size(); i++) {
            Map<String, Object> entry = skills.get(i);
            sb.append("    {");
            int c = 0;
            for (Map.Entry<String, Object> e : entry.entrySet()) {
                if (c++ > 0) {
                    sb.append(", ");
                }
                sb.append('"').append(e.getKey()).append("\": \"").append(e.getValue()).append('\"');
            }
            sb.append("}");
            if (i + 1 < skills.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n}\n");
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String strip(String text) {
        String s = text.trim();
        if (s.startsWith("\"")) {
            s = s.substring(1);
        }
        if (s.endsWith("\"")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String buildMlpJson(Args args) throws IOException {
        List<float[][]> weights = new ArrayList<>();
        List<float[]> biases = new ArrayList<>();
        int input = args.inDim;
        Random rnd = new Random(7);
        for (int layer = 0; layer < args.layers.length; layer++) {
            int out = args.layers[layer];
            float[][] W;
            if (args.weightCsv != null && args.weightCsv[layer] != null && !args.weightCsv[layer].isEmpty()) {
                W = readMatrixCsv(Paths.get(args.weightCsv[layer]), input, out);
            } else {
                W = new float[input][out];
                if (args.randomWeights) {
                    fillRandom(W, rnd, 0.02f);
                }
            }
            float[] b;
            if (args.biasCsv != null && args.biasCsv[layer] != null && !args.biasCsv[layer].isEmpty()) {
                b = readVectorCsv(Paths.get(args.biasCsv[layer]), out);
            } else {
                b = new float[out];
            }
            weights.add(W);
            biases.add(b);
            input = out;
        }
        return toJson(args.inDim, args.layers, args.activations, weights, biases);
    }

    private static void fillRandom(float[][] matrix, Random rnd, float scale) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                matrix[i][j] = (float) (rnd.nextGaussian() * scale);
            }
        }
    }

    private static float[][] readMatrixCsv(Path path, int rows, int cols) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        float[][] matrix = new float[rows][cols];
        int r = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] tokens = line.split(",");
            for (int c = 0; c < cols && c < tokens.length; c++) {
                matrix[r][c] = Float.parseFloat(tokens[c].trim());
            }
            r++;
            if (r >= rows) {
                break;
            }
        }
        return matrix;
    }

    private static float[] readVectorCsv(Path path, int len) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8).trim();
        if (content.isEmpty()) {
            return new float[len];
        }
        String[] tokens = content.split(",");
        float[] vector = new float[len];
        for (int i = 0; i < len && i < tokens.length; i++) {
            vector[i] = Float.parseFloat(tokens[i].trim());
        }
        return vector;
    }

    private static String toJson(int inDim, int[] layers, String[] acts, List<float[][]> W, List<float[]> B) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"type\": \"mlp\",\n  \"in\": ").append(inDim).append(",\n  \"layers\": [\n");
        int input = inDim;
        for (int l = 0; l < layers.length; l++) {
            int out = layers[l];
            sb.append("    {\"units\": ").append(out)
              .append(", \"activation\": \"").append(acts[l]).append("\", \"W\": [");
            float[][] weights = W.get(l);
            for (int i = 0; i < input; i++) {
                for (int j = 0; j < out; j++) {
                    sb.append(trim(weights[i][j]));
                    if (!(i == input - 1 && j == out - 1)) {
                        sb.append(", ");
                    }
                }
            }
            sb.append("], \"b\": [");
            float[] bias = B.get(l);
            for (int j = 0; j < out; j++) {
                sb.append(trim(bias[j]));
                if (j + 1 < out) {
                    sb.append(", ");
                }
            }
            sb.append("]}");
            if (l + 1 < layers.length) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
            input = out;
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static String trim(float value) {
        String s = String.format(Locale.US, "%.6f", value);
        int i = s.length() - 1;
        while (i > 0 && s.charAt(i) == '0') {
            i--;
        }
        if (s.charAt(i) == '.') {
            i--;
        }
        return s.substring(0, i + 1);
    }

    private static void fail(String message) {
        System.err.println("Error: " + message);
        System.err.println(usage());
        System.exit(1);
    }

    private static String usage() {
        return "\nInstallSkill usage:\n" +
                "  javac tools/InstallSkill.java && java InstallSkill [options]\n\n" +
                "Required:\n  --name <skillName>\n\n" +
                "Common options:\n" +
                "  --root <pathToProjectRoot>\n" +
                "  --type <mlp|rule>\n" +
                "  --in <inDim>\n" +
                "  --layers <commaList>\n" +
                "  --acts <commaList>\n" +
                "  --Wcsv <filePerLayerCommaList>\n" +
                "  --Bcsv <filePerLayerCommaList>\n" +
                "  --random\n" +
                "  --out <fileName.json>\n" +
                "  --ruleClass <FQCN>\n";
    }
}
