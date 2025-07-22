import serializer.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CustomTestItemSerializer implements Serializer<TestItem> {

    @Override
    public byte[] serialize(TestItem obj) {
        byte[] fieldABytes = obj.fieldA().getBytes(StandardCharsets.UTF_8);
        byte[] fieldBBytes = obj.fieldB().getBytes(StandardCharsets.UTF_8);

        // LocalDate as 3 integers: year, month, day
        int year = obj.dateValue().getYear();
        int month = obj.dateValue().getMonthValue();
        int day = obj.dateValue().getDayOfMonth();

        // Allocate buffer with fixed + variable size
        ByteBuffer buffer = ByteBuffer.allocate(
                fieldABytes.length + fieldBBytes.length
                        + 4 + 4 + 4 + 4 // intValue + year + month + day
        );

        buffer.put(fieldABytes);
        buffer.put(fieldBBytes);
        buffer.putInt(obj.intValue());
        buffer.putInt(year);
        buffer.putInt(month);
        buffer.putInt(day);
        return buffer.array();
    }
}

