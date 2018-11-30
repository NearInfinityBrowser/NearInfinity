JHexView component by Sebastian Porst (sp@porst.tv), modified by Argent77

Official website: https://github.com/sporst/JHexView
JHexView for Near Infinity: https://github.com/NearInfinityBrowser/JHexView
License: GPL 2.0

JHexView is a Java component for displaying binary data in Java applications
like hex editors or memory inspectors.

To see the component in action, please check out screenshots of the hex editor
Hexer that uses this component. You can find the screenshots at

http://www.the-interweb.com/serendipity/index.php?/archives/96-Hexer-1.0.0.html

Dependencies: https://github.com/sporst/splib

Features:
- Displays binary data in decimal and hexadecimal order
- Support for grouping bytes in columns
- Little endian or Big endian display
- Full mouse-wheel support
- Colorize individual bytes according to arbitrary criteria
- Colorize arbitrary ranges of bytes
- Customizable context-menus
- Can work with loaded data or data that must be dynamically loaded from an
  external source
- Full control over the display colors

Changes by Argent77:
- Included dependencies (splib)
- Added Apache Ant build script
- Find bytes/text support
- Copy/paste support
- Undo/redo support
