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
                        <#list versions as version>
                            <tr>
                                <td class="author">
                                    <a href="javascript:void(0);">
                                        <span class="username">${version.committerIdent.name}</span>
                                    </a>
                                </td>
                                <td class="commit-name">
                                    <span class="time-elapsed" title="">${(version.commitTime * 1000)?number_to_datetime}:</span>&nbsp;
                                    ${version.shortMessage}
                                    [<a href="${urlFor('showPageOrFile?path=' + page.urlPath + '&sha1=' + version.id.getName())}">${version.id.getName()[0..7]}</a>]
                                </td>
                            </tr>
                        </#list>
                    </tbody>
                </table>
            </fieldset>
        </div>

        <div id="footer">
        </div>
    </div>
</@layout.layout>
