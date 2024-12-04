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

package is.galia.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain {@link Processor} instances.
 */
public final class ProcessorFactory {

    private static final Set<Class<? extends Processor>> ALL_PROCESSOR_IMPLS = Set.of(
            Java2DProcessor.class);

    private static final Set<Processor> ALL_PROCESSORS = new HashSet<>();

    public static synchronized Set<Processor> getAllProcessors() {
        if (ALL_PROCESSORS.isEmpty()) {
            for (Class<? extends Processor> class_ : ALL_PROCESSOR_IMPLS) {
                try {
                    ALL_PROCESSORS.add(class_.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    // This exception is safe to swallow as it will be thrown
                    // and handled elsewhere.
                }
            }
        }
        return Collections.unmodifiableSet(ALL_PROCESSORS);
    }

    /**
     * @return New instance.
     */
    public static Processor newProcessor() {
        return new Java2DProcessor();
    }

    private ProcessorFactory() {}

}
