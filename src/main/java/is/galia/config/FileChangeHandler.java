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

import is.galia.util.FilesystemWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

class FileChangeHandler implements FilesystemWatcher.Callback {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileChangeHandler.class);

    @Override
    public void created(Path path) {
        handle(path);
    }

    @Override
    public void deleted(Path path) {}

    @Override
    public void modified(Path path) {
        handle(path);
    }

    private void handle(Path path) {
        final Configuration config = Configuration.forApplication();
        ((ConfigurationProvider) config).getWrappedConfigurations()
                .stream()
                .filter(c -> c.getFile().isPresent())
                .forEach(c -> {
                    if (path.equals(c.getFile().get())) {
                        reload(c);
                    }
                });
    }

    private void reload(Configuration config) {
        try {
            LOGGER.info("Configuration file changed; reloading");
            config.reload();
        } catch (FileNotFoundException | NoSuchFileException e) {
            LOGGER.error("reload(): file not found: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("reload(): {}", e.getMessage());
        }
    }

}
