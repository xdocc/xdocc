# File Structure

The file or directory names are structured in the following ways:

```
numberordate-optionalurlpath[optianalname]optionalproperties.suffix
```

|Name|Example|
|---|---|
|numberordate|**1** or **2014-01-01** or **2014-01-01_15:15:15**|
|optionalurlpath|**myurl**|
|optianalname|**About**|
|optionalproperty|**name=About**. Multiple properties are delimited with a &#124; e.g., **vis&#124;nav**|
|suffix|**md** or **txt**|

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

A special case is if nr = 0, then it is set to Long.MAX. The reason behind this, is that you can mix dates and numbers. If a file with a date is provided, then sort is descending. Now if the user wants to have a item always before, it can specify the nr as 0, which will be Long.MAX and will always be on top.

## Recursive properties for files and directories
Recursive means, if applied to a directory, its subdirectories will inherit the property

|Name|Shortname|Description|
|---|---|---|
|hide|hid|Does not compile or copy the files. Also if a file/directory starts with a dot "." or ends with a "~", its hidden as well|
|visible|vis|The file/directory becomes visible, that means also without the pattern 1-url.txt, the file/directory will be processed. The order is based on the filename|
|copy|cp|The file/directory will be copied and not processed regardless of the pattern|
|layout|l|If a layout is specified, then this property can be used in the template to change the design/layout. Example: 1-url\[Name\]layout=main.txt|

###Examples
The directory and all its files are not compiled or copied:
```
2014-mydirurl|hide/
 + 1-myfileurl.txt
 + 2-myfileurl.txt
```

The directory and all its files will be compiled even without the mandatory number or date in front (1- or 2014-). This is useful e.g., to display images as a gallery:
```
2014-mydirurl|vis/
 + image1.jpg
 + image1.jpg
```

The directory and all its files will be copied even with the a number or date in front (1- or 2014-). This is useful e.g., to browse contents of a directory:
```
2014-mydirurl|copy/
 + package.deb
 + notes.txt
 + readme.md
```

If layout is specified, all of its children will inherit the this layout property. The layout property can be used in templates:
```
2014-mydirurl|l=main/
 + 1-myfileurl1.txt
 + 2-myfileurl2.md
```


## Properties for files and directories
|Name|Shortname|Description|
|---|---|---|
|name|n|A name given to the file or directory that can be used in the template. |
|promote|prm|Content is promoted to the parent. The resulting item is treated as a file in the parent directory. If other files inside this directory are marked as promoted, only those files are promoted, otherwise the first file is promoted|
|expose|exp|All content is promoted to the parent. The resulting item is treated as a file in the parent directory.|


### Example
Alternatively [name] can be used as well. All three variants are treated the same:

```
1-url|name=About.txt
1-url|n=About.txt
1-url[About].txt
```



## Properties for directories
|Name|Shortname|Description|
|---|---|---|
|asc| |Sort ascending. If no sorting is provided, auto-sort is assumed: for small numbers ascending, for large numbers (dates are converted to unix timestamps) descending|
|desc|dsc|Sort ascending. If no sorting is provided, auto-sort is assumed: for small numbers ascending, for large numbers (dates are converted to unix timestamps) descending|
|nosplit|nosp|Every visible document is compiled into one index.html, no other files are generated. If neither page or noindex is provided, every visible document is compiled into one index.html as well as each visible document is compiled individually|
|noindex|nidx|No index.html is generated. If neither page or noindex is provided, every visible document is compiled into one index.html as well as each visible document is compiled individually|
|paging|p|The number of items per page|
|nav| |Mark the folder as part of the navigation|


The idea behind this syntax is that it should become clear from a filename what parameters are set. Thus, for parameters that have only a key, a 3-4 character abbreviation was chosen. For key-value parameters, a 1 charactor abbreviation was chosen, as it becomes clean from the value, what the parameter is.

### Example

The properties asc and desc/dsc set the sorting of the files in the directory according to the number or date. If nothing is provided autosort is assumed, where low numbers are sorted ascending and high numbers (e.g., dates) are sorted descending. The following example will compile the items in the following way: 3-third.txt, 2-second.txt, 1-first.txt.

```
1-dir|dsc
 + 1-first.txt
 + 2-second.txt
 + 3-third.txt
```

The property page tells a directory that every file will be compiled into one big HTML file. The following example will output only one index.html file:

```
1-dir|pag
 + 1-first.txt
 + 2-second.txt
 + 3-third.txt
```

If the propert pag is not provided, the followig 3 files will be created as well: first.html, second.html, third.html. The opposite of page is noindex, which will not create an index.html, but it will create first.html, second.html, third.html. The property noindex and page together will not create any html files.

```
1-dir|nidx
 + 1-first.txt
 + 2-second.txt
```

Paging will create several index files, e.g., index0.html, index1.html. If a directory has 40 itemes, paging is set to 20, then 2 index files: index0.html and index1.html will be created.

```
1-dir|p=20
 + 1-first.txt
 + 2-second.txt
```

examples that is used in this page is as follows. The document order number is 6, the url is cheat-fm, the name
is FreeMarker, the layout is "cheat", and it is rendered as one html (page), every item is visible (vis), and is
part of the global navigation (nav):
```
6-cheat-fm[FreeMarker]l=cheat|page|vis|nav
 + 2-files[File Structure]nav.md
```