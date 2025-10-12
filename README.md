# Probabilistic Data Structures in Java

This project provides Java implementations of several **probabilistic data structures**, designed for efficient memory usage and fast approximate computations on large datasets.

## 📦 Implemented Structures

- **HyperLogLog**: Estimates the cardinality (number of distinct elements) in a dataset.
- **Bloom Filter**: Tests whether an element is a member of a set, allowing false positives but no false negatives.
- **Scalable Bloom Filter**: A dynamic version of Bloom Filter that maintains a target false positive rate as the dataset grows.
- **HeavyKeeper**: Identifies heavy hitters (frequently occurring elements) in data streams.
- **Count-Min Sketch**: Approximates the frequency of elements in a stream with controlled error bounds.

## 🛠️ Technologies

- Java 21+
- Maven
- JUnit 5

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or higher
- Maven installed

### Installation

```bash
git clone https://github.com/gabbor/jprob.git
cd jprob
mvn clean install
```

## 📘 Examples

### 🔢 HyperLogLog

```java
import datastructures.HyperLogLog;

public class Main {
    public static void main(String[] args) {
        // Create a HyperLogLog instance with precision parameter b = 12
        // This means it will use 2^12 = 4096 registers
        // Default hashing (MurmurHash3) and serialization (JavaSerializer) will be used
        HyperLogLog<String> hll = new HyperLogLog<>(12);

        hll.add("apple");
        hll.add("banana");
        hll.add("orange");

        long estimate = hll.estimate();
        System.out.println("Estimated cardinality: " + estimate);
    }
}
```

#### ℹ️ Understanding the `b` Parameter in HyperLogLog

HyperLogLog uses a single precision parameter:

- **`b` (bit width)**:  
  This controls the number of registers used in the structure.  
  Specifically, it creates `2^b` registers, which directly affects both memory usage and estimation accuracy.

Typical values:
- `b = 4` → 16 registers (low accuracy, minimal memory)
- `b = 12` → 4096 registers (high accuracy, moderate memory)
- `b = 16` → 65,536 registers (very high accuracy, higher memory)

💡 Increasing `b` improves precision but also increases memory consumption.  
The recommended range is **4 ≤ b ≤ 16**, balancing performance and accuracy for most use cases.

### 🌸 Bloom Filter

```java
import datastructures.BloomFilter;

public class Main {
    public static void main(String[] args) {
        // Create a Bloom Filter with 1% error rate and expected 10,000 elements
        BloomFilter<String> filter = new BloomFilter<>(0.01, 10_000);

        // Add some items to the filter
        filter.add("apple");
        filter.add("banana");
        filter.add("orange");

        // Check for membership
        System.out.println("Contains 'apple'? " + filter.contains("apple"));   // true
        System.out.println("Contains 'grape'? " + filter.contains("grape"));   // likely false
    }
}
```

#### ℹ️ Understanding `errorRate` and `numElements`

Bloom Filter relies on two key parameters to balance memory usage and accuracy:

- **`errorRate`**:  
  This defines the target **false positive probability**—the likelihood that the filter incorrectly reports an element as present.  
  For example, an error rate of `0.01` means that approximately 1 in 100 queries for absent elements may return `true`.

- **`numElements`**:  
  This is the expected number of distinct elements you plan to insert into the filter.  
  It determines the size of the internal bit array and the number of hash functions.  
  If you insert significantly more elements than this value, the actual false positive rate will increase beyond the target.

💡 For dynamic or growing datasets, consider using a **Scalable Bloom Filter**, which adjusts its size while maintaining the desired error rate.


### 🧮 Count-Min Sketch

```java
import datastructures.CountMinSketch;

public class Main {
    public static void main(String[] args) {
        // Create a Count-Min Sketch with epsilon = 0.01 and delta = 0.0001
        CountMinSketch<String> sketch = new CountMinSketch<>(
            0.01, 0.0001,
            new hasher.MurmurHash3(),
            new serializer.JavaSerializer<>()
        );

        // Add elements with associated counts
        sketch.add("apple", 5);
        sketch.add("banana", 3);
        sketch.add("orange", 7);
        sketch.add("apple", 2); // total for "apple" should be ~7

        // Estimate frequency of elements
        System.out.println("Estimated count for 'apple': " + sketch.estimateCount("apple"));
        System.out.println("Estimated count for 'banana': " + sketch.estimateCount("banana"));
        System.out.println("Estimated count for 'grape': " + sketch.estimateCount("grape"));

        // Get error bound
        System.out.println("Error bound: ±" + sketch.getErrorBound());
    }
}
```

#### ℹ️ Understanding ε (epsilon) and δ (delta)

In Count-Min Sketch, two key parameters control the accuracy and reliability of frequency estimates:

- **ε (epsilon)**: This defines the **maximum error** in the estimated count, expressed as a fraction of the total number of items added.  
  For example, if ε = 0.01 and you've added 100,000 items, the error in any estimate will be at most ±1,000.

- **δ (delta)**: This defines the **probability that the error exceeds the bound ε**.  
  A smaller δ means higher confidence. For instance, δ = 0.0001 means there's a 0.01% chance that the error exceeds the bound.

Together, they guarantee that:
> With probability at least (1 − δ), the estimated count of any item is at most ε × total count above its true frequency.

Choosing smaller values for ε and δ improves accuracy but increases memory usage.


### 🧲 HeavyKeeper Top-K

```java
import datastructures.HeavyKeeperTopK;
import hasher.MurmurHash3;
import serializer.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // Create a HeavyKeeperTopK instance to track the top 2 most frequent flows
        HeavyKeeperTopK<String> tracker = new HeavyKeeperTopK<>(
            3,      // d: number of hash functions
            100,    // w: width of each hash table
            2,      // k: number of top elements to maintain
            1.08,   // b: base for probabilistic decrement
            new MurmurHash3(),
            (Serializer<String>) obj -> obj.getBytes(StandardCharsets.UTF_8)
        );

        // Insert many unique flows
        for (int i = 0; i < 100_000; i++) {
            tracker.insert("Player" + i);
        }

        // Insert frequent flows
        for (int i = 0; i < 100_000; i++) {
            tracker.insert("Maradona");
            tracker.insert("Messi");
        }

        // Retrieve and print the top-K elements
        System.out.println("Top-K Flows:");
        for (Map.Entry<String, Integer> entry : tracker.getTopK().entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}
```

#### ℹ️ Understanding HeavyKeeperTopK Parameters

HeavyKeeperTopK is designed to track the most frequent elements (top-k) in high-volume data streams using probabilistic counting. Here's what each parameter controls:

- **`d` (depth)**:  
  Number of hash functions and rows in the sketch.  
  More rows reduce collisions and improve accuracy.

- **`w` (width)**:  
  Number of buckets per row.  
  A larger width increases resolution and reduces noise in frequency estimation.

- **`k`**:  
  The number of top elements to maintain.  
  The structure will always return the top-k most frequent items seen so far.

- **`b` (base)**:  
  Base used for probabilistic decrementing of counters.  
  A typical value is slightly above 1 (e.g. `1.08`).  

💡 This structure is ideal for identifying **heavy hitters** in streaming data with limited memory and high throughput.


