<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrappter" class="history">
        <div id="head">
            <h1><strong>${page.name}</strong></h1>
            <ul class="actions">
                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=' + page.urlPath)}"
                       class="action-view-page">Home</a>
                </li>
            </ul>
        </div>

        <div id="wiki-history">
            <fieldset>
                <table>
                    <tbody>
                    </tbody>
                </table>
            </fieldset>
        </div>

        <div id="footer">
        </div>
    </div>
</@layout.layout>
