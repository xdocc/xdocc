<div id="file">
<table>
 <tr>
  <th></th>
  <th>Name</th>
  <th>Modification Date</a></th>
  <th>Size</a></th>
 </tr>
 <tr>
  <td>
   <#if size??>[file]<#else>[folder]</#if>
  </td>
  <td><a href="${url}">${name}</a></td>
  <td><#if date??>${date?datetime}</#if></td>
  <td><#if size??>${size} bytes<#else>-</#if></td>
 </tr>
</table>
</div>