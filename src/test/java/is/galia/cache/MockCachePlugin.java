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

package is.galia.cache;

import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.plugin.Plugin;
import is.galia.stream.CompletableOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

/**
 * Mock implementation. There is a file for this class in {@literal
 * src/test/resources/META-INF/services}.
 */
public class MockCachePlugin implements Plugin, VariantCache, InfoCache {

    public static boolean isApplicationStarted, isApplicationStopped;

    public boolean isInitialized;

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of();
    }

    @Override
    public String getPluginName() {
        return MockCachePlugin.class.getSimpleName();
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
    //region Cache methods

    @Override
    public void addObserver(CacheObserver observer) {
    }

    @Override
    public void evict(Identifier identifier) throws IOException {
    }

    @Override
    public void evictInvalid() throws IOException {
    }

    @Override
    public void purge() throws IOException {
    }

    //endregion
    //region VariantCache methods

    @Override
    public void evict(OperationList opList) throws IOException {
    }

    @Override
    public InputStream newVariantImageInputStream(OperationList opList)
            throws IOException {
        return null;
    }

    @Override
    public InputStream newVariantImageInputStream(
            OperationList opList,
            StatResult statResult) throws IOException {
        return null;
    }

    @Override
    public CompletableOutputStream newVariantImageOutputStream(OperationList opList)
            throws IOException {
        return null;
    }

    //endregion
    //region InfoCache methods

    @Override
    public void evictInfos() throws IOException {
    }

    @Override
    public Optional<Info> fetchInfo(Identifier identifier) throws IOException {
        return Optional.empty();
    }

    @Override
    public void put(Identifier identifier, Info info) throws IOException {
    }

    @Override
    public void put(Identifier identifier, String info) throws IOException {
    }

}
