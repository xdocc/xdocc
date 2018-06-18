# File Structure

The file or directory names are structured in the following ways:

```
number|date-url|properties.suffix
```

If a number or date is present the file or directory is flagged as visible. That means the following filenames are visible:

```
2014-.txt
2014-01-01-.txt
2014-01-01_15:15:15-.txt

```

The URL is typically lower case and comes after the dash:

```
2014-myurl.txt
2014-01-01-myurl.txt
2014-01-01_15:15:15-myurl.txt

```

If properties are specified after the URL, it must be separated by a "|". 


(rec)hidden, hide, . or ~
(rec)visible, vis 
(rec)layout,l
(rec) copy
(rec) keep_orig, keep

## Properties for files and directories
|Name|Shortname|Description|
|---|---|---|
|name|n|A name given to the file or directory that can be used in the template. Example: 1-url&#124;name=About.txt or 1-url&#124;n=About.txt|

## Properties for directories
|Name|Shortname|Description|
|---|---|---|
|asc| |Sort ascending. If no sorting is provided, auto-sort is assumed: for small numbers ascending, for large numbers (dates are converted to unix timestamps) descending|
|desc| |Sort ascending. If no sorting is provided, auto-sort is assumed: for small numbers ascending, for large numbers (dates are converted to unix timestamps) descending|
|page| |Every visible document is compiled into one index.html, no other files are generated. If neither page or noindex is provided, every visible document is compiled into one index.html and other files are generated as well|
|noindex|noidx|No index.html is generated. If neither page or noindex is provided, every visible document is compiled into one index.html and other files are generated as well|
|promote|prm|Content is promoted to the parent. The resulting item is treated as a file in the parent directory|
|highlight|hl|Items can be marked to be promoted|
|nav| |Mark the folder as part of the navigation|
|paging|pg|The number of items per page|