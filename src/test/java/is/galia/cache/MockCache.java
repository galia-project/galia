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
import is.galia.stream.CompletableOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

class MockCache extends AbstractCache implements VariantCache, InfoCache {

    private boolean isCleanUpCalled, isInitializeCalled, isOnCacheWorkerCalled,
            isPurgeInfosCalled, isPurgeInvalidCalled, isShutdownCalled;

    boolean isCleanUpCalled() {
        return isCleanUpCalled;
    }

    boolean isInitializeCalled() {
        return isInitializeCalled;
    }

    boolean isOnCacheWorkerCalled() {
        return isOnCacheWorkerCalled;
    }

    boolean isPurgeInfosCalled() {
        return isPurgeInfosCalled;
    }

    boolean isPurgeInvalidCalled() {
        return isPurgeInvalidCalled;
    }

    boolean isShutdownCalled() {
        return isShutdownCalled;
    }

    //region Cache methods

    @Override
    public void cleanUp() {
        isCleanUpCalled = true;
    }

    @Override
    public void evict(Identifier identifier) {}

    @Override
    public void evictInvalid() {
        isPurgeInvalidCalled = true;
    }

    @Override
    public void initialize() {
        isInitializeCalled = true;
    }

    @Override
    public void onCacheWorker() {
        VariantCache.super.onCacheWorker();
        isOnCacheWorkerCalled = true;
    }

    @Override
    public void purge() {}

    @Override
    public void shutdown() {
        isShutdownCalled = true;
    }

    //endregion
    //region InfoCache methods

    @Override
    public void evictInfos() {
        isPurgeInfosCalled = true;
    }

    @Override
    public Optional<Info> fetchInfo(Identifier identifier) {
        return Optional.empty();
    }

    @Override
    public void put(Identifier identifier, Info info) {}

    @Override
    public void put(Identifier identifier, String info) {}

    //endregion
    //region VariantCache methods

    @Override
    public void evict(OperationList opList) {}

    @Override
    public InputStream newVariantImageInputStream(
            OperationList opList,
            StatResult statResult) throws IOException {
        return null;
    }

    @Override
    public CompletableOutputStream
    newVariantImageOutputStream(OperationList opList) throws IOException {
        return null;
    }

}