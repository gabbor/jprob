
import hasher.MurmurHash3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import serializer.JavaSerializer;

import java.time.LocalDate;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class BloomFilterFalsePositiveTest {

    private <T> void runFalsePositiveTest(
            double errorRate,
            int numElements,
            int testSetSize,
            Function<Integer, T> itemFactory,
            String context
    ) {
        BloomFilter<T> bloomFilter = new BloomFilter<>(errorRate, numElements, new MurmurHash3(), new JavaSerializer<>());

        for (int i = 0; i < numElements; i++) {
            bloomFilter.add(itemFactory.apply(i));
        }

        int falsePositives = 0;
        for (int i = numElements; i < numElements + testSetSize; i++) {
            if (bloomFilter.contains(itemFactory.apply(i))) {
                falsePositives++;
            }
        }

        double actualRate = falsePositives / (double) testSetSize;
        System.out.printf("[%s] errorRate=%.4f, inserted=%d, testSet=%d, observed=%.4f%n",
                context, errorRate, numElements, testSetSize, actualRate);

        assertTrue(actualRate <= errorRate * 1.5,
                String.format("[%s] False positive rate too high: %.4f", context, actualRate));
    }

    @ParameterizedTest(name = "[String] errorRate={0}, elements={1}, testSet={2}")
    @CsvSource({
            "0.01, 500, 5000",
            "0.01, 1000, 10000",
            "0.01, 5000, 20000",
            "0.01, 10000, 30000",
            "0.01, 20000, 50000",
            "0.01, 50000, 100000",
            "0.05, 500, 5000",
            "0.05, 1000, 10000",
            "0.05, 5000, 20000",
            "0.05, 10000, 30000",
            "0.05, 20000, 50000",
            "0.05, 50000, 100000",
            "0.001, 500, 5000",
            "0.001, 1000, 10000",
            "0.001, 5000, 20000",
            "0.001, 10000, 30000",
            "0.001, 20000, 50000",
            "0.001, 50000, 100000"
    })
    public void testFalsePositiveRateWithStrings(double errorRate, int numElements, int testSetSize) {
        runFalsePositiveTest(
                errorRate,
                numElements,
                testSetSize,
                i -> "item-" + i,
                "String Elements"
        );
    }

    @ParameterizedTest(name = "[TestItem] errorRate={0}, elements={1}, testSet={2}")
    @CsvSource({
            "0.01, 1000, 10000",
            "0.01, 5000, 20000",
            "0.01, 10000, 30000",
            "0.01, 20000, 50000",
            "0.01, 50000, 100000",
            "0.05, 1000, 10000",
            "0.05, 5000, 20000",
            "0.05, 10000, 30000",
            "0.05, 20000, 50000",
            "0.05, 50000, 100000",
            "0.001, 1000, 10000",
            "0.001, 5000, 20000",
            "0.001, 10000, 30000",
            "0.001, 20000, 50000",
            "0.001, 50000, 100000"
    })
    public void testFalsePositiveRateWithTestItem(double errorRate, int numElements, int testSetSize) {
        runFalsePositiveTest(
                errorRate,
                numElements,
                testSetSize,
                i -> new TestItem("A" + i, "B" + i, i, LocalDate.of(2020, 1, 1).plusDays(i)),
                "TestItem Objects"
        );
    }

    @Test
    public void testNoFalseNegatives() {
        BloomFilter<String> bloomFilter = new BloomFilter<>(0.01, 1000);
        for (int i = 0; i < 1000; i++) {
            bloomFilter.add("item-" + i);
        }
        for (int i = 0; i < 1000; i++) {
            assertTrue(bloomFilter.contains("item-" + i), "False negative detected for item-" + i);
        }
    }

    @Test
    public void testOverCapacityBehavior() {
        int expectedElements = 1000;
        BloomFilter<String> bloomFilter = new BloomFilter<>(0.01, expectedElements);
        for (int i = 0; i < expectedElements * 2; i++) {
            bloomFilter.add("item-" + i);
        }
        int falseNegatives = 0;
        for (int i = 0; i < expectedElements * 2; i++) {
            if (!bloomFilter.contains("item-" + i)) {
                falseNegatives++;
            }
        }
        assertEquals(0, falseNegatives, "False negatives detected after overfilling the filter");
    }

}
