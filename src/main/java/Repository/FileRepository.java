package Repository;

import Entity.MyFile;

import java.io.File;
import java.util.*;

// Singleton class for storing files
public final class FileRepository {
    private static volatile FileRepository instance;
    private final Map<String, List<MyFile>> filesByYear = Collections.synchronizedMap(new HashMap<>());
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