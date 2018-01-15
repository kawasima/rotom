<#import "layout.ftl" as layout/>
<@layout.layout>
<div id="wiki-wrapper" class="page">
    <div id="head">
        <h1>${page.fileName}</h1>
        <ul class="actions">
            <li class="minibutton">
                searchbar
            </li>
        </ul>
    </div>
    <div id="wiki-content">
        <div>
            <div id="wiki-toc-main">
                toc_content
            </div>
            <div id="wiki-sidebar" class="gollum--content">
                <div id="sidebar-content" class="markdown-body">
                    sidebar_content
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