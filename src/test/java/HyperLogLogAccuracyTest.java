import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

public class HyperLogLogAccuracyTest {

    @ParameterizedTest
    @CsvSource({
            "4, 100",
            "5, 500",
            "6, 1000",
            "8, 5000",
            "10, 10000",
            "12, 20000",
            "14, 50000",
            "16, 100000"
    })
    public void testEstimateAccuracy(int b, int cardinality) {
        HyperLogLog<String> hll = new HyperLogLog<>(b);
        for (int i = 0; i < cardinality; i++) {
            hll.add("element-" + i);
        }

        double estimate = hll.estimate();
        double relativeError = Math.abs(estimate - cardinality) / cardinality;
        double theoreticalError = 1.04 / Math.sqrt(Math.pow(2, b));

        System.out.printf("b=%d, real=%d, estimate=%.2f, error=%.4f, bound=%.4f%n",
                b, cardinality, estimate, relativeError, theoreticalError);

        assertTrue(relativeError <= theoreticalError * 2,  // tolleranza raddoppiata per fluttuazioni
                "Relative error is significantly above the theoretical limit.");
    }
}

