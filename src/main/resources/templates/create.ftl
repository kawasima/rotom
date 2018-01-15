<#import "layout.ftl" as layout/>
<@layout.layout>
<div id="wiki-wrapper" class="create">
    <div id="head">
        <h1>Create New Page</h1>
    </div>
    <div id="wiki-content" class="create edit">
        <div class="has-sidebar">
            <#include "editor.ftl"/>
        </div>
    </div>
</div>
</@layout.layout>