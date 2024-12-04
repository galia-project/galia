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

package is.galia.codec;

import is.galia.codec.bmp.BMPDecoder;
import is.galia.codec.gif.GIFDecoder;
import is.galia.codec.jpeg.JPEGDecoder;
import is.galia.codec.png.PNGDecoder;
import is.galia.codec.tiff.TIFFDecoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used for obtaining {@link Decoder} instances.
 */
public final class DecoderFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DecoderFactory.class);

    private static final Set<Class<? extends Decoder>> BUILT_IN_DECODERS = Set.of(
            BMPDecoder.class, GIFDecoder.class, JPEGDecoder.class,
            PNGDecoder.class, TIFFDecoder.class);

    /**
     * @return Set of instances of all registered decoders.
     */
    public static Set<Decoder> getAllDecoders() {
        final Set<Decoder> decoders = new HashSet<>();
        for (Class<?> class_ : BUILT_IN_DECODERS) {
            try {
                decoders.add((Decoder) Arrays.stream(
                        class_.getConstructors()).findFirst().get().newInstance());
            } catch (Exception e) {
                LOGGER.error("getAllDecoders(): failed to instantiate {}: {}",
                        class_, e.getMessage());
            }
        }
        decoders.addAll(getPluginDecoders());
        return Set.copyOf(decoders); // immutable
    }

    /**
     * @return All formats supported by any {@link Decoder}.
     */
    public static Set<Format> getAllSupportedFormats() {
        return getAllDecoders().stream()
                .map(Decoder::getSupportedFormats)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * @return Set of instances of all decoders provided by plugins. The
     *         instances have not been {@link Plugin#initializePlugin()
     *         initialized}.
     */
    public static Set<Decoder> getPluginDecoders() {
        return PluginManager.getPlugins()
                .stream()
                .filter(Decoder.class::isInstance)
                .map(p -> (Decoder) p)
                .collect(Collectors.toSet());
    }

    /**
     * <p>Returns a new instance capable of decoding the given format.</p>
     *
     * <ul>
     *     <li>If a single {@link Decoder} implementation exists that supports
     *     the format:
     *         <ul>
     *             <li>If there is no configuration preference, or if there is
     *             a matching configuration preference, an instance of that
     *             that implementation is returned.</li>
     *             <li>Otherwise, a {@link DecoderConfigurationException} is
     *             thrown.</li>
     *         </ul>
     *     </li>
     *     <li>If multiple {@link Decoder} implementations exist that support
     *     the format, the {@link Key#DECODER_FORMATS} key in the application
     *     configuration is consulted to choose one.
     *         <ul>
     *             <li>If it is not set, a {@link SourceFormatException} is
     *             thrown.</li>
     *             <li>If it maps to a valid implementation, an instance of
     *             that implementation is returned.</li>
     *             <li>If it maps to an invalid implementation, a {@link
     *             DecoderConfigurationException} is thrown.</li>
     *         </ul>
     *     </li>
     *     <li>If no {@link Decoder} implementation exists that supports the
     *     format, a {@link SourceFormatException} is thrown.</li>
     * </ul>
     *
     * @param format Format for which to retrieve an instance.
     * @param arena  Will be assigned to the instance before {@link
     *               Plugin#initializePlugin()} is called.
     * @return Instance capable of decoding the given format.
     * @throws SourceFormatException if the given format is not supported by
     *         any decoder.
     * @throws DecoderConfigurationException if the configuration contains an
     *         invalid implementation name for the given format.
     */
    public static Decoder newDecoder(Format format, Arena arena)
            throws SourceFormatException, DecoderConfigurationException {
        final Set<Decoder> candidates = getAllDecodersSupportingFormat(format);

        if (candidates.isEmpty()) {
            throw new SourceFormatException(format);
        } else if (candidates.size() == 1) {
            Decoder decoder = null;
            String name = getPreferredDecoderName(format);
            if (name != null && !name.isBlank()) {
                decoder = newDecoder(name);
                if (decoder == null) {
                    throw new DecoderConfigurationException("The " +
                            Key.DECODER_FORMATS +
                            " configuration key contains an invalid " +
                            "implementation name for format " + format +
                            " (is this a plugin that is installed and compatible?)");
                }
            }
            if (decoder == null) {
                decoder = candidates.iterator().next();
            }
            initialize(decoder, arena);
            return decoder;
        } else {
            String name = getPreferredDecoderName(format);
            if (name != null && !name.isBlank()) {
                Decoder decoder = newDecoder(name);
                if (decoder == null) {
                    throw new DecoderConfigurationException("The " +
                            Key.DECODER_FORMATS +
                            " configuration key contains an invalid " +
                            "implementation name for format " + format +
                            " (is this a plugin that is installed and compatible?)");
                }
                initialize(decoder, arena);
                return decoder;
            } else {
                throw new SourceFormatException(
                        Key.ENCODER_FORMATS + " does not contain an entry for " +
                                "format: " + format);
            }
        }
    }

    private static String getPreferredDecoderName(Format format) {
        final Configuration config = Configuration.forApplication();
        @SuppressWarnings("unchecked")
        final Map<String,String> preferenceMap =
                (Map<String,String>) config.getProperty(Key.DECODER_FORMATS);
        if (preferenceMap != null && preferenceMap.containsKey(format.key())) {
            return preferenceMap.get(format.key());
        }
        return null;
    }

    private static Set<Decoder> getAllDecodersSupportingFormat(Format format) {
        final Set<Decoder> candidates = new HashSet<>();
        for (Decoder decoder : getAllDecoders()) {
            if (decoder.getSupportedFormats().contains(format)) {
                candidates.add(decoder);
            }
        }
        return candidates;
    }

    private static void initialize(Decoder decoder, Arena arena) {
        decoder.setArena(arena);
        if (decoder instanceof Plugin plugin) {
            plugin.initializePlugin();
        }
    }

    /**
     * @param name Decoder name. For {@link Plugin}s, this is the {@link
     *             Plugin#getPluginName() plugin name}. For built-in
     *             implementations, it is the simple class name.
     * @return Instance with the given name.
     */
    private static Decoder newDecoder(String name) {
        for (Decoder decoder : getAllDecoders()) {
            String decoderName = (decoder instanceof Plugin) ?
                    ((Plugin) decoder).getPluginName() :
                    decoder.getClass().getSimpleName();
            if (decoderName.equals(name)) {
                return decoder;
            }
        }
        return null;
    }

    private DecoderFactory() {}

}
