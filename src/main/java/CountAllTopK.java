import java.util.*;

import hasher.Hasher;
import serializer.Serializer;


public class CountAllTopK<T> extends CountMinSketch<T> {
    private final int k;
    private final Map<T, Element<T>> elementMap; // Tracks items in the heap
    private final PriorityQueue<Element<T>> minHeap; // Maintains lowest count at top

    public CountAllTopK(double epsilon, double delta, int k, Hasher hasher, Serializer<T> serializer) {
        super(epsilon, delta, hasher, serializer);
        this.k = k;
        this.elementMap = new HashMap<>(k);
        this.minHeap = new PriorityQueue<>(k, Comparator.comparingLong(e -> e.count));
    }

    @Override
    public void add(T item, long value) {
        super.add(item, value); // Update the sketch
        long estimated = super.estimateCount(item);

        if (elementMap.containsKey(item)) {
            // Item is already in Top-K: refresh its count
            // Updating priority requires removal + reinsertion: O(K) in worst case
            Element<T> existing = elementMap.get(item);
            minHeap.remove(existing);
            existing.count = estimated;
            minHeap.offer(existing);
        } else if (elementMap.size() < k) {
            // Still space in Top-K: add new item
            Element<T> newElement = new Element<>(item, estimated);
            elementMap.put(item, newElement);
            minHeap.offer(newElement);
        } else {
            // Replace least frequent item if new one is heavier
            assert minHeap.peek() != null;
            if (estimated > minHeap.peek().count) {
                Element<T> removed = minHeap.poll();
                if (removed != null) elementMap.remove(removed.item);
                Element<T> newElement = new Element<>(item, estimated);
                elementMap.put(item, newElement);
                minHeap.offer(newElement);
            }
        }
    }


    public List<Map.Entry<T, Long>> getTopK() {
        List<Map.Entry<T, Long>> result = new ArrayList<>();
        for (Element<T> e : minHeap) {
            result.add(new AbstractMap.SimpleEntry<>(e.item, e.count));
        }
        result.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return result;
    }

    private static class Element<T> {
        final T item;
        long count;

        Element(T item, long count) {
            this.item = item;
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Element<?> element)) return false;
            return Objects.equals(item, element.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }
}

