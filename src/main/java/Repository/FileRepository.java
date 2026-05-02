package Repository;

import Entity.MyFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Singleton class for storing files
public final class FileRepository {
    private static volatile FileRepository instance;
    private final Map<String, List<MyFile>> filesByYear = new ConcurrentHashMap<>();
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

    public Map<String, List<MyFile>> getFilesByYear() {
        return filesByYear;
    }

    public List<File> getUnsortedFiles() {
        return unsortedFiles;
    }
}