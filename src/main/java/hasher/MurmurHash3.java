package hasher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MurmurHash3 implements Hasher {

    public long hash64(byte[] data, long seed) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        seed =  seed & 0xFFFFFFFFL;
        long h1 = seed;
        long h2 = seed;

        long c1 = 0x87c37b91114253d5L;
        long c2 = 0x4cf5ad432745937fL;

        while (buffer.remaining() >= 16) {
            long k1 = buffer.getLong();
            long k2 = buffer.getLong();

            // mix k1
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;

            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            // mix k2
            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;

            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        long k1 = 0;
        long k2 = 0;
        int remaining = buffer.remaining();
        int pos = buffer.position(); // Current position in buffer

        // Handle remaining bytes using absolute indexing from original array
        switch (remaining) {
            case 15: k2 ^= ((long) (data[pos + 14] & 0xff)) << 48;
            case 14: k2 ^= ((long) (data[pos + 13] & 0xff)) << 40;
            case 13: k2 ^= ((long) (data[pos + 12] & 0xff)) << 32;
            case 12: k2 ^= ((long) (data[pos + 11] & 0xff)) << 24;
            case 11: k2 ^= ((long) (data[pos + 10] & 0xff)) << 16;
            case 10: k2 ^= ((long) (data[pos + 9] & 0xff)) << 8;
            case 9:  k2 ^= data[pos + 8] & 0xff;
            case 8:  k1 ^= buffer.getLong(); break;
            case 7:  k1 ^= ((long) (data[pos + 6] & 0xff)) << 48;
            case 6:  k1 ^= ((long) (data[pos + 5] & 0xff)) << 40;
            case 5:  k1 ^= ((long) (data[pos + 4] & 0xff)) << 32;
            case 4:  k1 ^= ((long) (data[pos + 3] & 0xff)) << 24;
            case 3:  k1 ^= ((long) (data[pos + 2] & 0xff)) << 16;
            case 2:  k1 ^= ((long) (data[pos + 1] & 0xff)) << 8;
            case 1:  k1 ^= data[pos] & 0xff; break;
        }

        if (remaining > 0) {
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;

            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;
        }

        // Finalization
        h1 ^= data.length;
        h2 ^= data.length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;

        return h1;
    }

    private long fmix64(long k) {
        k ^= (k >>> 33);
        k *= 0xff51afd7ed558ccdL;
        k ^= (k >>> 33);
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= (k >>> 33);
        return k;
    }
}