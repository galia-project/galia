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

package is.galia.test.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public final class PathAssert {

    public static void assertRecursiveFileCount(Path dir, long expected) {
        try (Stream<Path> stream = Files.walk(dir)) {
            long count = stream.filter(Files::isRegularFile)
                    .mapToLong(f -> 1).sum();
            assertEquals(expected, count);
        } catch (IOException e) {
            fail(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private PathAssert() {}

}
