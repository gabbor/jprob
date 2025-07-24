import hasher.Hasher;
import serializer.Serializer;

public class CountMinSketch<T> {
    private final long[] table;
    private final int depth;
    private final int width;
    private final Hasher hasher;
    private final Serializer<T> serializer;
    private final double epsilon;
    private final double delta;
    private long totalCount = 0;

    public CountMinSketch(double epsilon, double delta, Hasher hasher, Serializer<T> serializer) {
        if (epsilon <= 0 || delta <= 0 || epsilon >= 1 || delta >= 1) {
            throw new IllegalArgumentException("Epsilon and delta must be in (0, 1)");
        }

        this.epsilon = epsilon;
        this.delta = delta;
        this.depth = (int) Math.ceil(Math.log(1.0 / delta));
        this.width = (int) Math.ceil(Math.E / epsilon);
        this.table = new long[depth * width];
        this.hasher = hasher;
        this.serializer = serializer;
    }

    private int offset(int row, int col) {
        return row * width + col;
    }

    public void add(T item, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative values are not supported.");
        }
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, HashSeed.PRIMARY_HASH_SEED);
        long h2 = hasher.hash64(data, HashSeed.SECONDARY_HASH_SEED);

        for (int i = 0; i < depth; i++) {
            int col = Math.floorMod(h1 + i * h2, width);
            int idx = offset(i, col);
            table[idx] += value;
        }
        totalCount += value;
    }

    public long estimateCount(T item) {
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, HashSeed.PRIMARY_HASH_SEED);
        long h2 = hasher.hash64(data, HashSeed.SECONDARY_HASH_SEED);
        long min = Long.MAX_VALUE;

        for (int i = 0; i < depth; i++) {
            int col = Math.floorMod(h1 + i * h2, width);
            int idx = offset(i, col);
            min = Math.min(min, table[idx]);
        }

        return min;
    }

    public long getErrorBound() {
        return Math.round(epsilon * totalCount);
    }

}
