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

package is.galia.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Set;

class ApplicationLogFilter extends Filter<ILoggingEvent> {

    private static final Set<String> AWS_SDK_LOGGERS = Set.of(
            "software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain",
            "software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage",
            "software.amazon.awssdk.http.auth.aws.internal.signer.DefaultV4RequestSigner",
            "software.amazon.awssdk.request",
            "software.amazon.awssdk.utils.async.SimplePublisher"); // logs tons of stuff at trace level

    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }
        // N.B.: these checks should be arranged in rough order of most to
        // least likely to occur, and least to most expensive.

        // Reject Jetty debug messages, even though they might be useful, as
        // they totally overwhelm the debug log.
        if (event.getLoggerName().startsWith("org.eclipse.jetty") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Reject Jetty access log messages. AccessLogFilter will accept them
        // instead.
        else if (org.eclipse.jetty.server.RequestLog.class.getName().equals(event.getLoggerName())) {
            return FilterReply.DENY;
        }
        // Reject Velocity debug messages.
        else if (event.getLoggerName().startsWith("org.apache.velocity") &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // PDFBox log messages are unfortunately not calibrated very well.
        // The debug log is overwhelming and there are ever lots of warnings
        // and even errors for trivial things that nobody but a PDF enthusiast
        // would care about.
        else if (event.getLoggerName().startsWith("org.apache.pdfbox") ||
                event.getLoggerName().startsWith("org.apache.fontbox")) {
            return FilterReply.DENY;
        }
        // More PDFBox
        else if ("org.apache.pdfbox.io.ScratchFile".equals(event.getLoggerName()) &&
                Level.DEBUG.isGreaterOrEqual(event.getLevel())) {
            return FilterReply.DENY;
        }
        // Reject various AWS SDK log messages.
        else if (Level.INFO.isGreaterOrEqual(event.getLevel()) &&
                AWS_SDK_LOGGERS.contains(event.getLoggerName())) {
            return FilterReply.DENY;
        }
        else if ("software.amazon.awssdk.profiles.internal.ProfileFileReader".equals(event.getLoggerName())) {
            return FilterReply.DENY;
        }
        // Reject Jetty static content access messages.
        else if (Level.INFO.isGreaterOrEqual(event.getLevel()) &&
                org.eclipse.jetty.server.ResourceService.class.getName().equals(event.getLoggerName())) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }

}
