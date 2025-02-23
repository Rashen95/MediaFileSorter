package RepositoryFiller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public final class PhotoRepositoryFiller extends RepositoryFiller {
    @Override
    protected boolean formatIsCorrect(File photo) {
        return photo.getName().toLowerCase().endsWith(".jpg") ||
                photo.getName().toLowerCase().endsWith(".png") ||
                photo.getName().toLowerCase().endsWith(".jpeg") ||
                photo.getName().toLowerCase().endsWith(".bmp") ||
                photo.getName().toLowerCase().endsWith(".tiff") ||
                photo.getName().toLowerCase().endsWith(".tif") ||
                photo.getName().toLowerCase().endsWith(".heif") ||
                photo.getName().toLowerCase().endsWith(".heic");
    }

    @Override
    protected LocalDateTime getCreationDateTimeFromMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null) {
                Date date = exifDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}