package entity;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.File;
import java.time.LocalDateTime;

@Value
@Builder
public class MyFile {

    @EqualsAndHashCode.Exclude
    File path;

    LocalDateTime creationDateTime;

    Long size;
}