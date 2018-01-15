<div id="gollum-editor" data-escaped-name="{{escaped_name}}" class="<#if isCreatePage>create</#if> <#if isEditPage>edit</#if> <#if principal??>uploads-allowed</#if>">
    <form name="gollum-editor" action="/create" method="post">
        <fieldset id="gollum-editor-fields">
            <#if isCreatePage>
                <div id="gollum-editor-title-field" class="singleline">
                    <label for="page" class="jaws">Page Title</label>
                    <input type="text" name="page" id="gollum-editor-page-title" value="{{page_name}}">
                    {{#has_path}}
                    <p class="path_note"><strong>NOTE:</strong> This page will be created within the &quot;<strong>{{path}}</strong>&quot; directory</p>
                    {{/has_path}}
                </div>
            </#if>
            <input type="hidden" name="path" id="gollum-editor-page-path" value="{{path}}">
            <textarea id="gollum-editor-body"
                      data-markup-lang="${format}" name="content" class="mousetrap">${content}</textarea>
        </fieldset>
    </form>
</div>
