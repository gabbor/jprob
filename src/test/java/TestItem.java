import java.io.Serializable;
import java.time.LocalDate;

public record TestItem(String fieldA, String fieldB, int intValue, LocalDate dateValue) implements Serializable {
}