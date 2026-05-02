import entity.FileType;
import repository.FileRepository;
import service.creator.FileCreator;
import service.repositoryFiller.PhotoRepositoryFiller;
import service.repositoryFiller.VideoRepositoryFiller;

import java.io.File;
import java.util.Objects;
import java.util.Scanner;

public class MediaFileSorter {
    private FileType typeFilesForSorted;
    private File sourceFolder;
    private File folderForSortMedia;

    private final Scanner scanner = new Scanner(System.in);
    private final FileCreator fileCreator = FileCreator.getInstance();
    private final FileRepository fileRepository = FileRepository.getInstance();

    public void start() {
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

        // Создание файлов
        fileCreator.createFilesFromRepositoryInFolderForSortMedia(fileRepository, folderForSortMedia);

        // Копирование неотсортированных файлов (без даты)
        fileCreator.copyUnsortedFiles(fileRepository, folderForSortMedia);
    }

    private void changeDirectoryForSort() {
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
            String change = scanner.nextLine().strip();
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
}