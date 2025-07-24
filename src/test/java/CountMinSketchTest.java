import hasher.MurmurHash3;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import serializer.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CountMinSketchTest {

    @ParameterizedTest
    @CsvSource({
            // Realistic high-scale scenarios: web logs, internet traffic, search engines, e-commerce.
            // Dominant items vs sparse rare ones — stresses Count-Min Sketch with heavy-tailed distributions.
            "0.01, 0.01, 100, 500000, 100000, 2000, 5, 1",
            "0.005, 0.01, 5000, 5000000, 500000, 1000, 3, 1",
            "0.01, 0.05, 200, 100000, 20000, 1500, 10, 2",
            "0.01, 0.01, 50000, 10000, 10000, 500, 8000, 1000",
            "0.005, 0.01, 100000, 100000, 50000, 2000, 3000, 500",
            "0.01, 0.01, 50, 1000000, 500000, 1500, 5, 1",
            "0.001, 0.005, 500000, 10000, 100000, 1000, 1000, 300",
            "0.01, 0.01, 1000000, 1000000, 100000, 3000, 3000, 300",

            // Flat frequencies across items
            "0.01, 0.01, 10000, 10000, 500, 0, 500, 0",
            "0.001, 0.01, 10000, 10000, 1000, 100, 1000, 100",

            // Edge case: very few items but high variance
            "0.01, 0.01, 5, 5, 10000, 5000, 10, 10",

            // Cold-start sparse scenario: very low item counts and low frequencies
            // Simulates early-stage systems or underutilized features.
            "0.01, 0.01, 50, 500, 10, 2, 5, 1",
    })
    public void testBoundWithGaussianFrequencies(
            double epsilon, double delta,
            int frequentItemCount, int rareItemCount,
            double freqMean, double freqStd,
            double rareMean, double rareStd) {

        CountMinSketch<String> cms = new CountMinSketch<>(
                epsilon, delta,
                new MurmurHash3(),
                new StringSerializer(StandardCharsets.UTF_8)
        );

        Map<String, Long> trueFrequencies = new HashMap<>();
        Random random = new Random(42); // Seed for reproducibility

        // Generate frequencies for frequent items using a Gaussian distribution
        for (int i = 0; i < frequentItemCount; i++) {
            long freq = Math.max(1, Math.round(random.nextGaussian() * freqStd + freqMean));
            String key = "frequent" + i;
            cms.add(key, freq);
            trueFrequencies.put(key, freq);
        }

        // Generate frequencies for rare items using a Gaussian distribution
        for (int i = 0; i < rareItemCount; i++) {
            long freq = Math.max(1, Math.round(random.nextGaussian() * rareStd + rareMean));
            String key = "rare" + i;
            cms.add(key, freq);
            trueFrequencies.put(key, freq);
        }

        long errorBound = cms.getErrorBound();
        int violations = 0;

        // Check accuracy of estimated frequencies against true values
        for (Map.Entry<String, Long> entry : trueFrequencies.entrySet()) {
            long trueValue = entry.getValue();
            long estimate = cms.estimateCount(entry.getKey());

            if (estimate > trueValue + errorBound) {
                violations++;
            }
        }

        double violationRate = violations / (double) trueFrequencies.size();
        System.out.printf(
                "[ε=%.4f, δ=%.4f, freqMean=%.1f, rareMean=%.1f] Violations: %d / %d (%.2f%%)%n",
                epsilon, delta, freqMean, rareMean,
                violations, trueFrequencies.size(),
                violationRate * 100
        );

        // Assert the theoretical guarantee of Count-Min Sketch
        assertTrue(violationRate <= (1 - delta),
                "Too many violations of the theoretical error bound");
    }


}

