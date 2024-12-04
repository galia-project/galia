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

package is.galia.codec.tiff;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Set of {@link Tag}s in a {@link Directory}.</p>
 *
 * <p>A new instance is empty (has no tags), but there are several subclasses
 * whose constructors preload them with tags.</p>
 */
public class TagSet {

    private final int ifdPointerTag;
    private final String name;
    private final Set<Tag> tags = new TreeSet<>();

    /**
     * @param tagNum Tag of an IFD pointer a.k.a. offset field.
     * @return       Instance corresponding to the given tag, or {@code null}
     *               if the given tag is not recognized.
     */
    public static TagSet forIFDPointerTag(int tagNum) {
        if (tagNum == EXIFBaselineTIFFTagSet.IFD_POINTER) {
            return newBaselineSuperset();
        } else if (tagNum == EXIFTagSet.IFD_POINTER) {
            return new EXIFTagSet();
        } else if (tagNum == EXIFGPSTagSet.IFD_POINTER) {
            return new EXIFGPSTagSet();
        } else if (tagNum == EXIFInteroperabilityTagSet.IFD_POINTER) {
            return new EXIFInteroperabilityTagSet();
        }
        return null;
    }

    /**
     * @return Instance including all baseline tags plus tags in other tag sets
     *         that go in the baseline tag set.
     */
    public static TagSet newBaselineSuperset() {
        TagSet baselineSet = new BaselineTIFFTagSet();
        new EXIFBaselineTIFFTagSet().getTags().forEach(baselineSet::addTag);
        new GeoTIFFTagSet().getTags().forEach(baselineSet::addTag);
        new TIFFFXTagSet().getTags().forEach(baselineSet::addTag);
        return baselineSet;
    }

    public TagSet(int ifdPointerTag, String name) {
        Objects.requireNonNull(name);
        this.ifdPointerTag = ifdPointerTag;
        this.name          = name;
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public boolean containsTag(int id) {
        return tags.stream().anyMatch(t -> t.id() == id);
    }

    /**
     * @return Whether the given object's {@link #getName() name} and {@link
     *         #getIFDPointerTag() IFD pointer tag} are equal. Tags are not
     *         considered.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof TagSet other) {
            return Objects.equals(this.name, other.name) &&
                    Objects.equals(this.ifdPointerTag, other.ifdPointerTag);
        }
        return false;
    }

    public int getIFDPointerTag() {
        return ifdPointerTag;
    }

    public String getName() {
        return name;
    }

    public Tag getTag(int id) {
        return tags.stream().filter(t -> t.id() == id).findFirst().orElse(null);
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.ifdPointerTag);
    }

    /**
     * Merges all tags in the given instance into the instance.
     */
    public void merge(TagSet other) {
        other.getTags().forEach(this::addTag);
    }

    public void removeAllTags() {
        tags.clear();
    }

    @Override
    public String toString() {
        return name + " (" + ifdPointerTag + ")";
    }

}
