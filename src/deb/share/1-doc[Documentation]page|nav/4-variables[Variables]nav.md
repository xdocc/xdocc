# Variables

Variables are accessible from the template

|Name|Description|Usage|
|---|---|---|
|globalnav|Global Navigation|&lt;#list globalnav as link&gt;|
Example:
```
<#list globalnav as link>
  <a class="button col-sm col-md" href="${root}/${link.url}">
      <img style="vertical-align: text-top;" id="logo-small2" src="${root}/design/${link.url}.svg" alt="${link.url}">
      <span>${link.name}</span>
  </a>
</#list>
```
|Name|Description|Usage|
|---|---|---|
|isglobalnav|If the current navigation item is parto of the global Navigation|${isglobalnav}|
|localnav|Local Navigation|&lt;#list localnav as link&gt;|
The difference between local and global navigation is that the global navigations always starts at root. If a child
node has the navigation property, but is not part of the global navigation, then it is part of the local navigation. Example:

```
1-root
 + 2-dir1|nav
   + 3-dir2
     + 4-dir3|nav
 + 5-dir4|nav
```
In this example, 4-dir3 is local navigation, while 2-dir and 5-dir4 are global navigation.


|Name|Description|Usage|
|---|---|---|
|currentnav|Current Navigation Item|If you want to highlight the navigation item, you need to know where you are. Usage: ${link.url}|
|root|Relative path to root|&lt;a href=${root}/about&gt;|
|path|Path from root to current item|&lt;a href=${path}/about&gt;|
|breadcrumb|Paths to root|&lt;#list breadcrumb as link&gt;|
|content|Display content of the site|${content}|
|template|The name of the template for this item|&lt;#elseif layout=="cheat"&gt;|
|link|Link to this item|&lt;a href=${link.url}&gt;${link.name}&lt;/a&gt;|
|srcsets|For visible images, the width andd height of resized images|${srcset.src}|
|items|The items in a directory|&lt;#list items as item&gt;|
```
<#list items as item>
  <div id=${item.nr}>
    ${item.content}
  </div>
</#list>
```
|Name|Description|Usage|
|---|---|---|
|documentsize|The size of the document in bytes|${documentsize}|
|promotedepth|The depth of the promoted item. This way you know at which level the item is being displayed|${promotedepth}|
|consumesdirectory|(internal)|Sets the directory to be consumed by an external command|
|debug|shows all available variables|&lt;pre&gt;$debug&lt;pre&gt;|
|name|The name of the item|${name}|
|url|The url of the item|${url}|
|url|The url of the item|${url}|
|date|The date of the item|${date}|
|nr|The nr of the item|${nr}|
|filename|The filename of the item|${filename}|
|filescount|The number of files in the current directory of the item|${filescount}|
|filesize|The size of the file of the item|${filesize}|
|extensions|The extensions for this item|${extensions}|
|extensionlist|List of known extensions|${extensionlist}|
|properties|The properties for this item|${properties}|
|paging|The number of items to display in this directory|${paging}|
|layout|The layout for this item|${layout}|


|Name|Description|Usage|
|---|---|---|
|isascending|||
|isautosort|||
|iscompile|||
|isdescending|||
|isdirectory|||
|ishidden|||
|isnavigation|||
|isnoindex|||
|iscopy|||
|ispage|||
|ispromoted|||
|isroot|||
|isvisible|||
|isitemwritten|||
