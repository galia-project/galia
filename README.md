# Galia

*High-performance dynamic image server*

This is a quick getting-started guide. Detailed documentation is available on
the [Galia website](https://galia.is).

# Folder contents

* `bin/`                  Startup scripts.
* `config/`               Application configuration files.
* `lib/`                  Contains all JARs required by the application.
* `log/`                  Default location for logs, which can be changed in
                          the config file.
* `plugins/`              Contains all installed plugins. To install a plugin,
                          unzip it into here, or use `bin/install_plugin.sh`.
* `CHANGES.md`            Change log.
* `LICENSE.txt`           License of the software.
* `LICENSE-3RD-PARTY.txt` License information for dependencies.
* `README.md`             This file.
* `UPGRADING.md`          Migration guide.

# Initial setup

1. Modify `config/config.yml` and `config/jvm.options` as needed.
2. Install any desired plugins. Note that some plugins include their own
   `config.yml` files. Rather than copying these, you should copy their
   **contents** into your application config file.

Now you are ready to run `bin/start.sh` (Linux/macOS) or `bin/start.cmd`
(Windows).

# License

The default license is PolyForm Noncommercial, which permits most
non-commercial use. [LICENSE.txt](LICENSE.txt) contains the full text of this
license.

Note that PolyForm Noncommercial is **not** an open-source license as defined
by OSI.

For commercial and other types of licenses, contact
[sales@bairdcreek.software](mailto:sales@bairdcreek.software).
