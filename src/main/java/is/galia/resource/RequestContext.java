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

package is.galia.resource;

import is.galia.http.Reference;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Metadata;
import is.galia.image.ScaleConstraint;
import is.galia.operation.OperationList;

import java.util.Map;

/**
 * <p>Contains information about a client request.</p>
 *
 * <p>A single instance may be consulted from several different delegate
 * methods over the course of a request. Some delegate methods will be invoked
 * earlier in the request lifecycle before all information is available to be
 * filled in in the instance. It is normal for little information to be
 * available in the earliest-invoked delegate methods, and more or all
 * information to be available later.</p>
 *
 * <p>Developer note: adding, removing, or changing any of the properties also
 * requires updating {@link RequestContextMap}.</p>
 *
 * @see RequestContextMap
 */
public final class RequestContext {

    private String clientIPAddress;
    private Map<String,String> cookies;
    private Size fullSize;
    private Identifier identifier;
    private Reference localURI;
    private Metadata metadata;
    private OperationList operations;
    private Format outputFormat;
    private Integer pageCount;
    private Integer pageNumber;
    private Map<String,String> requestHeaders;
    private Reference requestURI;
    private String resourceClass;
    private Size resultingSize;
    private ScaleConstraint scaleConstraint;

    public String getClientIP() {
        return clientIPAddress;
    }

    public Map<String,String> getCookies() {
        return cookies;
    }

    public Size getFullSize() {
        return fullSize;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Reference getLocalURI() {
        return localURI;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public OperationList getOperationList() {
        return operations;
    }

    public Format getOutputFormat() {
        return outputFormat;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Map<String,String> getRequestHeaders() {
        return requestHeaders;
    }

    public Reference getRequestURI() {
        return requestURI;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    public Size getResultingSize() {
        return resultingSize;
    }

    public ScaleConstraint getScaleConstraint() {
        return scaleConstraint;
    }

    /**
     * @param clientIP May be {@code null}.
     */
    public void setClientIP(String clientIP) {
        this.clientIPAddress = clientIP;
    }

    /**
     * @param cookies May be {@code null}.
     */
    public void setCookies(Map<String,String> cookies) {
        this.cookies = cookies;
    }

    /**
     * @param fullSize May be {@code null}.
     */
    public void setFullSize(Size fullSize) {
        this.fullSize = fullSize;
    }

    /**
     * @param identifier May be {@code null}.
     */
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * @param uri URI seen by the application. May be {@code null}.
     * @see #setRequestURI(Reference)
     */
    public void setLocalURI(Reference uri) {
        this.localURI = uri;
    }

    /**
     * @param metadata May be {@code null}.
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * @param opList May be {@code null}.
     */
    public void setOperationList(OperationList opList) {
        this.operations = opList;
    }

    /**
     * @param outputFormat May be {@code null}.
     */
    public void setOutputFormat(Format outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * @param pageCount May be {@code null}.
     */
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * @param pageNumber May be {@code null}.
     */
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * @param requestHeaders May be {@code null}.
     */
    public void setRequestHeaders(Map<String,String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * @param uri URI requested by the client. May be {@code null}.
     * @see #setLocalURI(Reference)
     */
    public void setRequestURI(Reference uri) {
        this.requestURI = uri;
    }

    /**
     * @param resourceClass {@link Resource} implementation handling the
     *                      request.
     */
    public void setResourceClass(String resourceClass) {
        this.resourceClass = resourceClass;
    }

    /**
     * @param resultingSize May be {@code null}.
     */
    public void setResultingSize(Size resultingSize) {
        this.resultingSize = resultingSize;
    }

    /**
     * @param scaleConstraint May be {@code null}.
     */
    public void setScaleConstraint(ScaleConstraint scaleConstraint) {
        this.scaleConstraint = scaleConstraint;
    }

    /**
     * @return &quot;Live view&quot; map representation of the instance.
     * @see RequestContextMap for available keys.
     */
    public Map<String,Object> toMap() {
        return new RequestContextMap<>(this);
    }

}
