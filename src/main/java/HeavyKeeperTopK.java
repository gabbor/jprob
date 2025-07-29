import hasher.Hasher;
import hasher.MurmurHash3;
import serializer.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HeavyKeeperTopK<T> {

    private static class Bucket {
        long fingerprint;
        int count;

        Bucket() {
            this.fingerprint = 0;
            this.count = 0;
        }
    }

    private final int d; // number of hash functions
    private final int w; // width of each table
    private final double b; // base for probabilistic decrement
    private final Bucket[][] sketch;
    private final Hasher hasher;
    private final Serializer<T> serializer;
    private final long[] seeds;
    private final Map<T, Integer> minHeap;
    private final int k;

    public HeavyKeeperTopK(int d, int w, int k, double b, Hasher hasher, Serializer<T> serializer) {
        this.d = d;
        this.w = w;
        this.k = k;
        this.b = b;
        this.hasher = hasher;
        this.serializer = serializer;
        this.sketch = new Bucket[d][w];
        this.seeds = new long[d];
        this.minHeap = new HashMap<>();

        Random rand = new Random();
        for (int i = 0; i < d; i++) {
            seeds[i] = rand.nextLong();
            for (int j = 0; j < w; j++) {
                sketch[i][j] = new Bucket();
            }
        }
    }

    public void insert(T flow) {
        boolean inHeap = minHeap.containsKey(flow);
        int nmin = minHeap.values().stream().min(Integer::compareTo).orElse(0);
        int maxv = 0;

        byte[] flowBytes = serializer.serialize(flow);
        long fp = hasher.hash64(flowBytes, 0); // fingerprint

        for (int j = 0; j < d; j++) {
            int idx = (int) (Math.abs(hasher.hash64(flowBytes, seeds[j])) % w);
            Bucket entry = sketch[j][idx];

            if (entry.fingerprint == fp) {
                if (inHeap || entry.count <= nmin) { // Selective Increment
                    entry.count++;
                    maxv = Math.max(maxv, entry.count);
                }
            } else {
                if (Math.random() < Math.pow(b, -entry.count)) {
                    entry.count--;
                    if (entry.count <= 0) {
                        entry.fingerprint = fp;
                        entry.count = 1;
                        maxv = Math.max(maxv, 1);
                    }
                }
            }
        }

        if (inHeap) {
            minHeap.put(flow, Math.max(maxv, minHeap.get(flow)));

        } else if (minHeap.size() < k || maxv - nmin == 1) { // Fingerprint Collisions Detection
            minHeap.put(flow, maxv);
            if (minHeap.size() > k) {
                T minKey = Collections.min(minHeap.entrySet(), Map.Entry.comparingByValue()).getKey();
                minHeap.remove(minKey);
            }
        }
    }

    public Map<T, Integer> getTopK() {
        return new HashMap<>(minHeap);
    }


    public static void main(String[] args) {
        Hasher simpleHasher = new MurmurHash3();
        Serializer<String> stringSerializer = obj -> obj.getBytes(StandardCharsets.UTF_8);

        HeavyKeeperTopK<String> tracker = new HeavyKeeperTopK<>(
                3,      // d: number of hash functions
                100,    // w: width of the tables
                2,      // k: top-k to maintain
                1.08,   // b: base for probabilistic decrement
                simpleHasher,
                stringSerializer
        );

        for (int i = 0; i < 100000; i++) {
            tracker.insert("Player" + i);
        }

        for (int i = 0; i < 100000; i++) {
            tracker.insert("Maradona");
        }
        for (int i = 0; i < 100000; i++) {
            tracker.insert("Messi");
        }

        System.out.println("Top-K Flows:");
        for (Map.Entry<String, Integer> entry : tracker.getTopK().entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }


}
