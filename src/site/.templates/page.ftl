<#include "head.ftl">
<#if layout=="main">
  <#include "body-header-main.ftl">
</#if>

<header class="row sticky">
  <#if layout!="main">
    <label for="doc-drawer-checkbox" class="button drawer-toggle col-sm"></label>
  </#if>
  <a href="${root}" class="logo col-sm3 col-md"><img style="vertical-align: text-top;" id="logo-small2" src="${root}/design/xdocc-logo.svg" alt="xdocc"></a>
  <#list globalnav as link>
    <a class="button col-sm col-md" href="${root}/${link.url}">
        <img style="vertical-align: text-top;" id="logo-small2" src="${root}/design/${link.url}.svg" alt="${link.url}">
        <span>${link.name}</span>
    </a>
  </#list>
  <a class="button col-sm col-md" href="https://github.com/xdocc">
      <img style="vertical-align: text-top;" id="logo-small2" src="${root}/design/github.svg" alt="github">
      <span> GitHub</span>
  </a>
</header>

<div class="container responsive-padding">
  <div class="row">
    <#if layout=="main">
      <#include "landing.ftl">
      <div class="col-sm-10 col-md-10 col-lg-10 col-sm-offset-1 col-md-offset-1 col-lg-offset-1">
          <div id="document">${content}</div>
      </div>
    <#else>
      <input type="checkbox" id="doc-drawer-checkbox" class="drawer">
      <nav class="drawer col-md-3 col-lg-2 ">
        <label for="doc-drawer-checkbox" class="button drawer-close"></label>
	    <#list localnav as link>
          <a href="#${link.nr}"> ${link.name}</a>
        </#list>
      </nav>
      <div class="col-sm-12 col-md-9 col-lg-10 fluid">
          <div id="document">${content}</div>
      </div>
    </#if>
  </div>
</div>

<#include "footer.ftl">