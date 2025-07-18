import hasher.Hasher;
import hasher.MurmurHash3;

import java.util.Objects;

public class HyperLogLog<T> {
    private final int[] registers;
    private final int b;
    private final int m;
    private final double alphaMM;
    private final Hasher hasher;


    public HyperLogLog(int b, Hasher hasher) {
        if (b < 4 || b > 16) {
            throw new IllegalArgumentException("b must be between 4 and 16");
        }
        this.b = b;
        this.m = 1 << b;
        this.registers = new int[m];
        this.alphaMM = getAlphaMM(m);
        this.hasher = Objects.requireNonNullElseGet(hasher, () -> new MurmurHash3());
    }

    public HyperLogLog(int b) {
        this(b, null);
    }


    public void add(T item) {
        byte[] data = item.toString().getBytes();
        long hash = hasher.hash64(data);
        int index = (int) (hash >>> (64 - b));
        long remaining = hash << b;
        int rank = Long.numberOfLeadingZeros(remaining) + 1;
        registers[index] = Math.max(registers[index], rank);
    }

    public double estimate() {
        double sum = 0.0;
        for (int register : registers) {
            sum += 1.0 / (1 << register);
        }
        double estimate = alphaMM / sum;

        if (estimate <= 2.5 * m) {
            int zeros = 0;
            for (int register : registers) {
                if (register == 0) zeros++;
            }
            if (zeros != 0) {
                estimate = m * Math.log((double) m / zeros);
            }
        }

        return estimate;
    }

    private double getAlphaMM(int m) {
        return switch (m) {
            case 16 -> 0.673 * m * m;
            case 32 -> 0.697 * m * m;
            case 64 -> 0.709 * m * m;
            default -> (0.7213 / (1 + 1.079 / m)) * m * m;
        };
    }
}
