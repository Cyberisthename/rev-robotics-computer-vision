package org.firstinspires.ftc.teamcode.jarvis;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Random;

public class ExportDockWeightsTest {
    private static String trim(float value) {
        String formatted = String.format(Locale.US, "%.6f", value);
        int idx = formatted.length() - 1;
        while (idx > 0 && formatted.charAt(idx) == '0') {
            idx--;
        }
        if (formatted.charAt(idx) == '.') {
            idx--;
        }
        return formatted.substring(0, idx + 1);
    }

    private static String flatRowMajor(float[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(trim(matrix[i][j]));
                if (!(i == matrix.length - 1 && j == matrix[0].length - 1)) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    private static String flat(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            sb.append(trim(vector[i]));
            if (i + 1 < vector.length) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static String jsonForSimple4x32x3(float[][] W0, float[] b0, float[][] W1, float[] b1) {
        return "{\n" +
                "  \"type\": \"mlp\",\n" +
                "  \"in\": 4,\n" +
                "  \"layers\": [\n" +
                "    {\"units\": 32, \"activation\": \"relu\", \"W\": [" + flatRowMajor(W0) + "], \"b\": [" + flat(b0) + "]},\n" +
                "    {\"units\": 3, \"activation\": \"tanh\", \"W\": [" + flatRowMajor(W1) + "], \"b\": [" + flat(b1) + "]}\n" +
                "  ]\n" +
                "}\n";
    }

    @Test
    public void exportDock() throws Exception {
        int in = 4;
        int hidden = 32;
        int out = 3;
        Random rnd = new Random(3);
        float[][] W0 = new float[in][hidden];
        float[] b0 = new float[hidden];
        float[][] W1 = new float[hidden][out];
        float[] b1 = new float[out];
        for (int i = 0; i < in; i++) {
            for (int j = 0; j < hidden; j++) {
                W0[i][j] = (float) (rnd.nextGaussian() * 0.02);
            }
        }
        for (int i = 0; i < hidden; i++) {
            for (int j = 0; j < out; j++) {
                W1[i][j] = (float) (rnd.nextGaussian() * 0.02);
            }
        }
        String json = jsonForSimple4x32x3(W0, b0, W1, b1);
        Path outPath = Paths.get("build/dock_mlp.json");
        Files.createDirectories(outPath.getParent());
        Files.write(outPath, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + outPath.toAbsolutePath());
    }
}
