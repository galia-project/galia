# Galia Developer Guide

## Building & Running

### Command Line

* `mvn clean package` will build a release-style .zip file in the `target`
  folder.

### IDE

1. Add a new run configuration using the "Java Application" template or
   similar.
2. Set the main class to `is.galia.Application`, use `@config/jvm.options` as
   the VM argument, and set the `-config` argument.
    * Also, add `-Dis.galia.env=development` to your `config/jvm.options` file.
    * It's best to use copies of these config files instead, as they are under
      version control.

## Versioning

Galia uses semantic versioning. Major releases (n) involve major redesign that
breaks API compatibility. Minor releases (n.n) add features but do not break
compatibility. Patch releases (n.n.n) are for fixes only and also do not break
compatibility.

## Branching

The main branch is an unused orphan branch. Instead, there is one branch per
minor version. Work on a new minor or major version branches off from the
previous version branch.

Changes that would increment a patch version of a release are applied to its
release branch and then merged into any newer branches.

## Testing

* Unit tests: `mvn test`
* Integration tests: `mvn failsafe:integration-test`
* Unit and integration tests: `mvn verify`
* Javadoc tests: `mvn javadoc:javadoc`

Tests can also be run in Docker:

```sh
docker build -t galia:test -f docker/test/{platform}/Dockerfile .
docker run galia:test
```

## Contributions

The suggested process for contributing code changes is:

1. Submit a "heads-up" issue in the tracker, ideally before beginning any work.
2. Submit a Contributor License Agreement (CLA). No contributions can be
   accepted without one.
3. [Create a fork.](https://github.com/galia-project/galia/fork)
4. Create a feature branch off of your target version branch.
5. Make your changes.
6. Commit your changes.
7. Push the branch.
8. Create a pull request.

