package service.creator;

import entity.MyFile;
import repository.FileRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class FileCreator {

    private static volatile FileCreator instance;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final AtomicLong fileCounter = new AtomicLong();

    public void createFilesFromRepositoryInFolderForSortMedia(FileRepository fileRepository, File folderForSortMedia) {
        for (Map.Entry<String, Set<MyFile>> entry : fileRepository.getFilesByYear().entrySet()) {
            executorService.submit(() -> createFilesByMapEntry(entry, folderForSortMedia));
        }
        executorService.shutdown();
    }

    private void createFilesByMapEntry(Map.Entry<String, Set<MyFile>> entry, File folderForSortMedia) {
        int i = 1;

        File folderForPasteByYear = new File(
                folderForSortMedia.getAbsolutePath().concat(File.separator).concat(entry.getKey()));

        if (!folderForPasteByYear.exists()) {
            folderForPasteByYear.mkdirs();
        }

        List<MyFile> myFiles = new ArrayList<>(entry.getValue());
        myFiles.sort(Comparator.comparing(MyFile::getCreationDateTime));

        for (MyFile file : myFiles) {
            File fileForPaste = getFileForPaste(file, i, folderForPasteByYear);
            try {
                Files.copy(file.getPath().toPath(), fileForPaste.toPath(), StandardCopyOption.REPLACE_EXISTING);
                FileTime lastModifiedTime = Files.getLastModifiedTime(file.getPath().toPath());
                Files.setLastModifiedTime(fileForPaste.toPath(), lastModifiedTime);
                System.out.printf("%s файлов скопировано\n",
                        fileCounter.addAndGet(1));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            i++;
        }
    }

    private File getFileForPaste(MyFile file, int i, File folderForPasteByYear) {
        String newFileNameForPaste;

        if (file.getPath().getName().contains(".")) {
            String fileType = file.getPath().getName().substring(file.getPath().getName().lastIndexOf("."));
            newFileNameForPaste = String.valueOf(i).concat(fileType);
        } else {
            newFileNameForPaste = String.valueOf(i);
        }

        return new File(folderForPasteByYear + File.separator + newFileNameForPaste);
    }

    public void copyUnsortedFiles(FileRepository fileRepository, File folderForSortMedia) {
        if (fileRepository.getUnsortedFiles().isEmpty()) {
            return;
        }

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
    }

    private FileCreator() {
    }

    public static FileCreator getInstance() {
        if (instance == null) {
            synchronized (FileCreator.class) {
                if (instance == null) {
                    instance = new FileCreator();
                }
            }
        }
        return instance;
    }
}