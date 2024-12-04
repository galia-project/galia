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

import is.galia.cache.InfoCache;
import is.galia.cache.VariantCache;
import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.codec.Encoder;
import is.galia.codec.EncoderFactory;
import is.galia.config.Configuration;
import is.galia.config.ConfigurationFactory;
import is.galia.config.ConfigurationTester;
import is.galia.delegate.Delegate;
import is.galia.image.Format;
import is.galia.logging.CustomConfigurator;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import is.galia.plugin.PluginNotInstalledException;
import is.galia.plugin.VersionNotFoundException;
import is.galia.resource.Resource;
import is.galia.source.Source;
import is.galia.util.FileUtils;
import is.galia.util.SystemUtils;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class CommandLineArgumentHandler {

    static final String CONFIG_ARGUMENT          = "config";
    static final String CONFIGTEST_ARGUMENT      = "configtest";
    static final String INSTALL_PLUGIN_ARGUMENT  = "install-plugin";
    static final String LIST_FONTS_ARGUMENT      = "list-fonts";
    static final String LIST_FORMATS_ARGUMENT    = "list-formats";
    static final String LIST_PLUGINS_ARGUMENT    = "list-plugins";
    static final String REMOVE_PLUGIN_ARGUMENT   = "remove-plugin";
    static final String UPDATE_PLUGIN_ARGUMENT   = "update-plugin";
    static final String UPDATE_PLUGINS_ARGUMENT  = "update-plugins";
    static final String USAGE_ARGUMENT           = "help";
    static final String VERSION_ARGUMENT         = "version";

    private static final String DIVIDER = "-".repeat(76);
    private static final String NEWLINE = System.lineSeparator();

    private static final CommandLineArgumentParser argParser = new CommandLineArgumentParser(
            new CommandLineArgument(
                    CONFIG_ARGUMENT,
                    CommandLineArgument.Type.STRING,
                    true,
                    false,
                    "Configuration file"),
            new CommandLineArgument(
                    CONFIGTEST_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    false,
                    "Test the configuration"),
            new CommandLineArgument(
                    INSTALL_PLUGIN_ARGUMENT,
                    CommandLineArgument.Type.STRING,
                    false,
                    true,
                    "Install a plugin"),
            new CommandLineArgument(
                    LIST_FORMATS_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "List supported image formats"),
            new CommandLineArgument(
                    LIST_PLUGINS_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "List plugins"),
            new CommandLineArgument(
                    REMOVE_PLUGIN_ARGUMENT,
                    CommandLineArgument.Type.STRING,
                    false,
                    true,
                    "Remove a plugin"),
            new CommandLineArgument(
                    LIST_FONTS_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "List fonts"),
            new CommandLineArgument(
                    UPDATE_PLUGIN_ARGUMENT,
                    CommandLineArgument.Type.STRING,
                    false,
                    true,
                    "Update a plugin"),
            new CommandLineArgument(
                    UPDATE_PLUGINS_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "Update all plugins"),
            new CommandLineArgument(
                    USAGE_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "Print this message"),
            new CommandLineArgument(
                    VERSION_ARGUMENT,
                    CommandLineArgument.Type.FLAG_ONLY,
                    false,
                    true,
                    "Print version information"));

    void handle(String... args) throws IOException {
        final Map<String,Object> parsedArgs = argParser.parse(args);
        if (parsedArgs.containsKey(USAGE_ARGUMENT)) {
            printUsage();
            SystemUtils.exit(0);
            return;
        } else if (parsedArgs.containsKey(VERSION_ARGUMENT)) {
            printVersion();
            SystemUtils.exit(0);
            return;
        } else if (parsedArgs.containsKey(LIST_FONTS_ARGUMENT)) {
            printFonts();
            SystemUtils.exit(0);
            return;
        } else if (parsedArgs.containsKey(LIST_FORMATS_ARGUMENT)) {
            printFormats();
            SystemUtils.exit(0);
            return;
        } else if (parsedArgs.containsKey(LIST_PLUGINS_ARGUMENT)) {
            printPlugins();
            if (!SystemUtils.exitRequested()) {
                SystemUtils.exit(0);
            }
            return;
        } else if (parsedArgs.containsKey(INSTALL_PLUGIN_ARGUMENT)) {
            String plugin = (String) parsedArgs.get(INSTALL_PLUGIN_ARGUMENT);
            try {
                PluginManager.getInstaller().installPlugin(plugin);
                SystemUtils.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                SystemUtils.exit(-1);
            }
            return;
        } else if (parsedArgs.containsKey(REMOVE_PLUGIN_ARGUMENT)) {
            String plugin = (String) parsedArgs.get(REMOVE_PLUGIN_ARGUMENT);
            try {
                PluginManager.getRemover().backupPlugin(plugin);
                SystemUtils.exit(0);
            } catch (PluginNotInstalledException e) {
                System.err.println("Plugin not installed: " + plugin);
                SystemUtils.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
                SystemUtils.exit(-1);
            }
            return;
        } else if (parsedArgs.containsKey(UPDATE_PLUGIN_ARGUMENT)) {
            String plugin = (String) parsedArgs.get(UPDATE_PLUGIN_ARGUMENT);
            try {
                PluginManager.getUpdater().updatePlugin(plugin);
                SystemUtils.exit(0);
            } catch (VersionNotFoundException e) {
                System.out.println(e.getMessage());
                SystemUtils.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
                SystemUtils.exit(-1);
            }
            return;
        } else if (parsedArgs.containsKey(UPDATE_PLUGINS_ARGUMENT)) {
            try {
                PluginManager.getUpdater().updatePlugins();
                SystemUtils.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                SystemUtils.exit(-1);
            }
            return;
        }
        { // Configuration
            try {
                String configArg = (String) parsedArgs.get(CONFIG_ARGUMENT);
                Path path = FileUtils.locate(configArg);
                Configuration config = ConfigurationFactory.newProviderFromPath(path);
                ConfigurationFactory.setAppInstance(config);
                if (parsedArgs.containsKey(CONFIGTEST_ARGUMENT)) {
                    printConfigTestReport();
                    SystemUtils.exit(0);
                    return;
                }
                CustomConfigurator.resetLoggerContext();
            } catch (IllegalArgumentException e) {
                throw new CommandLineArgumentException(
                        "Missing or invalid " + CONFIG_ARGUMENT + " argument.");
            }
        }
    }

    private static void printConfigTestReport() {
        final ConfigurationTester tester = new ConfigurationTester(
                Configuration.forApplication());
        final List<String> unrecognizedKeys = tester.getUnrecognizedKeys();
        final List<String> missingKeys = tester.getMissingKeys();
        if (missingKeys.isEmpty() && unrecognizedKeys.isEmpty()) {
            System.out.println("Configuration OK");
        } else {
            if (!missingKeys.isEmpty()) {
                System.out.println(DIVIDER);
                System.out.println("MISSING KEYS");
                System.out.println(DIVIDER);
                missingKeys.forEach(System.out::println);
                System.out.println();
            }
            if (!unrecognizedKeys.isEmpty()) {
                System.out.println(DIVIDER);
                System.out.println("UNRECOGNIZED KEYS");
                System.out.println(DIVIDER);
                unrecognizedKeys.forEach(System.out::println);
                System.out.println();
            }
        }
    }

    private static void printFonts() {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String family : ge.getAvailableFontFamilyNames()) {
            System.out.println(family);
        }
    }

    private static void printFormats() {
        { // Formats supported by Decoders
            Set<Decoder> decoders = DecoderFactory.getAllDecoders();
            List<Format> formats = decoders.stream()
                    .map(Decoder::getSupportedFormats)
                    .flatMap(Collection::stream)
                    .toList();
            printCodecSection(Decoder.class, formats);
        }
        { // Formats supported by Encoders
            Set<Encoder> encoders = EncoderFactory.getAllEncoders();
            List<Format> formats = encoders.stream()
                    .map(Encoder::getSupportedFormats)
                    .flatMap(Collection::stream)
                    .toList();
            printCodecSection(Encoder.class, formats);
        }
    }

    private static void printPlugins() {
        Set<Plugin> plugins = PluginManager.getPlugins();
        try {
            FileUtils.checkWritableDirectory(PluginManager.getPluginsDir());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            SystemUtils.exit(-1);
            return;
        }
        { // Sources
            Set<Plugin> sources = plugins.stream()
                    .filter(Source.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("SOURCES", sources);
        }
        { // Variant caches
            Set<Plugin> caches = plugins.stream()
                    .filter(VariantCache.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("VARIANT CACHES", caches);
        }
        { // Info caches
            Set<Plugin> caches = plugins.stream()
                    .filter(InfoCache.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("INFO CACHES", caches);
        }
        { // Decoders
            Set<Plugin> decoders = plugins.stream()
                    .filter(Decoder.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("DECODERS", decoders);
        }
        { // Encoders
            Set<Plugin> encoders = plugins.stream()
                    .filter(Encoder.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("ENCODERS", encoders);
        }
        { // Endpoints
            Set<Plugin> endpoints = plugins.stream()
                    .filter(Resource.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("ENDPOINTS", endpoints);
        }
        { // Delegate
            Set<Plugin> delegates = plugins.stream()
                    .filter(Delegate.class::isInstance)
                    .collect(Collectors.toSet());
            printPluginSection("DELEGATE", delegates);
        }
    }

    private static void printCodecSection(Class<?> codecClass,
                                          List<Format> formats) {
        System.out.println(DIVIDER);
        System.out.println("FORMATS SUPPORTED BY " +
                codecClass.getSimpleName().toUpperCase() + "S");
        System.out.println(DIVIDER);

        final int colPadding = 3;
        final int col1Width = colPadding + formats.stream()
                .mapToInt(a -> a.key().length())
                .max()
                .orElse(0);
        final int col2Width = colPadding + formats.stream()
                .mapToInt(a -> a.name().length())
                .max()
                .orElse(0);
        final int col3Width = colPadding + formats.stream()
                .mapToInt(a -> String.join(", ", a.extensions()).length())
                .max()
                .orElse(0);
        String col1Heading = String.format("%1$-" + (col1Width + 1) + "s", "KEY");
        String col2Heading = String.format("%1$-" + (col2Width + 1) + "s", "NAME");
        String col3Heading = String.format("%1$-" + (col3Width + 1) + "s", "EXTENSIONS");
        String col4Heading = codecClass.getSimpleName().toUpperCase() + "S";

        System.out.println(col1Heading + col2Heading + col3Heading + col4Heading);
        formats.stream()
                .distinct()
                .sorted(Comparator.comparing(Format::name))
                .forEach(format -> printFormat(
                        format, codecClass, col1Width, col2Width, col3Width));
        System.out.println();
    }

    private static void printFormat(Format format,
                                    Class<?> codecClass,
                                    int col1Width,
                                    int col2Width,
                                    int col3Width) {
        Stream<?> coders;
        if (codecClass == Encoder.class) {
            coders = EncoderFactory.getAllEncoders().stream()
                    .filter(encoder -> encoder.getSupportedFormats().contains(format));
        } else {
            coders = DecoderFactory.getAllDecoders().stream()
                    .filter(decoder -> decoder.getSupportedFormats().contains(format));
        }
        String key        = String.format("%1$-" + (col1Width + 1) + "s",
                format.key());
        String name       = String.format("%1$-" + (col2Width + 1) + "s",
                format.name());
        String extensions = String.format("%1$-" + (col3Width + 1) + "s",
                String.join(", ", format.extensions()));
        String codersStr  = coders
                .map(c -> (c instanceof Plugin) ?
                        ((Plugin) c).getPluginName() : c.getClass().getSimpleName())
                .collect(Collectors.joining(", "));
        System.out.println(key + name + extensions + codersStr);
    }

    private static void printPluginSection(String title,
                                           Set<Plugin> plugins) {
        System.out.println(DIVIDER);
        System.out.println(title);
        if (!plugins.isEmpty()) {
            plugins.stream()
                    .sorted(Comparator.comparing(Plugin::getPluginName))
                    .forEach((p) -> System.out.println(
                            "* " + p.getPluginName() + " " +
                                    p.getPluginVersion() +
                                    " (spec version: " + p.getPluginSpecificationVersion() + ")"));
        } else {
            System.out.println("(none)");
        }
        System.out.println();
    }

    /**
     * Prints program usage to {@link System#out}.
     */
    static void printUsage() {
        System.out.println(NEWLINE + argParser.usage());
    }

    /**
     * Prints program usage to {@link System#out}.
     */
    static void printVersion() {
        System.out.println("Application version:   " +
                Application.getVersion());
        System.out.println("Specification version: " +
                Application.getSpecificationVersion());
    }

}
