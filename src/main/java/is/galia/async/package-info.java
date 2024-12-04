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

/**
 * <p>Provides asynchronous processing features:</p>
 *
 * <ul>
 *     <li>{@link is.galia.async.ThreadPool} maintains a
 *     pool of hardware threads that can run {@link java.lang.Runnable}s or
 *     {@link java.util.concurrent.Callable}s.</li>
 *     <li>{@link is.galia.async.VirtualThreadPool} provides access to a
 *     virtual thread executor.</li>
 *     <li>{@link is.galia.async.TaskQueue} can be used
 *     to submit {@link java.lang.Runnable}s to a hardware threaded queue.</li>
 * </ul>
 */
package is.galia.async;
