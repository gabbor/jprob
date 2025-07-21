import hasher.Hasher;
import hasher.MurmurHash3;
import serializer.JavaSerializer;
import serializer.Serializer;

import java.util.BitSet;

public class BloomFilter<T> {

    private final double errorRate;
    private final long numElements;
    private final Hasher hasher;
    private final Serializer<T> serializer;
    private final int numHashes;
    private final BitSet bitSet;
    private final int sliceSize;

    public BloomFilter(double errorRate, long numElements, Hasher hasher, Serializer<T> serializer) {
        this.errorRate = errorRate;
        this.numElements = numElements;
        this.hasher = hasher;
        this.serializer = serializer;

        int bitSetSize = calculateBitSetSize(numElements, errorRate);
        this.numHashes = calculateNumHashes(bitSetSize, numElements);

        // Ensure that the BitSet can be evenly divided into equal-sized slices
        this.sliceSize = (int) Math.ceil((double) bitSetSize / numHashes);
        int adjustedSize = sliceSize * numHashes;

        this.bitSet = new BitSet(adjustedSize);
    }

    public BloomFilter(double errorRate, long numElements) {
        this(errorRate, numElements, new MurmurHash3(), new JavaSerializer<>());
    }

    private int calculateBitSetSize(long n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.pow(Math.log(2), 2)));
    }

    private int calculateNumHashes(int m, long n) {
        return (int) Math.round((m / (double) n) * Math.log(2));
    }

    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        byte[] data = serializer.serialize(item);
        long seed = 0;

        for (int i = 0; i < numHashes; i++) {
            long h = hasher.hash64(data, seed);
            int indexInSlice = Math.floorMod(h, sliceSize);
            int index = i * sliceSize + indexInSlice;
            bitSet.set(index);
            seed = h;
        }
    }

    public boolean contains(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        byte[] data = serializer.serialize(item);
        long seed = 0;

        for (int i = 0; i < numHashes; i++) {
            long h = hasher.hash64(data, seed);
            int indexInSlice = Math.floorMod(h, sliceSize);
            int index = i * sliceSize + indexInSlice;
            if (!bitSet.get(index)) {
                return false;
            }
            seed = h;
        }

        return true;
    }
}
