import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Test {
    public static void main(String[] args) {
        DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
        System.out.println(FORMATTER.format(Instant.now()));
    }
}