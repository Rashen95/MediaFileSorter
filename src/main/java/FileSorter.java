import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class FileSorter {
    private final static AtomicLong counter = new AtomicLong(0);
    private final static ExecutorService executor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static boolean usedAllThreads;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите путь к папке, которую хотите копировать и отсортировать файлы по дате: ");
        String folderPathForCopy = scanner.nextLine();
        File folderForCopy = new File(folderPathForCopy);

        while (true) {
            System.out.println("""
                    Использовать все потоки процессора?
                    1. Да
                    2. Нет
                    Введите цифру соответствующую предпочтительного для Вас пункта меню:""");
            String userChange = scanner.nextLine().strip();
            if (userChange.equals("1")) {
                usedAllThreads = true;
                break;
            } else if (userChange.equals("2")) {
                usedAllThreads = false;
                break;
            } else {
                System.out.println("""
                        ------------------------------
                        Вы ввели недопустимое значение
                        ------------------------------""");
            }
        }

        String folderPathForPaste = "src/main/resources/Мои отсортированные файлы/";
        File folderForPaste = new File(folderPathForPaste);
        if (folderForPaste.exists()) {
            deleteDir(folderForPaste);
        }
        if (folderForPaste.mkdirs()) {
            System.out.println("Директория: \"" + folderForPaste.getAbsolutePath() + "\" создана");
        }

        sortAndCopyFiles(folderForCopy, folderForPaste);
    }

    private static void sortAndCopyFiles(File folderForCopy, File folderForPaste) {
        List<MyFile> myFiles = new ArrayList<>();

        if (folderForCopy.listFiles() == null) {
            System.out.println("""
                    ----------------------------------
                    Введенной директории не существует
                    ----------------------------------""");
            return;
        }

        for (File fileForCopy : Objects.requireNonNull(folderForCopy.listFiles())) {
            if (fileForCopy.isDirectory()) {
                File newFolderForPaste = new File(folderForPaste + "/" + fileForCopy.getName());
                if (newFolderForPaste.mkdirs()) {
                    System.out.println("Директория: \"" + newFolderForPaste.getAbsolutePath() + "\" создана");
                }
                sortAndCopyFiles(fileForCopy, newFolderForPaste);
            } else {
                try {
                    myFiles.add(new MyFile(
                                    fileForCopy.getName(),
                                    Files.readAttributes(fileForCopy.toPath(), BasicFileAttributes.class)
                                            .lastModifiedTime()
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                            )
                    );
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        myFiles.sort(Comparator.comparing(MyFile::creationTime));

        if (usedAllThreads) {
            executor.execute(() -> createFilesFromSortedList(myFiles, folderForCopy, folderForPaste));
        } else {
            createFilesFromSortedList(myFiles, folderForCopy, folderForPaste);
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
            if (file.delete()) {
                System.out.println("Файл: \"" + file.getAbsolutePath() + "\" удален");
            }
        }
        if (dir.delete()) {
            System.out.println("Директория: \"" + dir.getAbsolutePath() + "\" удалена");
        }
    }

    private static void createFilesFromSortedList(List<MyFile> myFiles, File folderForCopy, File folderForPaste) {
        int i = 1;
        for (MyFile myFile : myFiles) {
            String fileForCopyNameWithoutType = myFile.name().substring(0, myFile.name().lastIndexOf("."));
            String newFileNameForPaste = String.valueOf(i).concat(myFile.name().replace(fileForCopyNameWithoutType, ""));
            File fileForCopy = new File(folderForCopy + "/" + myFile.name());
            File fileForPaste = new File(folderForPaste + "/" + newFileNameForPaste);
            i++;
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileForCopy));
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileForPaste))) {
                while (bufferedInputStream.available() > 0) {
                    bufferedOutputStream.write(bufferedInputStream.read());
                }
                System.out.println(counter.addAndGet(1) + " файлов скопировано");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

record MyFile(String name, LocalDateTime creationTime) {
}