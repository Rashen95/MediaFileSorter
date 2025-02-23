import Entity.FileType;
import Entity.MyFile;
import Repository.FileRepository;
import RepositoryFiller.PhotoRepositoryFiller;
import RepositoryFiller.VideoRepositoryFiller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class MediaFileSorter {
    private final AtomicLong fileCounter = new AtomicLong();
    private FileType typeFilesForSorted;
    private File sourceFolder;
    private File folderForSortMedia;
    private final FileRepository fileRepository = FileRepository.getInstance();

    public void sort() {
        // Что сортируем?
        userSelectionTypeFilesForSorted();

        if (typeFilesForSorted == null) {
            return;
        }

        // Выбор директории для сортировки
        changeDirectoryForSort();

        // Наполнение репозитория файлами выбранного типа
        if (typeFilesForSorted == FileType.Photo) {
            new PhotoRepositoryFiller().fillRepositoryBySourceFolder(sourceFolder);
        } else if (typeFilesForSorted == FileType.Video) {
            new VideoRepositoryFiller().fillRepositoryBySourceFolder(sourceFolder);
        } else {
            return;
        }

        // Создание директории для отсортированных файлов
        createDirectoryForSortMedia();

        // Сортировка
        for (List<MyFile> files : fileRepository.getFilesByYear().values()) {
            files.sort(Comparator.comparing(MyFile::creationDateTime));
        }

        // Создание файлов
        createFilesFromRepositoryInFolderForSortMedia();

        // Копирование неотсортированных файлов (без даты или несоответствующего формата)
        copyUnsortedFiles();
    }

    private void changeDirectoryForSort() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите путь к папке, в которой хотите отсортировать "
                + typeFilesForSorted.name()
                + " по дате (путь должен содержать только английские символы или цифры: ");
        String folderPathForCopy = scanner.nextLine();
        folderPathForCopy = folderPathForCopy.replace("\\", File.separator);
        sourceFolder = new File(folderPathForCopy);

        folderForSortMedia = new File(sourceFolder.getAbsolutePath().concat(File.separator).concat("Мои отсортированные файлы"));
    }

    private void userSelectionTypeFilesForSorted() {
        label:
        while (true) {
            System.out.println("""
                    Выбор типа файлов для сортировки:
                    1. Photo
                    2. Video
                    3. Завершить работу программы
                    """);
            System.out.print("Введите соответствующую цифру: ");
            String change = new Scanner(System.in).nextLine().strip();
            switch (change) {
                case "1":
                    typeFilesForSorted = FileType.Photo;
                    break label;
                case "2":
                    typeFilesForSorted = FileType.Video;
                    break label;
                case "3":
                    System.out.println("Завершаю работу программы");
                    break label;
                default:
                    System.out.println("----------------------");
                    System.out.println("Такого пункта меню нет");
                    System.out.println("----------------------");
                    break;
            }
        }
    }

    private void createDirectoryForSortMedia() {
        if (folderForSortMedia.exists()) {
            deleteDir(folderForSortMedia);
        }
        folderForSortMedia.mkdirs();
    }

    private void deleteDir(File dir) {
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

    private void createFilesFromRepositoryInFolderForSortMedia() {
        for (Map.Entry<String, List<MyFile>> entry : fileRepository.getFilesByYear().entrySet()) {
            new Thread(() -> {
                int i = 1;

                File folderForPasteByYear = new File(
                        folderForSortMedia.getAbsolutePath().concat(File.separator).concat(entry.getKey()));

                if (!folderForPasteByYear.exists()) {
                    folderForPasteByYear.mkdirs();
                }

                for (MyFile file : entry.getValue()) {
                    File fileForPaste = getFileForPaste(file, i, folderForPasteByYear);
                    try {
                        Files.copy(file.path().toPath(), fileForPaste.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        FileTime lastModifiedTime = Files.getLastModifiedTime(file.path().toPath());
                        Files.setLastModifiedTime(fileForPaste.toPath(), lastModifiedTime);
                        System.out.printf("%s файлов скопировано\n",
                                fileCounter.addAndGet(1));
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    i++;
                }
            }).start();
        }
    }

    private static File getFileForPaste(MyFile file, int i, File folderForPasteByYear) {
        String newFileNameForPaste;

        if (file.path().getName().contains(".")) {
            String fileType = file.path().getName().substring(file.path().getName().lastIndexOf("."));
            newFileNameForPaste = String.valueOf(i).concat(fileType);
        } else {
            newFileNameForPaste = String.valueOf(i);
        }

        return new File(folderForPasteByYear + File.separator + newFileNameForPaste);
    }

    private void copyUnsortedFiles() {
        if (fileRepository.getUnsortedFiles().isEmpty()) {
            return;
        }

        new Thread(() -> {
            File folderForPasteUnsortedFiles =
                    new File(folderForSortMedia.getAbsolutePath().concat(File.separator).concat("unsortedFiles"));

            if (!folderForPasteUnsortedFiles.exists()) {
                folderForPasteUnsortedFiles.mkdirs();
            }

            for (File file : Objects.requireNonNull(fileRepository.getUnsortedFiles())) {
                File newFile = new File(
                        folderForPasteUnsortedFiles.getAbsolutePath()
                                .concat(File.separator)
                                .concat(file.getName()));

                while (newFile.exists()) {
                    newFile = new File(newFile.getAbsolutePath().concat("(1)"));
                }

                try {
                    Files.copy(
                            file.toPath(),
                            newFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();
    }
}
