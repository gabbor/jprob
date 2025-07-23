public interface IBloomFilter<T> {

    void add(T item);

    boolean contains(T item);

}
