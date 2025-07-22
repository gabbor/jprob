package serializer;

import java.nio.charset.Charset;

public class StringSerializer implements Serializer<String> {

    Charset charset;

    public StringSerializer(Charset charset) {
        this.charset = charset;
    }

    @Override
    public byte[] serialize(String obj) {
        return obj.getBytes(this.charset);
    }
}
