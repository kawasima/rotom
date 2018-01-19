<#import "layout.ftl" as layout/>
<@layout.layout>
    <div id="wiki-wrapper" class="results">
        <div id="head">
            <h1>Search Results for <strong>${query}</strong></h1>
            <ul class="actions">
                <li class="minibutton">
                    <#include "searchbar.ftl">
                </li>
                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=')}"
                       class="action-home-page">Home</a>
                </li>
            </ul>
        </div>

        <div id="results">
            <#list pagination.results>
                <ul>
                    <#items as page>
                        <li>
                            <a href="${urlFor('showPageOrFile?path=' + page.path)}">${page.name}</a>
                            <span>${page.score}</span>
                            <div>
                                ${page.summary?no_esc}
                            </div>
                        </li>
                    </#items>
                </ul>
                <#else>
                <p id="no-results">
                    There are no results for your search <strong>${query}</strong>.
                </p>
            </#list>
        </div>

        <div id="footer">
            <ul class="actions">
                <li class="minibutton"><a href="#">Back to Top</a></li>
            </ul>
        </div>
    </div>
</@layout.layout>
