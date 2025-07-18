import java.io.Serializable;
import java.time.LocalDate;

public record Person(String name, String surname, int height, LocalDate birthdate) implements Serializable {
}
