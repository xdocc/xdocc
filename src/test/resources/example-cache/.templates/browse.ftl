<!-- available variables: 
 content -> the generated html
 name -> name of this item
 url -> url of this site
 date or nr
-->

<td>
 <#if size??><img src="${path}images/file.svg" class="icon">
 <#else><img src="${path}images/folder.svg" class="icon"></#if>
</td>

<td><a href="${url}">${filename}</a></td>

<td><#if date??>${date?datetime}</#if></td>

<td>
 <#if size??>${size} bytes
 <#else>-</#if>
</td>