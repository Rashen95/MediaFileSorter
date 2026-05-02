package repository;

import entity.MyFile;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class FileRepository {
    private static volatile FileRepository instance;
    private final Map<String, Set<MyFile>> filesByYear = new ConcurrentHashMap<>();
    private final List<File> unsortedFiles = Collections.synchronizedList(new ArrayList<>());

    private FileRepository() {
    }

    public static FileRepository getInstance() {
        if (instance == null) {
            synchronized (FileRepository.class) {
                if (instance == null) {
                    instance = new FileRepository();
                }
            }
        }
        return instance;
    }
}