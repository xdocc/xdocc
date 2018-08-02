<figure>
  <#if link??><a href="${link}"></#if>
  <img src="${path}/${srcsets?last.src}" srcset="<#list srcsets as srcset>${path}/${srcset.src} ${srcset.attribute}<#sep>,</#list> sizes="90vw">
  <figcaption>${name}</figcaption>
  <#if link??></a></#if>
</figure>