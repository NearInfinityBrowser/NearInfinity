[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Argent77/NearInfinity?color=darkred&label=latest%20release)](https://GitHub.com/Argent77/NearInfinity/releases/latest)
[![GitHub release date (latest by date)](https://img.shields.io/github/release-date/Argent77/NearInfinity?color=gold)](https://GitHub.com/Argent77/NearInfinity/releases/latest)
[![Github downloads (total)](https://img.shields.io/github/downloads/Argent77/NearInfinity/total.svg?color=blueviolet)](https://GitHub.com/Argent77/NearInfinity/releases)

# Near Infinity

A file browser and editor for the Infinity Engine.

Find out more in the [Near Infinity Wiki](https://github.com/NearInfinityBrowser/NearInfinity/wiki).

Download Near Infinity directly from the [Releases section](https://github.com/Argent77/NearInfinity/releases)
or use [Nightly Releases](https://github.com/Argent77/NearInfinity/releases/tag/nightly) for the latest features
and bugfixes.

**Discuss Near Infinity on:**
- [GitHub Discussions](https://github.com/NearInfinityBrowser/NearInfinity/discussions)
- [Beamdog Forums](https://forums.beamdog.com/discussion/30593/new-versions-of-nearinfinity-available/)
- [Spellhold Studios Forums](http://www.shsforums.net/topic/45358-nearinfinity/)

## How to build Near Infinity

**Required tools:**
- [Oracle's Java SE Development Kit 8 or later](https://www.oracle.com/java/technologies/downloads/),
  or [OpenJDK 8 or later](https://adoptium.net/releases.html)
- [Apache Ant](https://ant.apache.org/)

If you wish to compile Near Infinity from source, the easiest way to do so is
by using the included Ant build script. The default target builds the `NearInfinity.jar`.

```bash
cd NearInfinity
ant
```

The sources also contain configuration files for the [Eclipse IDE](https://www.eclipse.org/)
and [IntelliJ IDEA](https://www.jetbrains.com/idea/download/).
It is preferred to use `Apache Ant` for compiling Near Infinity, however.

### Recreate parse tree

NI's BCS compiler uses code that is generated by the parser generator JavaCC. The file
`src/org/infinity/resource/bcs/parser/BafParser.jjt` is used to build the parse tree.
Use the following commands if you need to recreate it:

```bash
cd NearInfinity
ant parser-clean
ant parser-generate
```

The JavaCC executable is already included in the Near Infinity sources.

## Contributors

This section contains information intended for those who contribute
code to Near Infinity (NI). It contains some information on style and
explains how the code is organised and added to.

### Style

Code style is intended to improve code readability and to reduce
"diff noise" (meaningless changes).

Simply put, match the existing style. In particular:

* Use an indentation level of 2 spaces, not tab stops.

* Pay attention to how the existing code is indented and try to adhere
  to the same pattern.

* Do not leave trailing white space. Most decent editors have tools to
  help with this.

* End files with a single newline. Again, decent editors...

* No wildcard imports. While wildcarded import declarations are easy to
  use, they are also a source of error if you want to use classes of
  the same name from different packages. Modern editors or IDEs should
  take care of it automatically.

Additionally, try to avoid overly long lines. Breaking lines at column
80 or 100 is very standard, but since this is ReallyRidiculouslyLongNames
Java it's not always practical. Do try to limit yourself to less than
column 120, however, or it becomes difficult to read the code on
GitHub. The existing code is not always well-behaved in this regard,
however.

When in doubt, refer to the official
[Java conventions](https://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html) (outdated)
or [Google's Java Style](https://google.github.io/styleguide/javaguide.html).

### Workflow

The project consists of two active branches:

* `master` - code that reflects the latest stable release. It is only
  updated by a change of the version number which happens very rarely.

* `devel` - code that is ready to go into the next work-in-progress (WIP)
  release with little to no adjustment. That is, it should be complete and
  behave well locally, even if it has not been exhaustively tested under a
  wide variety of conditions.

If you work on something you should generally do so on a feature
branch based off devel. Once the feature is complete, tested and
preferably reviewed, it can be merged back into devel. While you are
working on your branch, be sure to keep it up to date with devel, in
order to avoid large divergences (and the resulting messy
merges). Small, straightforward commits can be made directly to
devel. Working off a feature branch also has the advantage of letting
others easily check out your work (provided you push it, naturally),
since they are able to pull your changes and simply check out your
branch (compared to needing to merge your devel branch into their own,
or clone a new local repository for your code).

The branch `gh-pages` is primarily used for hosting the update definition
file which is used to notify users when a new Near Infinity version has been
released.

Other branches, if any, are either used for testing purposes or are not
actively maintained anymore.

### Specifics

To maximise compatibility with available systems, NI's code base is using the
feature set of a specific Java version. You can find the currently supported
version by inspecting the variable `JAVA_VERSION` in `src/org/infinity/NearInfinity.java`.
