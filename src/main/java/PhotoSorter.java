import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PhotoSorter {
    private final static AtomicLong counter = new AtomicLong(0);
    private final static Map<String, List<Photo>> photosByYear = Collections.synchronizedMap(new HashMap<>());
    private final static List<File> unsortedFiles = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите путь к папке, которую хотите копировать и отсортировать " +
                "фото по дате (путь должен содержать только английские символы или цифры: ");
        String folderPathForCopy = scanner.nextLine();
        folderPathForCopy = folderPathForCopy.replace("\\", File.separator);
        File folderForCopy = new File(folderPathForCopy);

        System.out.print("Введите путь к папке, в которую хотите копировать и отсортировать " +
                "фото по дате (путь должен содержать только английские символы или цифры: ");
        String folderPathForPaste = scanner.nextLine();
        folderPathForPaste = folderPathForPaste.replace("\\", File.separator)
                .concat(File.separator)
                .concat("Мои отсортированные фото");
        File folderForPaste = new File(folderPathForPaste);

        if (folderForPaste.exists()) {
            deleteDir(folderForPaste);
        }
        folderForPaste.mkdirs();

        fillPhotosByYear(folderForCopy);

        for (List<Photo> photos : photosByYear.values()) {
            photos.sort(Comparator.comparing(Photo::creationTime));
        }

        new Thread(() -> createFilesFromPhotosByYear(
                new File(folderForPaste.getAbsolutePath().concat(File.separator).concat("photosByYear")))).start();
        new Thread(() -> justCopyUnsortedFiles(
                new File(folderForPaste.getAbsolutePath().concat(File.separator).concat("unsortedFiles")))).start();
    }

    private static void fillPhotosByYear(File folderForCopy) {
        if (folderForCopy.listFiles() == null) {
            System.out.println("""
                    ----------------------------------------------------------
                    Введенной директории (источника копирования) не существует
                    ----------------------------------------------------------""");
            return;
        }

        for (File photoForCopy : Objects.requireNonNull(folderForCopy.listFiles())) {
            if (photoForCopy.isDirectory()) {
                fillPhotosByYear(photoForCopy);
            } else {
                if (!photoForCopy.getName().toLowerCase().endsWith(".jpg") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".png") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".jpeg") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".bmp") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".tiff") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".tif") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".heif") &&
                        !photoForCopy.getName().toLowerCase().endsWith(".heic")) {
                    unsortedFiles.add(photoForCopy);
                    continue;
                }

                LocalDateTime creationDateOfPhoto = getPhotoCreationDate(photoForCopy);

                if (creationDateOfPhoto == null) {
                    try {
                        creationDateOfPhoto = Files.getLastModifiedTime(
                                        photoForCopy.toPath())
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }

                if (creationDateOfPhoto == null) {
                    unsortedFiles.add(photoForCopy);
                    continue;
                }

                if (!photosByYear.containsKey(String.valueOf(creationDateOfPhoto.getYear()))) {
                    photosByYear.put(String.valueOf(creationDateOfPhoto.getYear()),
                            Collections.synchronizedList(new ArrayList<>()));
                }

                photosByYear.get(String.valueOf(creationDateOfPhoto.getYear())).add(new Photo(photoForCopy, creationDateOfPhoto));
            }
        }
    }

    private static void deleteDir(File dir) {
        if (dir.listFiles() == null) {
            return;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                deleteDir(file);
            }
            file.delete();
        }

        dir.delete();
    }

    private static void createFilesFromPhotosByYear(File folderForPaste) {
        for (Map.Entry<String, List<Photo>> entry : photosByYear.entrySet()) {
            new Thread(() -> {
                int i = 1;

                File folderForPasteByYear = new File(folderForPaste.getAbsolutePath().concat(File.separator).concat(entry.getKey()));

                if (!folderForPasteByYear.exists()) {
                    folderForPasteByYear.mkdirs();
                }

                for (Photo photo : entry.getValue()) {
                    File fileForPaste = getFileForPaste(photo, i, folderForPasteByYear);
                    try {
                        Files.copy(photo.path().toPath(), fileForPaste.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        FileTime lastModifiedTime = Files.getLastModifiedTime(photo.path().toPath());
                        Files.setLastModifiedTime(fileForPaste.toPath(), lastModifiedTime);
                        System.out.printf("[%s] %s файлов скопировано\n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                counter.addAndGet(1));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    i++;
                }
            }).start();
        }
    }

    private static void justCopyUnsortedFiles(File folderForPaste) {
        if (unsortedFiles.isEmpty()) {
            return;
        }

        if (!folderForPaste.exists()) {
            folderForPaste.mkdirs();
        }

        for (File file : Objects.requireNonNull(unsortedFiles)) {
            try {
                Files.copy(
                        file.toPath(),
                        new File(
                                folderForPaste.getAbsolutePath()
                                        .concat(File.separator)
                                        .concat(file.getName())).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static File getFileForPaste(Photo photo, int i, File folderForPasteByYear) {
        String newFileNameForPaste;

        if (photo.path().getName().contains(".")) {
            String fileType = photo.path().getName().substring(photo.path().getName().lastIndexOf("."));
            newFileNameForPaste = String.valueOf(i).concat(fileType);
        } else {
            newFileNameForPaste = String.valueOf(i);
        }

        return new File(folderForPasteByYear + File.separator + newFileNameForPaste);
    }

    private static LocalDateTime getPhotoCreationDate(File photo) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(photo);
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

record Photo(File path, LocalDateTime creationTime) {
}