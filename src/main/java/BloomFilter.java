import hasher.Hasher;
import hasher.MurmurHash3;
import serializer.JavaSerializer;
import serializer.Serializer;

import java.util.BitSet;

public class BloomFilter<T> {

    private double errorRate;
    private long numElements;
    private final Hasher hasher;
    private final Serializer<T> serializer;
    private final int numHashes;
    private final BitSet bitSet;

    public BloomFilter(double errorRate, long numElements, Hasher hasher, Serializer<T> serializer) {
        this.errorRate = errorRate;
        this.numElements = numElements;
        this.hasher = hasher;
        this.serializer = serializer;
        this.bitSet = new BitSet(calculateBitSetSize(numElements, errorRate));
        this.numHashes = calculateNumHashes(bitSet.size(), numElements);
    }


    private int calculateBitSetSize(long n, double p) {
        return (int) Math.ceil(-n * Math.log(p) / (Math.pow(Math.log(2), 2)));
    }

    private int calculateNumHashes(int m, long n) {
        return (int) Math.round((m / (double) n) * Math.log(2));
    }

    public BloomFilter(double errorRate, long numElements) {
        this(errorRate, numElements, new MurmurHash3(), new JavaSerializer<>());
    }


    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        byte[] data = serializer.serialize(item);
        long seed = 0;
        int hashesAdded = 0;

        while (hashesAdded < numHashes) {
            long h = hasher.hash64(data, seed);
            int index = Math.floorMod(h, bitSet.size());
            bitSet.set(index);
            seed = h;
            hashesAdded++;
        }
    }


    public boolean contains(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        byte[] data = serializer.serialize(item);
        long seed = 0;
        int hashesAdded = 0;

        while (hashesAdded < numHashes) {
            long h = hasher.hash64(data, seed);
            int index = Math.floorMod(h, bitSet.size());
            if (! bitSet.get(index)) {
                return false;
            }
            seed = h;
            hashesAdded++;
        }

        return true;
    }


}
