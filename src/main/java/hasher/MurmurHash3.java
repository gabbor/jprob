package hasher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MurmurHash3 implements Hasher {

    @Override
    public long hash64(byte[] data, long seed) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        long h1 = seed;
        long c1 = 0x87c37b91114253d5L;
        long c2 = 0x4cf5ad432745937fL;

        while (buffer.remaining() >= 8) {
            long k1 = buffer.getLong();
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;

            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 = h1 * 5 + 0x52dce729;
        }

        long k1 = 0;
        int remaining = buffer.remaining();
        for (int i = 0; i < remaining; i++) {
            k1 ^= ((long) buffer.get() & 0xffL) << (i * 8);
        }

        if (remaining > 0) {
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;
        }

        h1 ^= data.length;
        h1 ^= (h1 >>> 33);
        h1 *= 0xff51afd7ed558ccdL;
        h1 ^= (h1 >>> 33);
        h1 *= 0xc4ceb9fe1a85ec53L;
        h1 ^= (h1 >>> 33);

        return h1;
    }
}
