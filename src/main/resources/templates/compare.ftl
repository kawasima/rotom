<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrapper" class="compare">
        <div id="head">
            <h1>History for <strong>${page.name}</strong></h1>

            <ul class="actions">
                <li class="minibutton">
                </li>

                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=' + page.urlPath)}"
                       class="action-view-page">View Page</a>
                </li>

                <li class="minibutton">
                    <a href="${urlFor('edit?path=' + page.urlPath)}"
                       class="action-edit-page">Edit Page</a>
                </li>
                <li class="minibutton">
                    <a href="${urlFor('history?path=' + page.urlPath)}"
                       class="action-page-history">Page History</a>
                </li>
            </ul>
        </div>

        <div id="compare-content">
            <div class="data highlight">
                <table cellpadding="0" cellspacing="0">
                </table>
            </div>
        </div>
        <div id="footer">
            <ul class="actions">
                <li class="minibutton">
                    <a href="${urlFor('history?path=' + page.urlPath)}"
                       class="action-page-history">Back to Page History</a>
                </li>
                <li class="minibutton">
                    <a href="#">Back to Top</a>
                </li>
            </ul>
        </div>
    </div>
</@layout.layout>
