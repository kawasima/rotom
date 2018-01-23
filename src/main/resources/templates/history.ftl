<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrapper" class="history">
        <div id="head">
            <h1>History for <strong>${page.name}</strong></h1>
            <ul class="actions">
                <li class="minibutton">
                </li>
                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=' + page.urlPath)}" class="action-view-page">View Page</a>
                </li>
                <#if hasPermission(userPrincipal, 'page:edit')>
                    <li class="minibutton">
                        <a href="${urlFor('edit?path=' + page.urlPath)}" class="action-edit-page">Edit Page</a>
                    </li>
                </#if>
            </ul>
        </div>
        <div id="wiki-history">
            <ul class="actions">
                <li class="minibutton">
                    <a href="javascript:document.querySelector('#version-form').submit();" class="action-compare-revision">Compare Revisions</a>
                </li>
            </ul>

            <form name="compare-versions" id="version-form" method="post"
                  action="${urlFor('compare?path=' + page.urlPath)}">
                <fieldset>
                    <table>
                        <tbody>
                            <#list versions as version>
                                <tr>
                                    <td class="checkbox">
                                        <input type="checkbox" name="versions[]" value="${version.id.getName()}">
                                    </td>
                                    <td class="author">
                                        <a href="javascript:void(0);">
                                            <span class="username">${version.committerIdent.name}</span>
                                        </a>
                                    </td>
                                    <td class="commit-name">
                                        <span class="time-elapsed" title="">${(version.commitTime * 1000)?number_to_datetime}</span>&nbsp;
                                        ${version.shortMessage}
                                        [<a href="${urlFor('showPageOrFile?path=' + page.urlPath + '&sha1=' + version.id.getName())}">${version.id.getName()[0..7]}</a>]
                                    </td>
                                </tr>
                            </#list>
                        </tbody>
                    </table>
                </fieldset>
            </form>
        </div>
    </div>
</@layout.layout>
