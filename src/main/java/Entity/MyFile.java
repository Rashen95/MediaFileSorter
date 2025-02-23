package Entity;

import java.io.File;
import java.time.LocalDateTime;

public record MyFile(File path, LocalDateTime creationDateTime) {
}