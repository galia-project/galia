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
import is.galia.image.StatResult;
import is.galia.operation.OperationList;
import is.galia.stream.CompletableOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class MockBrokenVariantCache extends AbstractCache
        implements VariantCache {

    //region Cache methods

    @Override
    public void evict(Identifier identifier) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void evictInvalid() throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public void purge() throws IOException {
        throw new IOException("I'm broken");
    }

    //endregion
    //region VariantCache methods

    @Override
    public void evict(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public InputStream newVariantImageInputStream(
            OperationList opList,
            StatResult statResult) throws IOException {
        throw new IOException("I'm broken");
    }

    @Override
    public CompletableOutputStream
    newVariantImageOutputStream(OperationList opList) throws IOException {
        throw new IOException("I'm broken");
    }

}
