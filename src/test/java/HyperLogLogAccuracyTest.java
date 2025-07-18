import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HyperLogLogAccuracyTest {


    private static <T extends Serializable> void testEstimateAccuracy(
            int b,
            int cardinality,
            Function<Integer, T> generator,
            Supplier<HyperLogLog<T>> hllSupplier
    ) {
        HyperLogLog<T> hll = hllSupplier.get();

        for (int i = 0; i < cardinality; i++) {
            hll.add(generator.apply(i));
        }

        long estimate = hll.estimate();
        double relativeError = (double) Math.abs(estimate - cardinality) / cardinality;
        double theoreticalError = 1.04 / Math.sqrt(Math.pow(2, b));

        System.out.printf("b=%d, real=%d, estimate=%d, error=%.4f, bound=%.4f%n",
                b, cardinality, estimate, relativeError, theoreticalError);

        assertTrue(relativeError <= theoreticalError * 2,
                "Relative error is significantly above the theoretical limit.");
    }


    @ParameterizedTest
    @CsvSource({
            "4, 100",
            "5, 500",
            "6, 1000",
            "8, 5000",
            "10, 10000",
            "12, 20000",
            "14, 50000",
            "16, 100000",
            "16, 1000000"
    })
    public void testEstimateAccuracyWithString(int b, int cardinality) {
        testEstimateAccuracy(
                b,
                cardinality,
                i -> "element-" + i,
                () -> new HyperLogLog<>(b)
        );
    }

    @ParameterizedTest
    @CsvSource({
            "4, 100",
            "5, 500",
            "6, 1000",
            "8, 5000",
            "10, 10000",
            "12, 20000",
            "14, 50000",
            "16, 100000",
            "16, 100000",
            "16, 1000000"
    })
    public void testEstimateAccuracyWithPerson(int b, int cardinality) {
        testEstimateAccuracy(
                b,
                cardinality,
                i -> new Person("Name" + i, "Surname" + i, 170 + (i % 30), LocalDate.of(1990 + (i % 30), 1, 1)),
                () -> new HyperLogLog<>(b)
        );
    }


}

