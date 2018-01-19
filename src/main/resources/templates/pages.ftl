<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrapper" class="results">
        <div id="head" style="border: none;">
            <h1>All pages in ${wiki.ref}</h1>
            <ul class="actions">
                <li class="minibutton">
                </li>
                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=')}"
                    class="action-home-page">Home</a>
                </li>
                <li class="minibutton jaws">
                    <a href="#" id="minibutton-new-page">New</a>
                </li>
            </ul>
        </div>

        <div id="pages">
            <#list pages>
                <div id="file-browser">
                    <div class="breadcrumb">
                    </div>
                    <ul>
                        <#items as page>
                            <#if page.path?contains("/")>
                                <li>
                                    <a href="${urlFor('pages?path=' + page.urlPath)}" class="folder">${page.name}</a>
                                </li>
                            <#else>
                                <li>
                                    <a href="${urlFor('pages?path=' + page.urlPath)}" class="file">${page.name}</a>
                                </li>
                            </#if>
                        </#items>
                    </ul>
                </div>
                <#else>
                <p id="no-results">
                    There are no pages in <stong>$ref</stong>
                </p>
            </#list>
        </div>
    </div>
</@layout.layout>
