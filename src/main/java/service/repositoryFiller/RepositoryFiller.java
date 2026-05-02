package service.repositoryFiller;

import entity.MyFile;
import repository.FileRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RepositoryFiller {
    private final FileRepository fileRepository = FileRepository.getInstance();

    protected abstract boolean formatIsCorrect(File file);

    protected abstract LocalDateTime getCreationDateTimeFromMetadata(File file);

    public final void fillRepositoryBySourceFolder(File sourceFolder) {
        if (sourceFolder.listFiles() == null) {
            System.out.println("""
                    ----------------------------------------------------------
                    Введенной директории (источника копирования) не существует
                    ----------------------------------------------------------""");
            return;
        }

        for (File file : Objects.requireNonNull(sourceFolder.listFiles())) {
            if (file.isDirectory()) {
                fillRepositoryBySourceFolder(file);
            } else {
                addFileToRepository(file);
            }
        }
    }

    private void addFileToRepository(File file) {
        if (!formatIsCorrect(file)) {
            return;
        }

        LocalDateTime creationDateTime = getCreationDateTimeFromMetadata(file);

        if (creationDateTime == null) {
            creationDateTime = getLastModified(file);
        }

        if (creationDateTime == null) {
            fileRepository.getUnsortedFiles().add(file);
            System.out.printf("У файла %s отсутствует дата создания. " +
                            "Будет перемещен в дирректорию unsortedFiles\n",
                    file.getAbsolutePath());
            return;
        }

        if (!fileRepository.getFilesByYear().containsKey(String.valueOf(creationDateTime.getYear()))) {
            fileRepository.getFilesByYear().put(String.valueOf(creationDateTime.getYear()),
                    ConcurrentHashMap.newKeySet());
        }

        fileRepository.getFilesByYear().get(String.valueOf(creationDateTime.getYear()))
                .add(MyFile.builder()
                        .path(file)
                        .creationDateTime(creationDateTime)
                        .size(file.length())
                        .build()
                );

        System.out.printf("Файл %s добавлен в перечень для сортировки\n", file.getAbsolutePath());
    }

    private LocalDateTime getLastModified(File file) {
        try {
            return Files.getLastModifiedTime(
                            file.toPath())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}