<#include "head.ftl">
<#if layout=="main">
  <#include "body-header-main.ftl">
</#if>
<#include "body-header.ftl">

<div class="container">
  <div class="row">
    <#if depth == 1>
      <input type="checkbox" id="drawer-checkbox">
      <nav class="drawer col-md-3 col-lg-2">
        <label for="drawer-checkbox" class="close"></label>
	    <#list localnav as link>
          <a href="#${link.nr}"> ${link.name}</a>
        </#list>
      </nav>
    </#if>  
    <div class="col-sm-12 col-md-9 col-lg-10">
      <div id="document">${content}</div>
    </div>
  </div>
</div>

<#include "footer.ftl">