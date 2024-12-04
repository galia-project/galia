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

package is.galia.source;

import is.galia.image.Format;
import is.galia.image.StatResult;
import is.galia.plugin.Plugin;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Mock implementation. There is a file for this class in {@literal
 * src/test/resources/META-INF/services}.
 */
public class MockSourcePlugin extends AbstractSource implements Plugin, Source {

    public static boolean isApplicationStarted, isApplicationStopped;

    public boolean isInitialized;

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of();
    }

    @Override
    public String getPluginName() {
        return MockSourcePlugin.class.getSimpleName();
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
    //region Source methods

    @Override
    public Iterator<Format> getFormatIterator() {
        return null;
    }

    @Override
    public ImageInputStream newInputStream() throws IOException {
        return null;
    }

    @Override
    public StatResult stat() throws IOException {
        return null;
    }

}
