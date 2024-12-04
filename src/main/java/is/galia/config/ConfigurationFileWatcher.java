/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.config;

import is.galia.async.VirtualThreadPool;
import is.galia.util.FilesystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches the configuration file (if available) for changes.
 */
public final class ConfigurationFileWatcher {

    /**
     * Listens for changes to a configuration file and reloads it when it has
     * changed.
     */
    private static class FileChangeHandlerRunner implements Runnable {

        private static final Logger LOGGER =
                LoggerFactory.getLogger(FileChangeHandlerRunner.class);

        private final Path file;
        private FilesystemWatcher filesystemWatcher;

        FileChangeHandlerRunner(Path file) {
            if (file == null) {
                throw new NullPointerException("Null file argument");
            }
            this.file = file;
        }

        @Override
        public void run() {
            try {
                Path path = file.getParent();
                filesystemWatcher = new FilesystemWatcher(path, new FileChangeHandler());
                filesystemWatcher.start();
            } catch (IOException e) {
                LOGGER.error("run(): {}", e.getMessage());
            }
        }

        void stop() {
            if (filesystemWatcher != null) {
                filesystemWatcher.stop();
            }
        }

    }

    private static final Set<FileChangeHandlerRunner> CHANGE_HANDLERS =
            ConcurrentHashMap.newKeySet();

    public static void startWatching() {
        final Configuration config = Configuration.forApplication();
        if (config instanceof ConfigurationProvider) {
            ((ConfigurationProvider) config).getWrappedConfigurations()
                    .stream()
                    .filter(c -> c.getFile().isPresent())
                    .map(c -> c.getFile().get())
                    .forEach(ConfigurationFileWatcher::startWatching);
        } else if (config.getFile().isPresent()) {
            startWatching(config.getFile().get());
        }
    }

    private static void startWatching(Path file) {
        FileChangeHandlerRunner runner = new FileChangeHandlerRunner(file);
        CHANGE_HANDLERS.add(runner);
        VirtualThreadPool.getInstance().submit(runner);
    }

    public static void stopWatching() {
        CHANGE_HANDLERS.forEach(FileChangeHandlerRunner::stop);
        CHANGE_HANDLERS.clear();
    }

    private ConfigurationFileWatcher() {}

}
