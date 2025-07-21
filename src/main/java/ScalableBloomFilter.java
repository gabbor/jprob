import java.util.ArrayList;
import java.util.List;

public class ScalableBloomFilter<T> {

    private final List<BloomFilter<T>> filters;
    private final double errorRate;
    private final long initialCapacity;
    private final double growthRate;
    private final double tighteningRatio;

    public ScalableBloomFilter(double errorRate, long initialCapacity, double growthRate, double errorRatio) {
        this.filters = new ArrayList<>();
        this.errorRate = errorRate;
        this.initialCapacity = initialCapacity;
        this.growthRate = growthRate;
        this.tighteningRatio = errorRatio;

        addNewFilter(errorRate * (1 - errorRatio), initialCapacity);
    }

    private void addNewFilter(double errorRate, long capacity) {
        BloomFilter<T> filter = new BloomFilter<>(errorRate, capacity);
        filters.add(filter);
    }

    public void add(T item) {
        BloomFilter<T> currentFilter = filters.getLast();
        currentFilter.add(item);

        // if saturation is high, add a new filter
        if (isSaturated(currentFilter)) {
            double newErrorRate = currentFilter.errorRate * tighteningRatio;
            long newCapacity = (long) (currentFilter.numElements * growthRate);
            addNewFilter(newErrorRate, newCapacity);
        }
    }

    public boolean contains(T item) {
        for (BloomFilter<T> filter : filters) {
            if (filter.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSaturated(BloomFilter<T> filter) {
        // Simple heuristic: if more than 50% of bits are set
        return filter.bitSet.cardinality() > filter.bitSet.size() * 0.5;
    }
}
