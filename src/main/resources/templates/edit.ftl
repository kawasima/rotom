<#import "layout.ftl" as layout/>
<@layout.layout>
<div id="wiki-wrapper" class="edit">
    <div id="head">
        <h1>Editing <strong>${page.name}</strong></h1>
        <ul class="actions">
            <li class="minibutton">
                <a href="${urlFor('showPageOrFile?path=' + page.urlPath)}" class="action-view-page">View Page</a>
            </li>
            <li class="minibutton">
                <a href="${urlFor('history?path=' + page.urlPath)}" class="action-page-history">Page History</a>
            </li>
        </ul>
    </div>
    <div id="wiki-content">
        <#include "editor.ftl"/>
    </div>
</div>
</@layout.layout>
