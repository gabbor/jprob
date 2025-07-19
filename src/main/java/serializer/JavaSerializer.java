package serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;


/**
 * Serializer implementation using Java's built-in serialization.
 * The type T must implement java.io.Serializable.
 */
public class JavaSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(T item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(item);
            return bos.toByteArray();
        } catch (NotSerializableException e) {
            throw new RuntimeException("Class " + item.getClass().getName() + " must implement Serializable interface", e);
        }
        catch (IOException e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

}

