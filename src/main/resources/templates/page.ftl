<#import "layout.ftl" as layout/>
<@layout.layout>
<div id="wiki-wrapper" class="page">
    <div id="head">
        <h1>${page.name}</h1>
        <ul class="actions">
            <li class="minibutton">
                <#include "searchbar.ftl">
            </li>
            <li class="minibutton">
                <a href="${urlFor('showPageOrFile?path=')}" class="action-home-page">Home</a>
            </li>
            <li class="minibutton">
                <a href="${urlFor('pages?path=')}" class="action-all-pages">All</a>
            </li>
            <li class="minibutton jaws">
                <a href="#" id="minibutton-new-page">New</a>
            </li>
            <li class="minibutton">
                <a href="${urlFor('edit?path=' + page.urlPath)}" class="action-edit-page">Edit</a>
            </li>
            <li class="minibutton">
                <a href="${urlFor('history?path=' + page.urlPath)}" class="action-edit-page">History</a>
            </li>
            <li class="minibutton">
                <a href="${urlFor('latestChanges?path=' + page.urlPath)}" class="action-edit-page">Latest Changes</a>
            </li>
        </ul>
    </div>
    <div id="wiki-content">
        <div>
            <div id="wiki-body" class="gollum-${page.format}-content">
                <div class="markdown-body">
                    ${page.formattedData?no_esc}
                </div>
            </div>
        </div>
    </div>
    <div id="footer">
        <p id="last-edit">Last edited by <b>${page.committer}</b>, <span class="time-elapsed" title="">${(page.modifiedTime * 1000)?number_to_datetime}</span></p>
    </div>
</div>

<form name="rename" method="POST" action="rename/${page.name}">
    <input type="hidden" name="rename"/>
    <input type="hidden" name="message"/>
</form>
</@layout.layout>
