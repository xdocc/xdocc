<#include "head.ftl">
<#if depth==0> <!-- main-->
  <#include "body-header-main.ftl">
</#if>
<header class="row sticky">
  <#if depth!=0> <!-- not main-->
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
    <#if depth==0> <!-- main-->
      <#include "landing.ftl">
      <div class="col-sm-12 col-md-12 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
          <div class="fluid card" id="document">${content}</div>
      </div>
    <#elseif layout=="cheat">
      <div class="col-sm-12 col-md-11 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
        <div class="fluid card" id="document">${content}</div>
      </div>
    <#elseif layout=="blog">
      <div class="col-sm-12 col-md-11 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
          <div class="fluid card" id="document_blog">${content}</div>
      </div>
    <#elseif layout=="old">
      <div class="col-sm-12 col-md-11 col-lg-10 col-sm-offset-0 col-md-offset-0 col-lg-offset-1 fluid">
          <div class="fluid card" id="document_blog">${content}</div>
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
  </div>
</div>

<#include "footer.ftl">