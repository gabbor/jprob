import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HLLAccuracyTest {

    private static <T> void assertEstimateWithinError(
            int b,
            Set<T> uniqueItems,
            HyperLogLog<T> hll,
            String context
    ) {
        long estimate = hll.estimate();
        int trueCardinality = uniqueItems.size();
        double relativeError = Math.abs(estimate - trueCardinality) / (double) trueCardinality;
        double theoreticalError = 1.04 / Math.sqrt(1 << b);

        System.out.printf(
                "[%s] b=%d, unique=%d, estimate=%d, error=%.4f, bound=%.4f%n",
                context, b, trueCardinality, estimate, relativeError, theoreticalError
        );

        assertTrue(
                relativeError <= theoreticalError * 2,
                String.format("[%s] Relative error %.4f exceeds bound %.4f", context, relativeError, theoreticalError * 2)
        );
    }

    private static <T> void runInsertionTest(
            int b,
            int totalInsertions,
            Function<Integer, T> generator,
            Supplier<HyperLogLog<T>> hllSupplier,
            String context
    ) {
        HyperLogLog<T> hll = hllSupplier.get();
        Set<T> uniqueItems = new HashSet<>();

        for (int i = 0; i < totalInsertions; i++) {
            T item = generator.apply(i);
            hll.add(item);
            uniqueItems.add(item);
        }

        assertEstimateWithinError(b, uniqueItems, hll, context);
    }

    @ParameterizedTest
    @CsvSource({
            "4, 100",
            "6, 1000",
            "10, 10000",
            "14, 50000",
            "16, 100000"
    })
    public void testEstimateWithUniqueStrings(int b, int insertions) {
        runInsertionTest(b, insertions, i -> "element-" + i, () -> new HyperLogLog<>(b), "Unique Strings");
    }

    @ParameterizedTest
    @CsvSource({
            "6, 1000, 100",
            "10, 10000, 1000",
            "14, 50000, 5000"
    })
    public void testEstimateWithDuplicates(int b, int totalInsertions, int uniqueElements) {
        runInsertionTest(
                b,
                totalInsertions,
                i -> "dup-" + (i % uniqueElements),
                () -> new HyperLogLog<>(b),
                "Strings with Duplicates"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "10, 10000",
            "14, 50000"
    })
    public void testEstimateWithCustomObjects(int b, int insertions) {
        runInsertionTest(
                b,
                insertions,
                i -> new TestItem("fieldA" + i, "fieldB" + i,
                        170 + (i % 30), LocalDate.of(1990 + (i % 30), 1, 1)),
                () -> new HyperLogLog<>(b),
                "Custom TestItem Objects"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "10, 5000, 5000",
            "14, 10000, 15000"
    })
    public void testMergeAccuracy(int b, int cardinality1, int cardinality2) {
        HyperLogLog<String> hll1 = new HyperLogLog<>(b);
        HyperLogLog<String> hll2 = new HyperLogLog<>(b);
        Set<String> groundTruth = new HashSet<>();

        for (int i = 0; i < cardinality1; i++) {
            String item = "merge-" + i;
            hll1.add(item);
            groundTruth.add(item);
        }

        for (int i = cardinality1; i < cardinality1 + cardinality2; i++) {
            String item = "merge-" + i;
            hll2.add(item);
            groundTruth.add(item);
        }

        hll1.merge(hll2);
        assertEstimateWithinError(b, groundTruth, hll1, "Merge Test");
    }
}
