package com.patex.service;

import com.patex.entities.ZUser;
import com.patex.utils.ExecutorCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
@ConditionalOnExpression("!'${bulkUploadDir}'.isEmpty()")
public class DirWatcherService {
    private static final Logger log = LoggerFactory.getLogger(DirWatcherService.class);

    private final Path directoryPath;
    private final BookService bookService;
    private final ZUserService zUserService;
    private final Executor executor;

    private volatile boolean running = false;

    @Autowired
    public DirWatcherService(@Value("${bulkUploadDir}") String path,
                             BookService bookService, ZUserService zUserService,
                             ExecutorCreator executorCreator) {
        this(FileSystems.getDefault().getPath(path), bookService, zUserService,
                Executors.newSingleThreadExecutor(executorCreator.createThreadFactory("DirWatcherService", log)));
    }

    public DirWatcherService(Path directoryPath, BookService bookService,
                             ZUserService zUserService, Executor executor) {
        this.directoryPath = directoryPath;
        this.bookService = bookService;
        this.zUserService = zUserService;
        this.executor = executor;
    }

    @PostConstruct
    public void setUp() {
        Optional<ZUser> user = getAdminUser();
        user.ifPresent(zUser -> run());
    }

    private synchronized void run() {
        if (!running) {
            running = true;
            executor.execute(this::initStart);
            executor.execute(this::watch);
        }
    }

    @EventListener
    public void newUserCreated(UserCreationEvent event) {
        if (event.isAdmin()) {
            run();
        }
    }

    private void watch() {
        try {
            WatchService watchService = directoryPath.getFileSystem().newWatchService();
            directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            while (running) {
                WatchKey watchKey = watchService.take();
                Optional<ZUser> user = getAdminUser();
                assert user.isPresent();
                watchKey.pollEvents().stream().
                        filter(e -> StandardWatchEventKinds.ENTRY_CREATE.equals(e.kind())).
                        map(e -> (Path) e.context()).
                        map(directoryPath::resolve).
                        map(Path::toFile).
                        filter(File::exists).
                        forEach(file -> processFile(file, user.get()));
            }
        } catch (InterruptedException e) {
            log.error("Thread got interrupted:" + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void initStart() {
        Optional<ZUser> user = getAdminUser();
        user.ifPresent(zUser -> {
            File dir = directoryPath.toFile();
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                processFile(file, zUser);
            }
        });
    }

    private Optional<ZUser> getAdminUser() {
        return zUserService.getByRole(ZUserService.ADMIN_AUTHORITY).stream().findFirst();
    }

    private void processFile(File file, ZUser adminUser) {
        try (FileInputStream fis = new FileInputStream(file)) {
            bookService.uploadBook(file.getName(), fis, adminUser);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
