import com.dynatrace.hash4j.hashing.HashValue128;
import com.dynatrace.hash4j.hashing.Hashing;
import hasher.MurmurHash3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class MurmurHash3Test {

    private final MurmurHash3 myHasher = new MurmurHash3();

    @Test
    void testBasicInputs() {
        byte[][] testInputs = {
                "hello".getBytes(StandardCharsets.UTF_8),
                "MurmurHash".getBytes(StandardCharsets.UTF_8),
                "1234567890abcdef".getBytes(StandardCharsets.UTF_8),
                "".getBytes(StandardCharsets.UTF_8),
                "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8),
                "a".getBytes(StandardCharsets.UTF_8),
                "ab".getBytes(StandardCharsets.UTF_8),
                "abc".getBytes(StandardCharsets.UTF_8),
                "abcd".getBytes(StandardCharsets.UTF_8),
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit".getBytes(StandardCharsets.UTF_8)
        };

        int seed = 0;
        for (byte[] input : testInputs) {
            assertHashMatches(input, seed, "Basic input: " + new String(input, StandardCharsets.UTF_8));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 42, 12345, -1, -388, -32763, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void testDifferentSeeds(int seed) {
        byte[] testData = "Test data for different seeds".getBytes(StandardCharsets.UTF_8);
        assertHashMatches(testData, seed, "Seed: " + seed);
    }

    @Test
    void testBoundaryLengths() {
        // Test lengths around 16-byte boundaries (important for the algorithm)
        int[] testLengths = {0, 1, 7, 8, 9, 15, 16, 17, 23, 24, 25, 31, 32, 33, 47, 48, 49, 63, 64, 65};

        for (int length : testLengths) {
            byte[] data = createTestData(length);
            assertHashMatches(data, 0, "Length: " + length);
        }
    }

    @Test
    void testLargeInputs() {
        // Test various large input sizes
        int[] largeSizes = {100, 256, 512, 1024, 2048, 4096, 8192};

        for (int size : largeSizes) {
            byte[] data = createTestData(size);
            assertHashMatches(data, 42, "Large input size: " + size);
        }
    }

    @Test
    void testRandomData() {
        Random random = new Random(12345); // Fixed seed for reproducibility

        for (int i = 0; i < 1000; i++) {
            int length = random.nextInt(1000) + 1;
            byte[] data = new byte[length];
            random.nextBytes(data);

            assertHashMatches(data, random.nextInt(), "Random data iteration: " + i);
        }
    }

    @Test
    void testSpecialByteValues() {
        // Test with arrays containing special byte values
        byte[][] specialInputs = {
                {0},
                {-1},
                {127},
                {-128},
                {0, 0, 0, 0, 0, 0, 0, 0},
                {-1, -1, -1, -1, -1, -1, -1, -1},
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
                {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16}
        };

        for (byte[] input : specialInputs) {
            assertHashMatches(input, 0, "Special bytes: " + java.util.Arrays.toString(input));
        }
    }

    @Test
    void testUnicodeStrings() {
        String[] unicodeStrings = {
                "café",
                "naïve",
                "🌟✨💫",
                "こんにちは世界",
                "Здравствуй мир",
                "مرحبا بالعالم",
                "𝕳𝖊𝖑𝖑𝖔 𝖂𝖔𝖗𝖑𝖉",
                "Hello\u0000World", // String with null byte
                "Test\r\nNewline",
                "Tab\tSeparated\tValues"
        };

        for (String str : unicodeStrings) {
            byte[] data = str.getBytes(StandardCharsets.UTF_8);
            assertHashMatches(data, 1, "Unicode string: " + str);
        }
    }

    @Test
    void testConsistency() {
        // Ensure same input always produces same output
        byte[] testData = "Consistency test data".getBytes(StandardCharsets.UTF_8);
        int seed = 123;

        long firstResult = myHasher.hash64(testData, seed);

        // Run multiple times to ensure consistency
        for (int i = 0; i < 10; i++) {
            long result = myHasher.hash64(testData, seed);
            assertEquals(firstResult, result, "Hash should be consistent across multiple calls");
        }
    }


    @Test
    void testInputSensitivity() {
        // Small changes in input should produce very different hashes
        String base = "The quick brown fox jumps over the lazy dog";
        String modified1 = "The quick brown fox jumps over the lazy cat"; // dog -> cat
        String modified2 = "the quick brown fox jumps over the lazy dog"; // T -> t

        long hash1 = myHasher.hash64(base.getBytes(StandardCharsets.UTF_8), 0);
        long hash2 = myHasher.hash64(modified1.getBytes(StandardCharsets.UTF_8), 0);
        long hash3 = myHasher.hash64(modified2.getBytes(StandardCharsets.UTF_8), 0);

        // These should all be different (avalanche effect)
        assert hash1 != hash2 && hash2 != hash3 && hash1 != hash3;
    }

    @Test
    void testEdgeCaseLengths() {
        // Test specific lengths that might cause issues in tail handling
        for (int len = 0; len <= 20; len++) {
            byte[] data = createSequentialData(len);
            assertHashMatches(data, len, "Sequential data length: " + len);
        }
    }

    // Helper methods
    private void assertHashMatches(byte[] input, int seed, String message) {
        HashValue128 expected = Hashing.murmur3_128(seed).hashBytesTo128Bits(input);
        long actual = myHasher.hash64(input, seed);

        assertEquals(expected.getLeastSignificantBits(), actual,
                message + " - Hash mismatch");
    }

    private byte[] createTestData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    private byte[] createSequentialData(int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = (byte) (i + 1);
        }
        return data;
    }
}