<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrapper" class="results">
        <div id="head" style="border: none;">
            <h1></h1>
            <ul class="actions">
            </ul>
        </div>
        <div id="pages">
            <#list results>
                <div id="file-browser">
                    <div class="breadcrumb">
                    </div>
                    <ul>
                        <#items as page>
                            <#if page.path?contains("/")>
                            <#else>
                                <li>${page.name}</li>
                            </#if>
                        </#items>
                    </ul>
                </div>
            </#list>
        </div>
    </div>
</@layout.layout>
