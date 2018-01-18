<#import "layout.ftl" as layout/>
<@layout.layout>
<div id="wiki-wrapper" class="page">
    <div id="head">
        <h1>${page.fileName}</h1>
        <ul class="actions">
            <li class="minibutton">
                searchbar
            </li>
            <li class="minibutton">
                <a href="/" class="action-home-page">Home</a>
            </li>
            <li class="minibutton">
                <a href="/pages" class="action-all-pages">All</a>
            </li>
            <li class="minibutton">
                <a href="/file" class="action-fileview">Files</a>
            </li>
            <li class="minibutton">
                <a href="/edit/${page.urlPath}" class="action-edit-page">Edit</a>
            </li>
            <li class="minibutton">
                <a href="/history/${page.urlPath}" class="action-edit-page">History</a>
            </li>
            <li class="minibutton">
                <a href="/latest_changes/${page.urlPath}" class="action-edit-page">Latest Changes</a>
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
        <p id="last-edit">Last edited by <b>author</b>, date</p>
    </div>
</div>

<form name="rename" method="POST" action="rename/${page.name}">
    <input type="hidden" name="rename"/>
    <input type="hidden" name="message"/>
</form>
</@layout.layout>