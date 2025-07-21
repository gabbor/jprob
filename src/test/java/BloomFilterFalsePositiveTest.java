import hasher.MurmurHash3;
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
            "0.01, 1000, 5000",
            "0.05, 500, 3000",
            "0.001, 2000, 10000"
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
            "0.01, 1000, 5000",
            "0.05, 500, 3000",
            "0.001, 2000, 10000"
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



}
