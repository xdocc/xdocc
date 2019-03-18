<#if layout=="old">
  <#list items as key, item>
    <div id=${item.nr}>
      ${item.date?string('dd.MM.yyyy')} - <a href="${path}/${item.url}">${item.name}</a>
    </div>
  </#list>
<#elseif layout=="gal">
  <#list items as key, item>
    <div id=${item.nr}>
      <a href="${path}/${item.url}">${item.url}</a>
      ${item.content}
    </div>
  </#list>
<#else>
  <#if layout??><div class="${layout}"></#if>
  <#list items as key, item>
    <div id=${item.nr}>
      ${item.content}
    </div>
  </#list>
  <#if layout??></div></#if>
</#if>
