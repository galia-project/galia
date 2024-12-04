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

package is.galia.status;

import is.galia.Application;
import is.galia.cache.CacheFacade;
import is.galia.cache.HeapInfoCache;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Provides views into various application status data points. All accessors
 * provide "live" values.
 */
public final class ApplicationStatus {

    public String getApplicationVersion() {
        return Application.getVersion().toString();
    }

    /**
     * @return Max {@link HeapInfoCache} size in bytes.
     */
    public long getHeapInfoCacheMaxSize() {
        Optional<HeapInfoCache> optCache = new CacheFacade().getHeapInfoCache();
        return optCache.map(HeapInfoCache::maxSize).orElse(0L);
    }

    /**
     * @return Number of {@link HeapInfoCache cached infos}.
     */
    public long getHeapInfoCacheSize() {
        Optional<HeapInfoCache> optCache = new CacheFacade().getHeapInfoCache();
        return optCache.map(HeapInfoCache::size).orElse(0L);
    }

    /**
     * @return Number of available processor cores.
     */
    public int getNumProcessors() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.availableProcessors();
    }

    /**
     * @return Free VM heap in bytes.
     */
    public long getVMFreeHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory();
    }

    /**
     * @return JVM info.
     */
    public String getVMInfo() {
        return System.getProperty("java.vm.info");
    }

    /**
     * @return Max VM heap in bytes.
     */
    public long getVMMaxHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory();
    }

    /**
     * @return JVM name.
     */
    public String getVMName() {
        return System.getProperty("java.vm.name");
    }

    public long getVMPID() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getPid();
    }

    /**
     * @return Total VM heap in bytes.
     */
    public long getVMTotalHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory();
    }

    /**
     * @return VM uptime in epoch milliseconds.
     */
    public long getVMUptime() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getUptime();
    }

    /**
     * @return Used VM heap in bytes.
     */
    public long getVMUsedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * @return JVM vendor.
     */
    public String getVMVendor() {
        return System.getProperty("java.vendor");
    }

    /**
     * @return JVM version.
     */
    public String getVMVersion() {
        return System.getProperty("java.version");
    }

    public Map<String,Object> toMap() {
        final Map<String,Object> status = new LinkedHashMap<>();

        { // Application
            var section = new LinkedHashMap<>();
            section.put("version", getApplicationVersion());
            status.put("application", section);
        }
        { // Heap info cache
            var section = new LinkedHashMap<>();
            section.put("size", getHeapInfoCacheSize());
            section.put("maxSize", getHeapInfoCacheMaxSize());
            status.put("heapInfoCache", section);
        }
        { // VM
            var section = new LinkedHashMap<>();
            section.put("vendor", getVMVendor());
            section.put("name", getVMName());
            section.put("version", getVMVersion());
            section.put("info", getVMInfo());
            section.put("numProcessors", getNumProcessors());
            section.put("usedHeapBytes", getVMTotalHeap() - getVMFreeHeap());
            section.put("freeHeapBytes", getVMFreeHeap());
            section.put("totalHeapBytes", getVMTotalHeap());
            section.put("maxHeapBytes", getVMMaxHeap());
            section.put("usedHeapPercent", getVMUsedHeap() / (double) getVMMaxHeap());
            section.put("uptimeMsec", getVMUptime());
            status.put("vm", section);
        }
        return status;
    }

}
