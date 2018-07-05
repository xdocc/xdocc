# Introduction

Xdocc parses file and directory names that have a special syntax. Regular files and directories are simply copied to the output. Before going into details, here is the syntax of this folder:

```
1-doc[Documentation]page|nav
 1-intro[Introduction]nav.md
 2-files[File Structure]nav.md
```

## Syntax Alternatives

Sometimes its not possible to have the syntax in the filename, e.g., if the name length gets too large, or if the name has "/" in it. As an alternative, the syntax can be placed in a separate file with the same filename plus the suffic .xdocc

```
1-doc[Documentation]page|nav
 1-intro|nav.md
 1-intro|nav.md.xdocc (this file contains the line n=Introduction)
 2-files[File Structure]nav.md
```

For directories, a file with the name .xdocc can be placed inside the directory. A further alternative is to use frontmatter in the file 1-intro.nav.md:

```
---
n: Introduction
---
```

