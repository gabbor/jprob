import hasher.Hasher;
import hasher.MurmurHash3;
import serializer.JavaSerializer;
import serializer.Serializer;

import java.util.BitSet;

public class BloomFilter<T> implements IBloomFilter<T> {
    final double errorRate;
    final long numElements;
    final BitSet bitSet;
    int bitsSetCount = 0;
    private final Hasher hasher;
    private final Serializer<T> serializer;
    private final int numHashes;
    private final int sliceSize;
    private final long primarySeed;
    private final long secondarySeed;

    public BloomFilter(double errorRate, long numElements, Hasher hasher, Serializer<T> serializer, long primarySeed, long secondarySeed) {
        this.errorRate = errorRate;
        this.numElements = numElements;
        this.hasher = hasher;
        this.serializer = serializer;
        this.primarySeed = primarySeed;
        this.secondarySeed = secondarySeed;

        int bitSetSize = calculateBitSetSize(numElements, errorRate);
        this.numHashes = calculateNumHashes(bitSetSize, numElements);

        // Ensure that the BitSet can be evenly divided into equal-sized slices
        this.sliceSize = (int) Math.ceil((double) bitSetSize / numHashes);
        int adjustedSize = sliceSize * numHashes;

        this.bitSet = new BitSet(adjustedSize);
    }

    public BloomFilter(double errorRate, long numElements, Hasher hasher, Serializer<T> serializer) {
        this(errorRate, numElements, hasher, serializer, HashSeed.PRIMARY_HASH_SEED, HashSeed.SECONDARY_HASH_SEED);
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

    @Override
    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, primarySeed);
        long h2 = hasher.hash64(data, secondarySeed);
        this.add(h1, h2);
    }

    void add(long h1, long h2) {
        for (int i = 0; i < numHashes; i++) {
            long combined = h1 + i * h2;
            int indexInSlice = (int) Long.remainderUnsigned(combined, sliceSize);
            int index = i * sliceSize + indexInSlice;
            if (!bitSet.get(index)) {
                bitSet.set(index);
                bitsSetCount++;
            }
        }
    }

    @Override
    public boolean contains(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, primarySeed);
        long h2 = hasher.hash64(data, secondarySeed);
        return contains(h1, h2);
    }


    boolean contains(long h1, long h2) {
        for (int i = 0; i < numHashes; i++) {
            long combined = h1 + i * h2;
            int indexInSlice = (int) Long.remainderUnsigned(combined, sliceSize);
            int index = i * sliceSize + indexInSlice;
            if (!bitSet.get(index)) {
                return false;
            }
        }
        return true;
    }


}
