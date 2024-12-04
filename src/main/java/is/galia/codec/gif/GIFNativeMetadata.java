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

package is.galia.codec.gif;

import com.fasterxml.jackson.annotation.JsonProperty;
import is.galia.image.NativeMetadata;

import java.util.Map;
import java.util.Objects;

final class GIFNativeMetadata implements NativeMetadata {

    private int delayTime, loopCount;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof GIFNativeMetadata other) {
            return hashCode() == other.hashCode();
        }
        return super.equals(obj);
    }

    @JsonProperty
    public int getDelayTime() {
        return delayTime;
    }

    @JsonProperty
    public int getLoopCount() {
        return loopCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delayTime, loopCount);
    }

    void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }

    @Override
    public Map<String, Integer> toMap() {
        return Map.of(
                "delayTime", getDelayTime(),
                "loopCount", getLoopCount());
    }

    @Override
    public String toString() {
        return "delayTime:" + delayTime + ";loopCount:" + loopCount;
    }

}
