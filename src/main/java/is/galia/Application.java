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

package is.galia;

import ch.qos.logback.classic.LoggerContext;
import is.galia.async.ThreadPool;
import is.galia.async.VirtualThreadPool;
import is.galia.cache.CacheFactory;
import is.galia.cache.CacheWorkerRunner;
import is.galia.config.Configuration;
import is.galia.config.ConfigurationFileWatcher;
import is.galia.config.Key;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import is.galia.source.Source;
import is.galia.source.SourceFactory;
import is.galia.util.MavenUtils;
import is.galia.util.SoftwareVersion;
import is.galia.util.StringUtils;
import is.galia.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

/**
 * Main application class.
 */
public final class Application {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Application.class);

    /**
     * This should match the project name in pom.xml, and must also match
     * the product name in the BCS artifact repository.
     */
    private static final String DEFAULT_NAME    = "galia";
    private static final String DEFAULT_VERSION = "0.0";

    /**
     * Can be set to {@link #DEVELOPMENT_ENVIRONMENT}, {@link
     * #TEST_ENVIRONMENT}, or {@code null}.
     */
    public static final String ENVIRONMENT_VM_ARGUMENT = "is.galia.env";
    /** Possible value for {@link #ENVIRONMENT_VM_ARGUMENT}. */
    public static final String DEVELOPMENT_ENVIRONMENT = "development";
    /** Possible value for {@link #ENVIRONMENT_VM_ARGUMENT}. */
    public static final String TEST_ENVIRONMENT        = "test";

    private static ApplicationServer appServer;
    private static SoftwareVersion cachedAppVersion, cachedSpecVersion;

    static {
        // Suppress a Dock icon and annoying Space transition in full-screen
        // mode in macOS.
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * @return Application web server instance.
     */
    public static synchronized ApplicationServer getAppServer() {
        if (appServer == null) {
            appServer = new ApplicationServer(Configuration.forApplication());
        }
        return appServer;
    }

    /**
     * Returns the path to the application JAR file. When not running from a
     * JAR, this will be the path to {@code target/classes}.
     *
     * @return Path to the JAR file.
     */
    public static Path getJARFile() {
        ProtectionDomain protectionDomain =
                ApplicationServer.class.getProtectionDomain();
        URL location = protectionDomain.getCodeSource().getLocation();
        File file    = new File(location.getFile());
        return file.toPath();
    }

    /**
     * Returns the application title from {@literal MANIFEST.MF}, or some other
     * string if not running from a JAR.
     *
     * @return The application name.
     */
    public static String getName() {
        Package myPackage = Application.class.getPackage();
        String name       = myPackage.getImplementationTitle();
        return (name != null) ? name : DEFAULT_NAME;
    }

    /**
     * Returns the application specification (API) version from {@literal
     * MANIFEST.MF}, or the version from {@literal pom.xml} if not running from
     * a JAR.
     *
     * @return The application specification version.
     */
    public static synchronized SoftwareVersion getSpecificationVersion() {
        if (cachedSpecVersion == null) {
            String version = readSpecificationVersionFromJARManifest();
            if (version == null) {
                try {
                    version = MavenUtils.readSpecificationVersionFromPOM();
                } catch (Exception e) {
                    LOGGER.warn("getSpecificationVersion(): {}",
                            e.getMessage());
                    version = DEFAULT_VERSION;
                }
            }
            cachedSpecVersion = SoftwareVersion.parse(version);
        }
        return cachedSpecVersion;
    }

    /**
     * Returns the path to the temporary directory used by the application. If
     * it does not exist, it will be created.
     *
     * @return Path to the effective application temporary directory.
     */
    public static Path getTempDir() {
        final Configuration config = Configuration.forApplication();
        final String pathStr       = config.getString(Key.TEMP_PATHNAME, "");
        if (!pathStr.isEmpty()) {
            Path dir = Paths.get(pathStr);
            try {
                Files.createDirectories(dir);
                return dir;
            } catch (FileAlreadyExistsException ignore) {
                // This is fine.
            } catch (IOException e) {
                LOGGER.warn("getTempDir(): {} (falling back to java.io.tmpdir) ",
                        e.getMessage());
            }
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Returns the application version, a.k.a. implementation version, from
     * {@literal MANIFEST.MF}, or the version from {@literal pom.xml} if not
     * running from a JAR.
     *
     * @return The application version.
     */
    public static synchronized SoftwareVersion getVersion() {
        if (cachedAppVersion == null) {
            String version = readVersionFromJARManifest();
            if (version == null) {
                try {
                    version = MavenUtils.readVersionFromPOM();
                } catch (Exception e) {
                    LOGGER.warn("getVersion(): {}", e.getMessage());
                    version = DEFAULT_VERSION;
                }
            }
            cachedAppVersion = SoftwareVersion.parse(version);
        }
        return cachedAppVersion;
    }

    /**
     * Used when running from a JAR.
     */
    private static String readSpecificationVersionFromJARManifest() {
        Package myPackage = Application.class.getPackage();
        return myPackage.getSpecificationVersion();
    }

    /**
     * Used when running from a JAR.
     */
    private static String readVersionFromJARManifest() {
        Package myPackage = Application.class.getPackage();
        return myPackage.getImplementationVersion();
    }

    /**
     * @return Whether the application is running in development mode.
     * @see #ENVIRONMENT_VM_ARGUMENT
     */
    public static boolean isDeveloping() {
        return DEVELOPMENT_ENVIRONMENT.equals(System.getProperty(ENVIRONMENT_VM_ARGUMENT));
    }

    /**
     * @return Whether the application is running in test mode.
     * @see #ENVIRONMENT_VM_ARGUMENT
     */
    public static boolean isTesting() {
        return TEST_ENVIRONMENT.equals(System.getProperty(ENVIRONMENT_VM_ARGUMENT));
    }

    /**
     * Handles the command-line arguments and starts the web server.
     *
     * @param args Command-line arguments. See {@link
     *             CommandLineArgumentHandler}.
     */
    public static void main(String... args) {
        try {
            new CommandLineArgumentHandler().handle(args);
            LOGGER.debug("Notifying all plugins of application start...");
            PluginManager.getPlugins().forEach(Plugin::onApplicationStart);
            initializeInServerMode();
        } catch (CommandLineArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            CommandLineArgumentHandler.printUsage();
            SystemUtils.exit(-1);
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            SystemUtils.exit(-1);
        }
    }

    private static void initializeInServerMode() throws Exception {
        logSystemInfo();
        // Start the application web server.
        getAppServer().start();
        // Initialize Image I/O.
        ImageIO.scanForPlugins();
        ImageIO.setUseCache(false);
        // Start the configuration file watcher.
        ConfigurationFileWatcher.startWatching();
        // Start the cache worker, if necessary.
        if (Configuration.forApplication()
                .getBoolean(Key.CACHE_WORKER_ENABLED, false)) {
            CacheWorkerRunner.getInstance().start();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("\u23F9\uFE0F Shutting down...");
            try {
                LOGGER.debug("Stopping the application server...");
                getAppServer().stop();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            // Tell all plugins we're shutting down.
            LOGGER.debug("Notifying all plugins of application stop...");
            PluginManager.getPlugins().forEach(Plugin::onApplicationStart);
            // Stop the cache worker runner.
            LOGGER.debug("Stopping the cache worker...");
            CacheWorkerRunner.getInstance().stop();
            // Stop the configuration file watcher.
            LOGGER.debug("Stopping the configuration file watcher...");
            ConfigurationFileWatcher.stopWatching();
            // Shut down all caches.
            LOGGER.debug("Shutting down all caches...");
            CacheFactory.shutdownCaches();
            // Shut down all sources.
            LOGGER.debug("Shutting down all sources...");
            SourceFactory.getAllSources().forEach(Source::shutdown);
            // Shut down the application platform thread pool.
            LOGGER.debug("Shutting down the application platform thread pool...");
            ThreadPool.getInstance().shutdown();
            // Shut down the application virtual thread pool.
            LOGGER.debug("Shutting down the application virtual thread pool...");
            VirtualThreadPool.getInstance().shutdown();
            // Shut down the logger.
            LOGGER.debug("Stopping the logger...");
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        }));
    }

    private static void logSystemInfo() {
        final Runtime runtime = Runtime.getRuntime();

        LOGGER.info(System.getProperty("java.vendor") + " " +
                System.getProperty("java.vm.name") + " " +
                System.getProperty("java.version") + " / " +
                System.getProperty("java.vm.info"));
        LOGGER.info("{} available processor cores",
                runtime.availableProcessors());
        LOGGER.info("Heap total: {}; max: {}",
                StringUtils.fromByteSize(runtime.totalMemory()),
                StringUtils.fromByteSize(runtime.maxMemory()));
        LOGGER.info("Java home: {}",
                System.getProperty("java.home"));
        LOGGER.info("Java library path: {}",
                System.getProperty("java.library.path"));
        LOGGER.info("Effective temp directory: {}",
                Application.getTempDir());
        LOGGER.info("License: PolyForm Noncommercial 1.0 (unless another was obtained)");
        LOGGER.info("See the file LICENSE.txt for detailed license terms.");
        LOGGER.info("\u25B6\uFE0F Starting {} {}",
                Application.getName(),
                Application.getVersion());
    }

    private Application() {}

}
