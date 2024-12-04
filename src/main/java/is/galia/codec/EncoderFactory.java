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

import is.galia.codec.gif.GIFEncoder;
import is.galia.codec.jpeg.JPEGEncoder;
import is.galia.codec.png.PNGEncoder;
import is.galia.codec.tiff.TIFFEncoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.operation.Encode;
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

public final class EncoderFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EncoderFactory.class);

    private static final Set<Class<? extends Encoder>> BUILT_IN_ENCODERS = Set.of(
            GIFEncoder.class, JPEGEncoder.class, PNGEncoder.class,
            TIFFEncoder.class);

    /**
     * @return Set of instances of all registered {@link Encoder}s. N.B.: it is
     *         possible that not all of these are enabled!
     * @see #getEnabledEncoders()
     */
    public static Set<Encoder> getAllEncoders() {
        final Set<Encoder> encoders = new HashSet<>();
        for (Class<?> class_ : BUILT_IN_ENCODERS) {
            try {
                encoders.add((Encoder) Arrays.stream(
                        class_.getConstructors()).findFirst().get().newInstance());
            } catch (Exception e) {
                LOGGER.error("getAllDecoders(): failed to instantiate {}: {}",
                        class_, e.getMessage());
            }
        }
        encoders.addAll(getPluginEncoders());
        return Set.copyOf(encoders); // immutable
    }

    /**
     * @return Set of instances of all {@link Encoder}s that are mapped to a
     *         format in the {@link Key#ENCODER_FORMATS} configuration key.
     */
    public static Set<Encoder> getEnabledEncoders() {
        final Configuration config  = Configuration.forApplication();
        final Set<Encoder> encoders = new HashSet<>();
        @SuppressWarnings("unchecked")
        final Map<String,String> preferenceMap =
                (Map<String,String>) config.getProperty(Key.ENCODER_FORMATS);
        for (Map.Entry<String,String> entry : preferenceMap.entrySet()) {
            encoders.add(newEncoder(entry.getValue()));
        }
        return Set.copyOf(encoders); // immutable
    }

    /**
     * @return Set of instances of all encoders provided by plugins. The
     *         plugins have not been {@link Plugin#initializePlugin()
     *         initialized}.
     */
    public static Set<Encoder> getPluginEncoders() {
        return PluginManager.getPlugins()
                .stream()
                .filter(Encoder.class::isInstance)
                .map(p -> (Encoder) p)
                .collect(Collectors.toSet());
    }

    /**
     * @return All formats supported by any {@link Encoder}.
     */
    public static Set<Format> getAllSupportedFormats() {
        return getAllEncoders().stream()
                .map(Encoder::getSupportedFormats)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * <p>Returns a new instance capable of encoding the given format, based
     * on the setting of {@link Key#ENCODER_FORMATS}.</p>
     *
     * <p>N.B.: This behavior differs from that of {@link
     * DecoderFactory#newDecoder(Format, Arena)} in that configuration
     * preferences are required.</p>
     *
     * @return Instance capable of encoding the given format.
     * @throws EncoderConfigurationException if the configuration contains an
     *         invalid implementation name for the given format.
     * @throws VariantFormatException if the given format is not supported by
     *         the chosen implementation.
     */
    public static Encoder newEncoder(Encode encode, Arena arena)
            throws VariantFormatException, EncoderConfigurationException {
        final Format format        = encode.getFormat();
        final Configuration config = Configuration.forApplication();
        @SuppressWarnings("unchecked")
        final Map<String,String> preferenceMap =
                (Map<String,String>) config.getProperty(Key.ENCODER_FORMATS);
        if (preferenceMap != null && preferenceMap.containsKey(format.key())) {
            String name     = preferenceMap.get(format.key());
            Encoder encoder = newEncoder(name);
            if (encoder != null) {
                if (!encoder.getSupportedFormats().contains(format)) {
                    throw new VariantFormatException("The " +
                            Key.ENCODER_FORMATS + " configuration key" +
                            " contains an incompatible implementation name " +
                            "for format " + format);
                }
                encoder.setArena(arena);
                if (encoder instanceof Plugin plugin) {
                    plugin.initializePlugin();
                }
                encoder.setEncode(encode);
                return encoder;
            } else {
                throw new EncoderConfigurationException("The " +
                        Key.ENCODER_FORMATS +
                        " configuration key contains an invalid " +
                        "implementation name for format " + format +
                        " (is this a plugin that is installed and compatible?)");
            }
        } else {
            throw new VariantFormatException(
                    Key.ENCODER_FORMATS + " does not contain an entry for " +
                            "format: " + format);
        }
    }

    /**
     * @param name Encoder name. For {@link Plugin}s, this is the {@link
     *             Plugin#getPluginName()}. For built-in implementations, it is
     *             the simple class name.
     * @return Instance with the given name.
     */
    private static Encoder newEncoder(String name) {
        for (Encoder encoder : getAllEncoders()) {
            String encoderName = (encoder instanceof Plugin) ?
                    ((Plugin) encoder).getPluginName() :
                    encoder.getClass().getSimpleName();
            if (encoderName.equals(name)) {
                return encoder;
            }
        }
        return null;
    }

    private EncoderFactory() {}

}
