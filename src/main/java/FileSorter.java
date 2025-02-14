import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
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

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите путь к папке, которую хотите копировать и отсортировать " +
                "файлы по дате (путь должен содержать только английские символы или цифры: ");
        String folderPathForCopy = scanner.nextLine();
        folderPathForCopy = folderPathForCopy.replace("\\", File.separator);
        File folderForCopy = new File(folderPathForCopy);

        System.out.print("Введите путь к папке, в которую хотите копировать и отсортировать " +
                "файлы по дате (путь должен содержать только английские символы или цифры: ");
        String folderPathForPaste = scanner.nextLine();
        folderPathForPaste = folderPathForPaste.replace("\\", File.separator)
                .concat(File.separator)
                .concat("Мои отсортированные файлы");
        File folderForPaste = new File(folderPathForPaste);

        if (folderForPaste.exists()) {
            deleteDir(folderForPaste);
        }
        folderForPaste.mkdirs();

        sortAndCopyFiles(folderForCopy, folderForPaste);
        executor.shutdown();
    }

    private static void sortAndCopyFiles(File folderForCopy, File folderForPaste) {
        List<MyFile> myFiles = new ArrayList<>();

        if (folderForCopy.listFiles() == null) {
            System.out.println("""
                    ----------------------------------------------------------
                    Введенной директории (источника копирования) не существует
                    ----------------------------------------------------------""");
            return;
        }

        for (File fileForCopy : Objects.requireNonNull(folderForCopy.listFiles())) {
            if (fileForCopy.isDirectory()) {
                File newFolderForPaste = new File(folderForPaste + File.separator + fileForCopy.getName());
                newFolderForPaste.mkdirs();
                sortAndCopyFiles(fileForCopy, newFolderForPaste);
            } else {
                try {
                    myFiles.add(new MyFile(
                                    fileForCopy.getName(),
                                    Files.getLastModifiedTime(fileForCopy.toPath())
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime()
                            )
                    );
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        myFiles.sort(Comparator.comparing(MyFile::creationTime));

        executor.execute(() -> createFilesFromSortedList(myFiles, folderForCopy, folderForPaste));
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

    private static void createFilesFromSortedList(List<MyFile> myFiles, File folderForCopy, File folderForPaste) {
        int i = 1;
        for (MyFile myFile : myFiles) {
            String newFileNameForPaste;

            if (myFile.name().contains(".")) {
                String fileForCopyNameWithoutType = myFile.name().substring(0, myFile.name().lastIndexOf("."));
                newFileNameForPaste = String.valueOf(i).concat(myFile.name().replace(fileForCopyNameWithoutType, ""));
            } else {
                newFileNameForPaste = String.valueOf(i);
            }

            File fileForCopy = new File(folderForCopy + File.separator + myFile.name());
            File fileForPaste = new File(folderForPaste + File.separator + newFileNameForPaste);
            i++;
            try {
                Files.copy(fileForCopy.toPath(), fileForPaste.toPath(), StandardCopyOption.REPLACE_EXISTING);
                FileTime lastModifiedTime = Files.getLastModifiedTime(fileForCopy.toPath());
                Files.setLastModifiedTime(fileForPaste.toPath(), lastModifiedTime);
                System.out.println(counter.addAndGet(1) + " файлов скопировано");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

record MyFile(String name, LocalDateTime creationTime) {
}