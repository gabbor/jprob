import hasher.MurmurHash3;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScalableBloomFilterTest {

    @ParameterizedTest(name = "[TestItem FPR test] errorRate={0}, initialCapacity={1}, elements={2}, testSet={3}")
    @CsvSource({
            "0.01, 1000, 1000, 10000",
            "0.01, 1000, 5000, 20000",
            "0.01, 1000, 10000, 30000",
            "0.01, 1000, 20000, 50000",
            "0.01, 1000, 50000, 100000",
            "0.01, 1000, 100000, 100000",
            "0.05, 1000, 1000, 10000",
            "0.05, 1000, 5000, 20000",
            "0.05, 1000, 10000, 30000",
            "0.05, 1000, 20000, 50000",
            "0.05, 1000, 50000, 100000",
            "0.05, 1000, 100000, 100000",
            "0.001, 1000, 1000, 10000",
            "0.001, 1000, 5000, 20000",
            "0.001, 1000, 10000, 30000",
            "0.001, 1000, 20000, 50000",
            "0.001, 1000, 50000, 100000",
            "0.001, 1000, 100000, 100000",
    })
    public void testFPRWithScalingAndTestItem(double errorRate, int initialCapacity, int numElements, int testSetSize) {
        double growthRate = 2.0;
        double tighteningRatio = 0.5;
        ScalableBloomFilter<TestItem> sbf = new ScalableBloomFilter<>(
                errorRate, initialCapacity, growthRate, tighteningRatio, new MurmurHash3(), new CustomTestItemSerializer()
        );

        for (int i = 0; i < numElements; i++) {
            TestItem item = new TestItem("A" + i, "B" + i, i, LocalDate.of(2020, 1, 1).plusDays(i));
            sbf.add(item);
        }

        int falsePositives = 0;
        int i = 0;
        while (i < testSetSize) {
            TestItem candidate = new TestItem("X" + i, "Y" + i, i, LocalDate.of(2030, 1, 1).plusDays(i));

            if (sbf.contains(candidate)) {
                falsePositives++;
            }
            i++;
        }

        double actualRate = (double) falsePositives / testSetSize;
        System.out.printf("TestItem FP rate: %.5f (expected <= %.5f)%n", actualRate, errorRate * 1.5);

        assertTrue(actualRate <= errorRate * 1.5,
                String.format("False positive rate %.5f exceeds expected max %.5f", actualRate, errorRate));
    }

}


