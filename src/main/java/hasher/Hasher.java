package hasher;

@FunctionalInterface
public interface Hasher {
    long hash64(byte[] data);
}
