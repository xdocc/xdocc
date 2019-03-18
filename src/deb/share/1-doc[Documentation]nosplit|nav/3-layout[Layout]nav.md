# Layout

The layout option is a recursive property for files and directories. The following example uses the layout "cheat" for the FreeMarker documentation:

```
6-cheat-fm[FreeMarker]l=cheat|page|vis|nav
 + 2-files[File Structure]nav.md
```

Together with the site depth and the layout option, the template can be styled as required:

```
<#if depth==0> <!-- main-->
  <#include "landing.ftl">
  <div class="col-sm-12 col-md-12 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
      <div class="fluid card" id="document">${content}</div>
  </div>
<#elseif layout=="cheat">
  <div class="col-sm-12 col-md-11 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
    <div class="fluid card" id="document">${content}</div>
  </div>
<#else>
  <input type="checkbox" id="doc-drawer-checkbox" class="drawer">
  <nav class="drawer col-md-3 col-lg-2 ">
    <label for="doc-drawer-checkbox" class="button drawer-close"></label>
    <#list localnav as link>
      <a href="#${link.nr}"> ${link.name}</a>
    </#list>
  </nav>
  <div class="col-sm-12 col-md-11 col-lg-10 fluid">
      <div id="document">${content}</div>
  </div>
</#if>

```