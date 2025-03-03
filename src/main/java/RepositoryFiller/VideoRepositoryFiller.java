package RepositoryFiller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public final class VideoRepositoryFiller extends RepositoryFiller {
    @Override
    protected boolean formatIsCorrect(File file) {
        return file.getName().toLowerCase().endsWith(".mp4") ||
                file.getName().toLowerCase().endsWith(".avi") ||
                file.getName().toLowerCase().endsWith(".mkv") ||
                file.getName().toLowerCase().endsWith(".mov") ||
                file.getName().toLowerCase().endsWith(".wmv") ||
                file.getName().toLowerCase().endsWith(".flv") ||
                file.getName().toLowerCase().endsWith(".webm") ||
                file.getName().toLowerCase().endsWith(".mpeg") ||
                file.getName().toLowerCase().endsWith(".mpg") ||
                file.getName().toLowerCase().endsWith(".3gp");
    }

    @Override
    protected LocalDateTime getCreationDateTimeFromMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            Mp4Directory mp4Directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
            if (mp4Directory != null) {
                Date date = mp4Directory.getDate(Mp4Directory.TAG_CREATION_TIME);
                if (date != null) {
                    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("+00:00"));
                }
            }
            QuickTimeDirectory quickTimeDirectory = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class);
            if (quickTimeDirectory != null) {
                Date date = quickTimeDirectory.getDate(QuickTimeDirectory.TAG_CREATION_TIME);
                if (date != null) {
                    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("+00:00"));
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}