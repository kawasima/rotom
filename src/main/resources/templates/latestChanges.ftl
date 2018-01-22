<#import "layout.ftl" as layout>
<@layout.layout>
    <div id="wiki-wrapper" class="history">
        <div id="head">
            <h1><strong>Latest Changes (Globally)</strong></h1>
            <ul class="actions">
                <li class="minibutton">
                    <a href="${urlFor('showPageOrFile?path=')}"
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
                                    [${version.id.getName()[0..7]}]
                                    <br>
                                    <a href=""></a>
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
