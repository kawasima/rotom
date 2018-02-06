<div id="gollum-editor" data-escaped-name="{{escaped_name}}" class="<#if isCreatePage>create</#if> <#if isEditPage>edit</#if> <#if principal??>uploads-allowed</#if>">
    <#if isCreatePage>
        <#assign action="${urlFor('create')}"/>
    </#if>
    <#if isEditPage>
        <#assign action="${urlFor('edit?path=' + page.urlPath)}"/>
    </#if>
    <form name="gollum-editor" action="${action}" method="post">
        <fieldset id="gollum-editor-fields">
            <#if isCreatePage>
                <div id="gollum-editor-title-field" class="singleline">
                    <label for="page" class="jaws">Page Title</label>
                    <input type="text" name="page" id="gollum-editor-page-title" value="${page.name}"
                        pattern="^(?!/)[^&quot;<>*:?\\|]+$" title="You can use characters except : &quot; < > * : ? \ |" required autofocus>
                    <p class="path_note"><strong>NOTE:</strong>You can use characters except : <strong>&quot; < > * : ? \ |</strong></p>
                </div>
            </#if>
            <#if isEditPage>
                <input type="hidden" name="page" id="gollum-editor-page-title" value="${page.name}">
            </#if>
            <input type="hidden" name="dir" id="gollum-editor-page-dir" value="${page.dir}">

            <div id="gollum-editor-function-bar">
                <div id="gollum-editor-function-buttons">
                    <a href="#" id="function-bold" class="function-button">
                        <span>Bold</span>
                    </a>
                    <a href="#" id="function-italic" class="function-button">
                        <span>Italic</span>
                    </a>
                    <a href="#" id="function-code" class="function-button">
                        <span>Code</span>
                    </a>

                    <span class="function-divider">&nbsp;</span>

                    <a href="#" id="function-ul" class="function-button">
                        <span>Unordered List</span>
                    </a>
                    <a href="#" id="function-ol" class="function-button">
                        <span>Ordered List</span>
                    </a>
                    <a href="#" id="function-blockquote" class="function-button">
                        <span>Blockquote</span>
                    </a>
                    <a href="#" id="function-hr" class="function-button">
                        <span>Horizontal Rule</span>
                    </a>

                    <span class="function-divider">&nbsp;</span>

                    <a href="#" id="function-h1" class="function-button">
                        <span>h1</span>
                    </a>
                    <a href="#" id="function-h2" class="function-button">
                        <span>h2</span>
                    </a>
                    <a href="#" id="function-h3" class="function-button">
                        <span>h3</span>
                    </a>

                    <span class="function-divider">&nbsp;</span>

                    <a href="#" id="function-link" class="function-button">
                        <span>Link</span>
                    </a>
                    <a href="#" id="function-image" class="function-button">
                        <span>Image</span>
                    </a>

                    <span class="function-divider">&nbsp;</span>

                    <a href="#" id="function-help" class="function-button">
                        <span>Help</span>
                    </a>
                </div>

                <div id="gollum-editor-format-selector">
                    <label for="format">Edit Mode</label>
                    <select id="wiki_format" name="format">
                        <#list markupTypes as markupType>
                        <option value="${markupType}">${markupType.getName()}</option>
                        </#list>
                    </select>
                </div>
            </div>
            <textarea id="gollum-editor-body"
                      data-markup-lang="${format}" name="content" class="mousetrap">${page.textData}</textarea>
            <div></div>
            <div id="gollum-editor-edit-summary" class="singleline">
                <label for="message" class="jaws">Edit message:</label>
                <#if isCreatePage>
                    <#assign message="Created ${page.name} (${format})">
                </#if>
                <#if isEditPage>
                    <#assign message="Updated ${page.name} (${format})">
                </#if>
                <input type="text" name="message" id="gollum-editor-message-field" value="${message}">
            </div>

            <span class="jaws"><br/></span>

            <input type="submit" id="gollum-editor-submit" value="Save" title="Save current changes">
        </fieldset>
    </form>
</div>
