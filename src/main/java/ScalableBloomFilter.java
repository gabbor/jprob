import hasher.Hasher;
import hasher.MurmurHash3;
import serializer.Serializer;

import java.util.ArrayList;
import java.util.List;

public class ScalableBloomFilter<T> implements IBloomFilter<T> {

    private final List<BloomFilter<T>> filters;
    private final double errorRate;
    private final long initialCapacity;
    private final double growthRate;
    private final double tighteningRatio;
    private final Serializer<T> serializer;
    private final Hasher hasher;

    public ScalableBloomFilter(double errorRate, long initialCapacity, double growthRate, double errorRatio, Hasher hasher, Serializer<T> serializer) {
        this.filters = new ArrayList<>();
        this.errorRate = errorRate;
        this.initialCapacity = initialCapacity;
        this.growthRate = growthRate;
        this.tighteningRatio = errorRatio;
        this.serializer = serializer;
        this.hasher = hasher;
        addNewFilter(errorRate * (1 - errorRatio), initialCapacity);
    }

    private void addNewFilter(double errorRate, long capacity) {
        BloomFilter<T> filter = new BloomFilter<>(errorRate, capacity, new MurmurHash3(), this.serializer);
        filters.add(filter);
    }

    @Override
    public void add(T item) {
        BloomFilter<T> currentFilter = filters.getLast();
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, HashSeed.PRIMARY_HASH_SEED);
        long h2 = hasher.hash64(data, HashSeed.SECONDARY_HASH_SEED);
        if (!this.contains(h1, h2)) {
            // if saturation is high, add a new filter
            if (isSaturated(currentFilter)) {
                double newErrorRate = currentFilter.errorRate * tighteningRatio;
                long newCapacity = (long) (currentFilter.numElements * growthRate);
                addNewFilter(newErrorRate, newCapacity);
            }
            filters.getLast().add(h1, h2);
        }

    }

    @Override
    public boolean contains(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        byte[] data = serializer.serialize(item);
        long h1 = hasher.hash64(data, HashSeed.PRIMARY_HASH_SEED);
        long h2 = hasher.hash64(data, HashSeed.SECONDARY_HASH_SEED);
        return this.contains(h1, h2);
    }


    boolean contains(long h1, long h2) {
        for (BloomFilter<T> filter : filters) {
            if (filter.contains(h1, h2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSaturated(BloomFilter<T> filter) {
        // Simple heuristic: if more than 50% of bits are set
        return filter.bitsSetCount > filter.bitSet.size() * 0.5;
    }

}
