package serializer;

public interface Serializer<T> {

    byte[] serialize(T obj);

}
