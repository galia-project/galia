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

import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.plugin.Plugin;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Mock implementation. There is a file for this class in {@literal
 * src/test/resources/META-INF/services}.
 */
public class MockDecoderPlugin2 implements Decoder, Plugin {

    public static boolean isApplicationStarted, isApplicationStopped;

    public boolean isInitialized;

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of();
    }

    @Override
    public String getPluginName() {
        return MockDecoderPlugin2.class.getSimpleName();
    }

    @Override
    public void onApplicationStart() {
        isApplicationStarted = true;
    }

    @Override
    public void onApplicationStop() {
        isApplicationStopped = true;
    }

    @Override
    public void initializePlugin() {
        isInitialized = true;
    }


    //endregion
    //region Decoder methods

    @Override
    public void close() {
    }

    @Override
    public Format detectFormat() throws IOException {
        return Format.UNKNOWN;
    }

    @Override
    public int getNumImages() throws IOException {
        return 0;
    }

    @Override
    public int getNumResolutions() throws IOException {
        return 0;
    }

    @Override
    public Size getSize(int imageIndex) throws IOException {
        return null;
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(MockDecoderPlugin.SUPPORTED_FORMAT);
    }

    @Override
    public Size getTileSize(int imageIndex) throws IOException {
        return null;
    }

    @Override
    public BufferedImage decode(int imageIndex,
                                Region region,
                                double[] scales,
                                ReductionFactor reductionFactor,
                                double[] diffScales,
                                Set<DecoderHint> decoderHints) throws IOException {
        return null;
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        return null;
    }

    @Override
    public void setSource(ImageInputStream inputStream) {
    }

    @Override
    public void setSource(Path imageFile) {
    }

}
