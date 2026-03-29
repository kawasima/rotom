<div id="gollum-editor" class="<#if isCreatePage>create</#if> <#if isEditPage>edit</#if>">
    <#if isCreatePage>
        <#assign action="${urlFor('create')}"/>
    </#if>
    <#if isEditPage>
        <#assign action="${urlFor('edit?path=' + page.urlPath)}"/>
    </#if>
    <form name="gollum-editor" id="gollum-editor-form" action="${action}" method="post">
        <fieldset id="gollum-editor-fields">
            <#if isCreatePage>
                <div id="gollum-editor-title-field" class="singleline">
                    <label for="page">Page Title</label>
                    <input type="text" name="page" id="gollum-editor-page-title" value="${page.name}"
                        pattern="^(?!/)[^&quot;<>*:?\\|]+$" title="You can use characters except : &quot; < > * : ? \ |" required autofocus>
                </div>
            </#if>
            <#if isEditPage>
                <input type="hidden" name="page" id="gollum-editor-page-title" value="${page.name}">
            </#if>
            <input type="hidden" name="dir" id="gollum-editor-page-dir" value="${page.dir}">

            <div id="gollum-editor-function-bar">
                <div id="gollum-editor-function-buttons">
                    <button type="button" data-action="bold" title="Bold (Ctrl+B)"><strong>B</strong></button>
                    <button type="button" data-action="italic" title="Italic (Ctrl+I)"><em>I</em></button>
                    <button type="button" data-action="strikethrough" title="Strikethrough"><s>S</s></button>
                    <button type="button" data-action="code" title="Code">&lt;/&gt;</button>

                    <span class="function-divider"></span>

                    <button type="button" data-action="h1" title="Heading 1">H1</button>
                    <button type="button" data-action="h2" title="Heading 2">H2</button>
                    <button type="button" data-action="h3" title="Heading 3">H3</button>

                    <span class="function-divider"></span>

                    <button type="button" data-action="ul" title="Unordered List">&#8226;</button>
                    <button type="button" data-action="ol" title="Ordered List">1.</button>
                    <button type="button" data-action="tasklist" title="Task List">&#9744;</button>
                    <button type="button" data-action="blockquote" title="Blockquote">&gt;</button>
                    <button type="button" data-action="hr" title="Horizontal Rule">&mdash;</button>

                    <span class="function-divider"></span>

                    <button type="button" data-action="link" title="Link (Ctrl+K)">&#128279;</button>
                    <button type="button" data-action="wikilink" title="Wiki Link">[[&nbsp;]]</button>
                    <button type="button" data-action="image" title="Image">&#128247;</button>
                    <button type="button" data-action="table" title="Table">&#9638;</button>
                </div>

                <div id="gollum-editor-format-selector">
                    <label for="wiki_format">Format</label>
                    <select id="wiki_format" name="format">
                        <#list markupTypes as markupType>
                        <option value="${markupType}" <#if markupType.getName() == format>selected</#if>>${markupType.getName()}</option>
                        </#list>
                    </select>
                </div>
            </div>

            <div id="gollum-editor-tab-bar">
                <a href="#" id="gollum-editor-tab-write" class="tab active">Write</a>
                <a href="#" id="gollum-editor-tab-preview" class="tab">Preview</a>
            </div>

            <textarea id="gollum-editor-body" name="content" style="display:none">${page.textData}</textarea>
            <div id="gollum-editor-preview-pane" class="markdown-body" style="display:none;"></div>

            <div id="gollum-editor-edit-summary" class="singleline">
                <label for="message">Edit message:</label>
                <#if isCreatePage>
                    <#assign message="Created ${page.name} (${format})">
                </#if>
                <#if isEditPage>
                    <#assign message="Updated ${page.name} (${format})">
                </#if>
                <input type="text" name="message" id="gollum-editor-message-field" value="${message}">
            </div>

            <input type="submit" id="gollum-editor-submit" value="Save" title="Save current changes">
        </fieldset>
    </form>
</div>
