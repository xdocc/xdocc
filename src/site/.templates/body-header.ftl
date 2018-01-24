<header class="sticky">
    <label for="drawer-checkbox" class="button drawer-toggle"></label>
    <a href="${root}" class="logo"><img id="logo-small" src="${root}/design/xdocc-logo-inv.svg" alt="xdocc"></a>
    <#list globalnav as link>
      <a class="button" href="${root}/${link.url}"> ${link.name}</a>
    </#list>
</header>