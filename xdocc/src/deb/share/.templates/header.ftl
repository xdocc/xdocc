<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
 <head>
  <title>Example Page</title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 </head>
 <body>
  <!-- Banner -->
  <h1>Example Page</h1>
  <h2>Minial example of xdocc</h2>
  
  <!-- Quick navigation (level0) for going further -->
  <ul>
   <#if path = ""><li class="current"><a href="${path}" class="current">Home</a></li>
   <#else><li><a href="${path}">Home</a></li></#if>
   <#list navigation.children as link>
    <#if link.selected><li class="current"><a href="${path}${link.URL}" class="current">${link.name}</a></li>
    <#else><li><a href="${path}${link.URL}"> ${link.name}</a></li></#if>
   </#list>
  </ul>

  
  <!-- Breadcrumb bar for going back -->
  <ul><li><a href="${path}">/</a></li>
   <#list breadcrumb as link>
    <li><a href="${path}${link.URL}"> ${link.name}</a></li>
   </#list>
  </ul>
  
  <!-- Detailed navigation level1-x for going further -->
  
  <#if local_navigation??>
   <ul><#list local_navigation.children as link>
    <li><a href="${path}${link.URL}">${link.name}</a></li>
   </#list></ul>
  </#if>