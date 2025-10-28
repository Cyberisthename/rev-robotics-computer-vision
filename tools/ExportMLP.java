import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ExportMLP {
    static final class Layer {
        int units;
        String activation;
        float[] W;
        float[] b;
    }

    static String trim(float v) {
        String s = String.format(Locale.US, "%.6f", v);
        int i = s.length() - 1;
        while (i > 0 && s.charAt(i) == '0') {
            i--;
        }
        if (s.charAt(i) == '.') {
            i--;
        }
        return s.substring(0, i + 1);
    }

    static float[] flattenRowMajor(float[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[] out = new float[rows * cols];
        int k = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                out[k++] = matrix[i][j];
            }
        }
        return out;
    }

    static String toJson(int inDim, List<Layer> layers) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"mlp\",\n");
        sb.append("  \"in\": ").append(inDim).append(",\n");
        sb.append("  \"layers\": [\n");
        for (int idx = 0; idx < layers.size(); idx++) {
            Layer layer = layers.get(idx);
            sb.append("    {\"units\": ").append(layer.units)
              .append(", \"activation\": \"").append(layer.activation).append("\", ");
            sb.append("\"W\": [");
            for (int i = 0; i < layer.W.length; i++) {
                sb.append(trim(layer.W[i]));
                if (i + 1 < layer.W.length) {
                    sb.append(", ");
                }
            }
            sb.append("], \"b\": [");
            for (int i = 0; i < layer.b.length; i++) {
                sb.append(trim(layer.b[i]));
                if (i + 1 < layer.b.length) {
                    sb.append(", ");
                }
            }
            sb.append("]}");
            if (idx + 1 < layers.size()) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        int inDim = 4;
        int hidden = 32;
        float[][] W0 = new float[inDim][hidden];
        float[] b0 = new float[hidden];
        float[][] W1 = new float[hidden][3];
        float[] b1 = new float[3];

        Random rnd = new Random(7);
        for (int i = 0; i < inDim; i++) {
            for (int j = 0; j < hidden; j++) {
                W0[i][j] = (float) (rnd.nextGaussian() * 0.02);
            }
        }
        for (int i = 0; i < hidden; i++) {
            for (int j = 0; j < 3; j++) {
                W1[i][j] = (float) (rnd.nextGaussian() * 0.02);
            }
        }

        List<Layer> layers = new ArrayList<>();
        Layer l0 = new Layer();
        l0.units = hidden;
        l0.activation = "relu";
        l0.W = flattenRowMajor(W0);
        l0.b = b0;
        layers.add(l0);

        Layer l1 = new Layer();
        l1.units = 3;
        l1.activation = "tanh";
        l1.W = flattenRowMajor(W1);
        l1.b = b1;
        layers.add(l1);

        String json = toJson(inDim, layers);
        Path out = Paths.get(args.length > 0 ? args[0] : "dock_mlp.json");
        Files.write(out, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + out.toAbsolutePath());
    }
}
