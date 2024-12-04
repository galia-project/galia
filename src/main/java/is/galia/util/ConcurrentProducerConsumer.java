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

package is.galia.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrently pits a consumer against a producer. Used only for testing, but
 * located here in order to be shareable to plugins.
 */
public final class ConcurrentProducerConsumer {

    private static final int DEFAULT_NUM_THREADS = 500;

    private final Callable<Void> consumer;
    private final Callable<Void> producer;
    private final AtomicInteger readCount    = new AtomicInteger(0);
    private final AtomicInteger writeCount   = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    private int numThreads = DEFAULT_NUM_THREADS;

    public ConcurrentProducerConsumer(Callable<Void> producer,
                                      Callable<Void> consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    public ConcurrentProducerConsumer(Callable<Void> producer,
                                      Callable<Void> consumer,
                                      int numThreads) {
        this(producer, consumer);
        this.numThreads = numThreads;
    }

    public void run() throws Exception {
        for (int i = 0; i < numThreads / 2f; i++) {
            new Thread(() -> { // producer thread
                try {
                    producer.call();
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    writeCount.incrementAndGet();
                }
            }).start();

            new Thread(() -> {
                while (true) {
                    // Spin until we have something to consume.
                    if (writeCount.get() > 0) {
                        try {
                            consumer.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                            failureCount.incrementAndGet();
                        } finally {
                            readCount.incrementAndGet();
                        }
                        break;
                    } else {
                        sleep(1);
                    }
                }
            }).start();
        }

        while (readCount.get() < numThreads / 2f ||
                writeCount.get() < numThreads / 2f) {
            sleep(1);
        }

        if (failureCount.get() > 0) {
            throw new Exception("Failure count: " + failureCount);
        }
    }

    public ConcurrentProducerConsumer numThreads(int count) {
        this.numThreads = count;
        return this;
    }

    private void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ignore) {
        }
    }

}
