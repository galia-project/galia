# Migration Guide

If you are skipping versions, work through the sections backwards from your
current version.

## Cantaloupe 5.0.5 → Galia 1.0

Galia has evolved so much from Cantaloupe that it is difficult to make a
concrete list of upgrade steps. The items below augment `CHANGES.md`, which
is also recommended reading.

* Galia's YAML-format configuration file is incompatible with Cantaloupe's
  properties-format configuration file. Because of that, and because there
  are so many new, changed, and removed keys, it is recommended to start over
  with a new configuration file.
* Some of the functionality provided by various Cantaloupe components (like
  PdfBoxProcessor, S3Source, etc.) has been moved into plugins. This includes
  JRuby, which enables the Ruby delegate. See the Plugins sections of the
  website for a list of available plugins. To install a plugin, use
  `bin/install_plugin.sh`.
* Additionally, some of the functionality available in Cantaloupe has been
  removed:
    * KakaduNativeProcessor, OpenJpegProcessor, and GrokProcessor are no
      longer available and have been replaced by galia-plugin-openjpeg.
    * The built-in GeoSolutions TIFF reader has been removed and replaced by
      the JDK TIFF reader, which has fewer features. The galia-plugin-libtiff
      plugin is available as an alternative. 
* Java >= 22 is now required.
* Existing cached content is not compatible.
* The following HTTP API methods have been renamed:
    * `PurgeInfoCache` -> `EvictInfosFromCache`
    * `PurgeInvalidFromCache` -> `EvictInvalidFromCache`
    * `PurgeItemFromCache` -> `EvictItemFromCache`
* The `X-Forwarded-Path` header has been replaced with `X-Forwarded-BasePath`.
  Some users were using URL rewriting and proxying in order to gain more
  control over their endpoint URIs. The new `endpoint.iiif.n.path`
  configuration keys should make this easier.
* The `page` and `time` query arguments are no longer supported. Any clients
  using these must be modified to insert a page number or seconds offset into
  the meta-identifier instead.
* The `pre_authorize()` delegate method has been renamed to
  `authorize_before_access()`. 
* The `extra_iiifn_information_response_keys()` delegate methods have been
  redesigned as `customize_iiifn_information_response()`.
* The structure of the `exif` section of the `metadata` delegate context key
  has changed in order to better accommodate multiple values per field.
