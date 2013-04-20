<td>
 <#if size??>[file]<#else>[folder]</#if>
</td>

<td><a href="${url}">${name}</a></td>

<td><#if date??>${date?datetime}</#if></td>

<td>
 <#if size??>${size} bytes
 <#else>-</#if>
</td>