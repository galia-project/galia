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

package is.galia.config;

import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * <p>Application-defined configuration key.</p>
 *
 * <p>Plugins can define their own configuration keys. Although they cannot be
 * values of this enum, they can be strings which can be retrieved by the
 * string accessor overloads of {@link Configuration}.</p>
 *
 * <h2>Adding a key</h2>
 *
 * <ol>
 *     <li>Add a value for it here.</li>
 *     <li>Add it to the sample config file.</li>
 *     <li>Document the addition in the upgrade guide.</li>
 * </ol>
 */
public record Key(String key, boolean isPublic) {

    public static final Key ACCESS_LOG_CONSOLEAPPENDER_ENABLED                   = new Key("log.access.ConsoleAppender.enabled", true);
    public static final Key ACCESS_LOG_FILEAPPENDER_ENABLED                      = new Key("log.access.FileAppender.enabled", true);
    public static final Key ACCESS_LOG_FILEAPPENDER_PATHNAME                     = new Key("log.access.FileAppender.pathname", true);
    public static final Key ACCESS_LOG_ROLLINGFILEAPPENDER_ENABLED               = new Key("log.access.RollingFileAppender.enabled", true);
    public static final Key ACCESS_LOG_ROLLINGFILEAPPENDER_PATHNAME              = new Key("log.access.RollingFileAppender.pathname", true);
    public static final Key ACCESS_LOG_ROLLINGFILEAPPENDER_POLICY                = new Key("log.access.RollingFileAppender.policy", true);
    public static final Key ACCESS_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN      = new Key("log.access.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern", true);
    public static final Key ACCESS_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY           = new Key("log.access.RollingFileAppender.TimeBasedRollingPolicy.max_history", true);
    public static final Key ACCESS_LOG_SYSLOGAPPENDER_ENABLED                    = new Key("log.access.SyslogAppender.enabled", true);
    public static final Key ACCESS_LOG_SYSLOGAPPENDER_HOST                       = new Key("log.access.SyslogAppender.host", true);
    public static final Key ACCESS_LOG_SYSLOGAPPENDER_PORT                       = new Key("log.access.SyslogAppender.port", true);
    public static final Key ACCESS_LOG_SYSLOGAPPENDER_FACILITY                   = new Key("log.access.SyslogAppender.facility", true);
    public static final Key API_ENABLED                                          = new Key("endpoint.api.enabled", true);
    public static final Key API_SECRET                                           = new Key("endpoint.api.secret", true);
    public static final Key API_USERNAME                                         = new Key("endpoint.api.username", true);
    public static final Key APPLICATION_LOG_CONSOLEAPPENDER_COLOR                = new Key("log.application.ConsoleAppender.color", true);
    public static final Key APPLICATION_LOG_CONSOLEAPPENDER_ENABLED              = new Key("log.application.ConsoleAppender.enabled", true);
    public static final Key APPLICATION_LOG_CONSOLEAPPENDER_FORMAT               = new Key("log.application.ConsoleAppender.format", true);
    public static final Key APPLICATION_LOG_FILEAPPENDER_ENABLED                 = new Key("log.application.FileAppender.enabled", true);
    public static final Key APPLICATION_LOG_FILEAPPENDER_FORMAT                  = new Key("log.application.FileAppender.format", true);
    public static final Key APPLICATION_LOG_FILEAPPENDER_PATHNAME                = new Key("log.application.FileAppender.pathname", true);
    public static final Key APPLICATION_LOG_LEVEL                                = new Key("log.application.level", true);
    public static final Key APPLICATION_LOG_LOGSTASHAPPENDER_ENABLED             = new Key("log.application.LogstashAppender.enabled", true);
    public static final Key APPLICATION_LOG_LOGSTASHAPPENDER_HOST                = new Key("log.application.LogstashAppender.host", true);
    public static final Key APPLICATION_LOG_LOGSTASHAPPENDER_PORT                = new Key("log.application.LogstashAppender.port", true);
    public static final Key APPLICATION_LOG_LOGSTASHAPPENDER_PROTOCOL            = new Key("log.application.LogstashAppender.protocol", true);
    public static final Key APPLICATION_LOG_LOGSTASHAPPENDER_SSL                 = new Key("log.application.LogstashAppender.ssl", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_ENABLED          = new Key("log.application.RollingFileAppender.enabled", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_FORMAT           = new Key("log.application.RollingFileAppender.format", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_PATHNAME         = new Key("log.application.RollingFileAppender.pathname", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_POLICY           = new Key("log.application.RollingFileAppender.policy", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN = new Key("log.application.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern", true);
    public static final Key APPLICATION_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY      = new Key("log.application.RollingFileAppender.TimeBasedRollingPolicy.max_history", true);
    public static final Key APPLICATION_LOG_SYSLOGAPPENDER_ENABLED               = new Key("log.application.SyslogAppender.enabled", true);
    public static final Key APPLICATION_LOG_SYSLOGAPPENDER_HOST                  = new Key("log.application.SyslogAppender.host", true);
    public static final Key APPLICATION_LOG_SYSLOGAPPENDER_PORT                  = new Key("log.application.SyslogAppender.port", true);
    public static final Key APPLICATION_LOG_SYSLOGAPPENDER_FACILITY              = new Key("log.application.SyslogAppender.facility", true);
    /** For development only; not present in the sample config file. */
    public static final Key ARTIFACT_REPOSITORY_BASE_URI                         = new Key("artifact_repo.base_uri", false);
    public static final Key BASE_URI                                             = new Key("base_uri", true);
    public static final Key CACHE_SERVER_EVICT_MISSING                           = new Key("cache.server.evict_missing", true);
    public static final Key CACHE_SERVER_RESOLVE_FIRST                           = new Key("cache.server.resolve_first", true);
    public static final Key CACHE_WORKER_ENABLED                                 = new Key("cache.server.worker.enabled", true);
    public static final Key CACHE_WORKER_INTERVAL                                = new Key("cache.server.worker.interval", true);
    public static final Key CHUNK_CACHE_ENABLED                                  = new Key("source.chunk_cache.enabled", true);
    public static final Key CHUNK_CACHE_MAX_SIZE                                 = new Key("source.chunk_cache.max_size", true);
    public static final Key CLIENT_CACHE_ENABLED                                 = new Key("cache.client.enabled", true);
    public static final Key CLIENT_CACHE_MAX_AGE                                 = new Key("cache.client.max_age", true);
    public static final Key CLIENT_CACHE_MUST_REVALIDATE                         = new Key("cache.client.must_revalidate", true);
    public static final Key CLIENT_CACHE_NO_CACHE                                = new Key("cache.client.no_cache", true);
    public static final Key CLIENT_CACHE_NO_STORE                                = new Key("cache.client.no_store", true);
    public static final Key CLIENT_CACHE_NO_TRANSFORM                            = new Key("cache.client.no_transform", true);
    public static final Key CLIENT_CACHE_PRIVATE                                 = new Key("cache.client.private", true);
    public static final Key CLIENT_CACHE_PROXY_REVALIDATE                        = new Key("cache.client.proxy_revalidate", true);
    public static final Key CLIENT_CACHE_PUBLIC                                  = new Key("cache.client.public", true);
    public static final Key CLIENT_CACHE_SHARED_MAX_AGE                          = new Key("cache.client.shared_max_age", true);
    public static final Key CUSTOMER_KEY                                         = new Key("customer_key", true);
    public static final Key DECODER_FORMATS                                      = new Key("decoder.formats", true);
    public static final Key DEEPZOOM_ENDPOINT_ENABLED                            = new Key("endpoint.deepzoom.enabled", true);
    public static final Key DEEPZOOM_ENDPOINT_PATH                               = new Key("endpoint.deepzoom.path", true);
    public static final Key DEEPZOOM_FORMAT                                      = new Key("endpoint.deepzoom.format", true);
    public static final Key DEEPZOOM_MIN_TILE_SIZE                               = new Key("endpoint.deepzoom.min_tile_size", true);
    /** For testing only; not present in the sample config file. */
    public static final Key DELEGATE_ENABLED                                     = new Key("delegate.enabled", false);
    public static final Key ENCODER_FORMATS                                      = new Key("encoder.formats", true);
    public static final Key ENCODER_JPEGENCODER_PROGRESSIVE                      = new Key("encoder.JPEGEncoder.progressive", true);
    public static final Key ENCODER_JPEGENCODER_QUALITY                          = new Key("encoder.JPEGEncoder.quality", true);
    public static final Key ENCODER_TIFFENCODER_COMPRESSION                      = new Key("encoder.TIFFEncoder.compression", true);
    public static final Key ERROR_LOG_FILEAPPENDER_ENABLED                       = new Key("log.error.FileAppender.enabled", true);
    public static final Key ERROR_LOG_FILEAPPENDER_FORMAT                        = new Key("log.error.FileAppender.format", true);
    public static final Key ERROR_LOG_FILEAPPENDER_PATHNAME                      = new Key("log.error.FileAppender.pathname", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_ENABLED                = new Key("log.error.RollingFileAppender.enabled", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_FORMAT                 = new Key("log.error.RollingFileAppender.format", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_PATHNAME               = new Key("log.error.RollingFileAppender.pathname", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_POLICY                 = new Key("log.error.RollingFileAppender.policy", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_FILENAME_PATTERN       = new Key("log.error.RollingFileAppender.TimeBasedRollingPolicy.filename_pattern", true);
    public static final Key ERROR_LOG_ROLLINGFILEAPPENDER_MAX_HISTORY            = new Key("log.error.RollingFileAppender.TimeBasedRollingPolicy.max_history", true);
    public static final Key FILESYSTEMCACHE_DIRECTORY_DEPTH                      = new Key("cache.FilesystemCache.dir.depth", true);
    public static final Key FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH                = new Key("cache.FilesystemCache.dir.name_length", true);
    public static final Key FILESYSTEMCACHE_PATHNAME                             = new Key("cache.FilesystemCache.pathname", true);
    public static final Key FILESYSTEMSOURCE_LOOKUP_STRATEGY                     = new Key("source.FilesystemSource.lookup_strategy", true);
    public static final Key FILESYSTEMSOURCE_PATH_PREFIX                         = new Key("source.FilesystemSource.BasicLookupStrategy.path_prefix", true);
    public static final Key FILESYSTEMSOURCE_PATH_SUFFIX                         = new Key("source.FilesystemSource.BasicLookupStrategy.path_suffix", true);
    public static final Key HEALTH_DEPENDENCY_CHECK                              = new Key("endpoint.health.dependency_check", true);
    public static final Key HEALTH_ENDPOINT_ENABLED                              = new Key("endpoint.health.enabled", true);
    public static final Key HEAPCACHE_TARGET_SIZE                                = new Key("cache.HeapCache.target_size", true);
    public static final Key HEAP_INFO_CACHE_ENABLED                              = new Key("cache.server.heap_info.enabled", true);
    public static final Key HTTP_ACCEPT_QUEUE_LIMIT                              = new Key("server.http.accept_queue_limit", true);
    public static final Key HTTP_CLIENT_IMPLEMENTATION                           = new Key("http_client.implementation", false);
    public static final Key HTTP_ENABLED                                         = new Key("server.http.enabled", true);
    public static final Key HTTP_HOST                                            = new Key("server.http.host", true);
    public static final Key HTTP_IDLE_TIMEOUT                                    = new Key("server.http.idle_timeout", true);
    public static final Key HTTP_MAX_THREADS                                     = new Key("server.http.max_threads", true);
    public static final Key HTTP_MIN_THREADS                                     = new Key("server.http.min_threads", true);
    public static final Key HTTP_PORT                                            = new Key("server.http.port", true);
    public static final Key HTTPSOURCE_ALLOW_INSECURE                            = new Key("source.HTTPSource.allow_insecure", true);
    public static final Key HTTPSOURCE_BASIC_AUTH_SECRET                         = new Key("source.HTTPSource.BasicLookupStrategy.auth.basic.secret", true);
    public static final Key HTTPSOURCE_BASIC_AUTH_USERNAME                       = new Key("source.HTTPSource.BasicLookupStrategy.auth.basic.username", true);
    public static final Key HTTPSOURCE_CHUNKING_ENABLED                          = new Key("source.HTTPSource.chunking.enabled", true);
    public static final Key HTTPSOURCE_CHUNK_SIZE                                = new Key("source.HTTPSource.chunking.chunk_size", true);
    public static final Key HTTPSOURCE_HTTP_PROXY_HOST                           = new Key("source.HTTPSource.proxy.http.host", true);
    public static final Key HTTPSOURCE_HTTP_PROXY_PORT                           = new Key("source.HTTPSource.proxy.http.port", true);
    public static final Key HTTPSOURCE_LOOKUP_STRATEGY                           = new Key("source.HTTPSource.lookup_strategy", true);
    public static final Key HTTPSOURCE_REQUEST_TIMEOUT                           = new Key("source.HTTPSource.request_timeout", true);
    public static final Key HTTPSOURCE_SEND_HEAD_REQUESTS                        = new Key("source.HTTPSource.BasicLookupStrategy.send_head_requests", true);
    public static final Key HTTPSOURCE_URL_PREFIX                                = new Key("source.HTTPSource.BasicLookupStrategy.url_prefix", true);
    public static final Key HTTPSOURCE_URL_SUFFIX                                = new Key("source.HTTPSource.BasicLookupStrategy.url_suffix", true);
    public static final Key HTTPS_ENABLED                                        = new Key("server.https.enabled", true);
    public static final Key HTTPS_HOST                                           = new Key("server.https.host", true);
    public static final Key HTTPS_KEY_PASSWORD                                   = new Key("server.https.key_password", true);
    public static final Key HTTPS_KEY_STORE_PASSWORD                             = new Key("server.https.key_store_password", true);
    public static final Key HTTPS_KEY_STORE_PATH                                 = new Key("server.https.key_store_path", true);
    public static final Key HTTPS_KEY_STORE_TYPE                                 = new Key("server.https.key_store_type", true);
    public static final Key HTTPS_PORT                                           = new Key("server.https.port", true);
    public static final Key IIIF_1_ENDPOINT_ENABLED                              = new Key("endpoint.iiif.1.enabled", true);
    public static final Key IIIF_1_ENDPOINT_PATH                                 = new Key("endpoint.iiif.1.path", true);
    public static final Key IIIF_2_ENDPOINT_ENABLED                              = new Key("endpoint.iiif.2.enabled", true);
    public static final Key IIIF_2_ENDPOINT_PATH                                 = new Key("endpoint.iiif.2.path", true);
    public static final Key IIIF_3_ENDPOINT_ENABLED                              = new Key("endpoint.iiif.3.enabled", true);
    public static final Key IIIF_3_ENDPOINT_PATH                                 = new Key("endpoint.iiif.3.path", true);
    public static final Key IIIF_MIN_SIZE                                        = new Key("endpoint.iiif.min_size", true);
    public static final Key IIIF_MIN_TILE_SIZE                                   = new Key("endpoint.iiif.min_tile_size", true);
    public static final Key IIIF_RESTRICT_TO_SIZES                               = new Key("endpoint.iiif.restrict_to_sizes", true);
    public static final Key INFO_CACHE                                           = new Key("cache.server.info.implementation", true);
    public static final Key INFO_CACHE_ENABLED                                   = new Key("cache.server.info.enabled", true);
    public static final Key INFO_CACHE_TTL                                       = new Key("cache.server.info.ttl_seconds", true);
    public static final Key LOG_ERROR_RESPONSES                                  = new Key("log_error_responses", true);
    public static final Key MAX_PIXELS                                           = new Key("max_pixels", true);
    public static final Key MAX_SCALE                                            = new Key("max_scale", true);
    public static final Key META_IDENTIFIER_TRANSFORMER                          = new Key("meta_identifier.transformer", true);
    public static final Key OVERLAY_ENABLED                                      = new Key("overlays.BasicStrategy.enabled", true);
    public static final Key OVERLAY_IMAGE                                        = new Key("overlays.BasicStrategy.image", true);
    public static final Key OVERLAY_INSET                                        = new Key("overlays.BasicStrategy.inset", true);
    public static final Key OVERLAY_OUTPUT_HEIGHT_THRESHOLD                      = new Key("overlays.BasicStrategy.output_height_threshold", true);
    public static final Key OVERLAY_OUTPUT_WIDTH_THRESHOLD                       = new Key("overlays.BasicStrategy.output_width_threshold", true);
    public static final Key OVERLAY_POSITION                                     = new Key("overlays.BasicStrategy.position", true);
    public static final Key OVERLAY_STRATEGY                                     = new Key("overlays.strategy", true);
    public static final Key OVERLAY_STRING_BACKGROUND_COLOR                      = new Key("overlays.BasicStrategy.string.background.color", true);
    public static final Key OVERLAY_STRING_COLOR                                 = new Key("overlays.BasicStrategy.string.color", true);
    public static final Key OVERLAY_STRING_FONT                                  = new Key("overlays.BasicStrategy.string.font", true);
    public static final Key OVERLAY_STRING_FONT_MIN_SIZE                         = new Key("overlays.BasicStrategy.string.font.min_size", true);
    public static final Key OVERLAY_STRING_FONT_SIZE                             = new Key("overlays.BasicStrategy.string.font.size", true);
    public static final Key OVERLAY_STRING_FONT_WEIGHT                           = new Key("overlays.BasicStrategy.string.font.weight", true);
    public static final Key OVERLAY_STRING_GLYPH_SPACING                         = new Key("overlays.BasicStrategy.string.glyph_spacing", true);
    public static final Key OVERLAY_STRING_STRING                                = new Key("overlays.BasicStrategy.string", true);
    public static final Key OVERLAY_STRING_STROKE_COLOR                          = new Key("overlays.BasicStrategy.string.stroke.color", true);
    public static final Key OVERLAY_STRING_STROKE_WIDTH                          = new Key("overlays.BasicStrategy.string.stroke.width", true);
    public static final Key OVERLAY_TYPE                                         = new Key("overlays.BasicStrategy.type", true);
    public static final Key PRINT_STACK_TRACE_ON_ERROR_PAGES                     = new Key("print_stack_trace_on_error_pages", true);
    public static final Key PROCESSOR_BACKGROUND_COLOR                           = new Key("processor.background_color", true);
    public static final Key PROCESSOR_DOWNSCALE_LINEAR                           = new Key("processor.downscale_linear", true);
    public static final Key PROCESSOR_DOWNSCALE_FILTER                           = new Key("processor.downscale_filter", true);
    public static final Key PROCESSOR_SHARPEN                                    = new Key("processor.sharpen", true);
    public static final Key PROCESSOR_UPSCALE_FILTER                             = new Key("processor.upscale_filter", true);
    public static final Key PROCESSOR_USE_EMBEDDED_THUMBNAILS                    = new Key("processor.use_embedded_thumbnails", true);
    /** Not present in the sample config file. */
    public static final Key REPORT_ERRORS                                        = new Key("report_errors", false);
    public static final Key SOURCE_DELEGATE                                      = new Key("source.delegate", true);
    public static final Key SOURCE_STATIC                                        = new Key("source.static", true);
    public static final Key SLASH_SUBSTITUTE                                     = new Key("slash_substitute", true);
    public static final Key STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER       = new Key("meta_identifier.transformer.StandardMetaIdentifierTransformer.delimiter", true);
    public static final Key TEMP_PATHNAME                                        = new Key("temp_pathname", true);
    public static final Key VARIANT_CACHE                                        = new Key("cache.server.variant.implementation", true);
    public static final Key VARIANT_CACHE_ENABLED                                = new Key("cache.server.variant.enabled", true);
    public static final Key VARIANT_CACHE_TTL                                    = new Key("cache.server.variant.ttl_seconds", true);

    /**
     * @return All application-defined keys. Plugins may define their own keys
     *         which will not be included.
     */
    public static Key[] values() {
        return Arrays.stream(Key.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .map(f -> {
                    try {
                        return f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(v -> v instanceof Key)
                .toList()
                .toArray(new Key[0]);
    }

    @Override
    public String toString() {
        return key;
    }

}
