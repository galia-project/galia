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

package is.galia.auth;

import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;

import java.io.IOException;
import java.util.Map;

/**
 * Authorizes a request using a {@link Delegate}.
 */
final class DelegateAuthorizer implements Authorizer {

    private final Delegate delegate;

    /**
     * @param args One-element array containing a {@link Delegate}.
     */
    DelegateAuthorizer(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    Delegate.class.getSimpleName() + " argument is required");
        }
        this.delegate = (Delegate) args[0];
    }

    @Override
    public AuthInfo authorizeBeforeAccess() throws IOException {
        try {
            return processDelegateMethodResult(delegate.authorizeBeforeAccess());
        } catch (DelegateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public AuthInfo authorize() throws IOException {
        try {
            return processDelegateMethodResult(delegate.authorize());
        } catch (DelegateException e) {
            throw new IOException(e);
        }
    }

    private AuthInfo processDelegateMethodResult(Object result) {
        if (result instanceof Map<?, ?> map) {
            Number status            = (Number) map.get("status_code");
            // Used when returning HTTP 401. Value will be inserted into a
            // WWW-Authorization header.
            String challenge         = (String) map.get("challenge");
            // Used when redirecting.
            String uri               = (String) map.get("location");
            // Used when redirecting to a virtual scale-reduced version.
            Number scaleNumerator   = (Number) map.get("scale_numerator");
            Number scaleDenominator = (Number) map.get("scale_denominator");

            return new AuthInfo.RestrictiveBuilder()
                    .withResponseStatus(status.intValue())
                    .withChallengeValue(challenge)
                    .withRedirectURI(uri)
                    .withRedirectScaleConstraint(scaleNumerator, scaleDenominator)
                    .build();
        } else if (result instanceof Boolean) {
            return new AuthInfo.BooleanBuilder((Boolean) result).build();
        } else {
            throw new IllegalArgumentException(
                    "Illegal return type from delegate method");
        }
    }

}
