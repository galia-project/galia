# Change Log

## 1.0

Galia forks from [Cantaloupe](https://cantaloupe-project.github.io) at commit
[21057cf](https://github.com/cantaloupe-project/cantaloupe/commit/21057cf72071840e2a984450bf8845c6e6490ec9),
which is essentially version 5.0.5 plus a small amount of work toward 6.0. The
significant changes are summarized below.

### License

* The license has changed to
  [Polyform Noncommercial](https://polyformproject.org/licenses/noncommercial/1.0.0/),
  which authorizes most non-commercial usage, whether by individuals or
  non-profit organizations. See the [License page](https://galia.is/license.html)
  of the website for more information.

### Architecture

* Codec support has been decoupled from the internal image processing engines,
  and is now provided by plugins, of which several are bundled into the core,
  and others are available as optional downloads.
* The above plugin support extends to sources, caches, encoders, decoders,
  delegates, and HTTP resources, a.k.a. endpoints. All of these can be
  developed externally and provided at runtime without modifying the core
  application.
* Thanks in part to this modularization, the core application distribution
  has slimmed down to just over 30MB, compared to over 80MB for Cantaloupe.
  Startup time is also <1 second on most systems.
* Whereas Cantaloupe is packaged as a "shaded JAR," Galia is packaged in a
  self-contained folder structure featuring separate Linux/Windows startup
  scripts, a configuration folder, a plugin folder, and others.
* The various VM arguments (`-Dcantaloupe.config`, etc.) have been replaced
  with application arguments. Startup scripts are provided (in the `bin`
  folder) which support a new `jvm.options` file in the `config` folder.

### Configuration

* The configuration file format is YAML. Properties format is no longer
  supported.
* The application no longer writes to the configuration file.
* `bin/configtest.sh` prints a report of missing config file keys, as well as
  any keys it contains that are recognized neither by the application nor any
  plugins.
* Support for configuration file inheritance has been removed.

### Endpoints

* The Control Panel has been removed. It may reappear in a future version.
* A Deep Zoom endpoint is available.
* All image endpoint path prefixes are configurable.
* IIIF image and information responses include a `Last-Modified` header when
  possible.
* IIIF image and information responses include an
  `Access-Control-Allow-Headers` header with a value of `Authorization`.
* IIIF information responses include a `pageCount` (or `page_count` for
  version 1) key.
* IIIF information responses return a complete, valid representation even for
  HTTP 4xx responses.
* IIIF information response JSON can be modified via a delegate method prior
  to it being returned.
* `!w,h` size requests to the IIIF Image API 3.0 endpoint for an image larger
  than its native size are fulfilled with a full-sized image instead of being
  rejected with an HTTP 400 error, which aligns more closely with the
  Image API 3.0 specification.
* Added an HTTP API method to evict all infos from the info cache (which has
  been split out from the variant cache; see below).
* The health check endpoint is enabled independently of the HTTP API endpoint.
* The idle timeout of the built-in web server's thread pool is configurable.
* The HTTP TRACE method is no longer allowed.
* Disabled endpoints return HTTP 404 instead of 403.
* The `page` and `time` query arguments (which were deprecated in Cantaloupe
  5.0) have been removed.
* The `X-Forwarded-Path` header has been replaced with `X-Forwarded-BasePath`.

### Sources

* All sources except FilesystemSource and HTTPSource have been removed from
  the core and made into optional plugins, all of which have received
  extensive updates:
    * galia-plugin-azurestorage provides **AzureBlobStorageSource**
    * galia-plugin-s3 provides **S3Source**
    * galia-plugin-jdbc provides **JDBCSource** & **PostgreSQLSource**
* HTTPSource supports a client HTTP proxy.
* HTTPSource can be configured to send a ranged GET request instead of a HEAD
  request, enabling it to work with pre-signed URLs that do not allow HEAD
  requests.
* Instead of separate per-request, per-source chunk caches, there is one
  unified chunk cache, which should improve performance and make tuning easier.

### Codecs

* Decoders for BMP, JPEG, PNG, GIF, and TIFF, which rely on the standard JDK
  Image I/O image readers for these formats, are built in.
* Encoders for all of the above formats except BMP, which rely on the standard
  JDK Image I/O image writers for these formats, are built in.
* Several new codec plugins have been developed:
    * **LibTIFFDecoder** uses Java 22's Foreign Function & Memory (FF&M) API
      to call into the LibTIFF library. It supports all standard TIFF
      compressions, pyramidal and multi-page images, BigTIFF, and efficient
      tile and strip reading. It can read from both files and streams. It also
      supports automatic orientation of images with no performance penalty.
        * This decoder supersedes the Image I/O TIFF reader from GeoSolutions
          SRL, which has been removed along with its JAI dependency.
    * **OpenJPEGDecoder** has been written from scratch to use the FF&M API
      to call into the OpenJPEG native library. This implementation is more
      efficient and robust than Cantaloupe's OpenJpegProcessor.
    * **FFmpegDecoder** has been written from scratch to use the FF&M API to
      call into the FFmpeg A/V codec library. Unlike Cantaloupe's
      FfmpegProcessor, it can read from streams, meaning that it can work
      seamlessly with videos hosted in non-filesystem storage.
    * **TurboJPEGDecoder** and **TurboJPEGEncoder** have been written from
      scratch to use the FF&M API to call into the latest major version of the
      TurboJPEG library (3.0).
    * **HEIFDecoder** and **HEIFEncoder** use the FF&M API to call into
      libheif. They support both HEIF (HEVC/H.265) and AVIF formats.
    * **WebPDecoder** and **WebPEncoder** use the FF&M API to call into
      Google's libwebp. The "advanced decoding" and "advanced encoding" APIs
      are utilized for fine-grained configurability.
    * **PDFBoxDecoder** has been extracted out of PdfBoxProcessor and updated
      to PDFBox version 3.
* Support for the following codecs has been removed:
    * Grok.
    * The Cantaloupe distribution included a Kakadu shared library made
      available under a Kakadu Public Service License. This license does not
      extend to Galia, and therefore, Kakadu has been removed for the time
      being. If you are interested in a license for a Kakadu decoder plugin,
      please contact
      [sales@bairdcreek.software](mailto:sales@bairdcreek.software).
* Decoders now respect the TIFF/EXIF `Orientation` values of embedded
  subimages, in addition to the primary image.
* Support for the XPM format has been removed. (It may return in a future
  plugin.)

### Image Processing

* Requests for small enough images can be fulfilled by embedded thumbnails,
  where available.
* Added a Magic Kernel Sharp 2013 scaling filter.

### Caches

* The source cache, which existed in order to enable processors that could
  only read from files to work with non-file sources, has been removed, as
  there is no longer any use for it.
* The derivative cache has been split into separate variant and info caches.
* The in-memory info cache has been renamed the heap info cache.
* HeapCache supports LRU operation.
* HeapCache's persistence feature has been removed.
* All caches except FilesystemCache and HeapCache have been removed from the
  core and are now available as optional plugins, all of which have received
  extensive updates:
    * galia-plugin-azurestorage provides **AzureBlobStorageCache**
    * galia-plugin-jdbc provides **JDBCCache** & **PostgreSQLCache**
    * galia-plugin-redis provides **RedisCache**
    * galia-plugin-s3 provides **S3Cache**

### Metadata

* The bundled EXIF reader now supports EXIF 3.0.

### Delegate System

* The JRuby delegate script mechanism has been moved out of the core and into
  a plugin (galia-plugin-jruby).
* The Java delegate system has been redesigned. Java delegates are now
  first-class plugin implementations with full access to internal API.
* Added a `request_class` key to the delegate request context.
* The delegate context's `metadata` key contains a new field, `xmp_elements`,
  that provides a high-level key-value view of the XMP model.
* Calling `puts` on the delegate request context object prints its keys and
  values in a convenient formatted table.
* The request header map (or Ruby hash) offers case-insensitive key access.

### Redaction

* Redaction color and opacity is configurable to support search result
  highlighting.

### Logging

* Logstash log appenders are now available.
* Added a configuration key to disable console log colorization.

### Other

* Java 22 is now required, as several of the new codec plugins rely on the
  Foreign Function & Memory API that debuted in that version.
* Extensive internal redesign.
* Improved tests and testing methods.
* Many dependency updates.
* Many bug fixes.
