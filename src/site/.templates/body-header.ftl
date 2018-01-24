<header class="sticky">
    <label for="drawer-checkbox" class="button drawer-toggle"></label>
    <a href="${root}" class="logo"><img id="logo-small" src="${root}/design/xdocc-logo-inv.svg" alt="xdocc"></a>
    <#list globalnav as link>
      <a class="button" href="${root}/${link.url}"> ${link.name}</a>
    </#list>
    <img style="padding-left:1em;" class="centered" id="logo-small2" src="${root}/design/github.svg" alt="github">
    <a class="button" style="padding-left:0" href="https://github.com/xdocc">GitHub</a>
</header>