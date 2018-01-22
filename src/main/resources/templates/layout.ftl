<#macro layout title="Rotom">
<!DOCTYPE html>
    <html>
        <head>
            <meta http-equiv="Content-type" content="text/html;charset=utf-8">
            <meta name="MobileOptimized" content="width">
            <meta name="HandheldFriendly" content="true">
            <meta name="viewport" content="width=device-width">
            <link rel="stylesheet" type="text/css" href="${baseUrl()}/assets/css/gollum.css" media="all">
            <link rel="stylesheet" type="text/css" href="${baseUrl()}/assets/css/editor.css" media="all">
            <link rel="stylesheet" type="text/css" href="${baseUrl()}/assets/css/dialog.css" media="all">
            <link rel="stylesheet" type="text/css" href="${baseUrl()}/assets/css/template.css" media="all">
            <link rel="stylesheet" type="text/css" href="${baseUrl()}/assets/css/print.css" media="print">

            <script>
             var baseUrl = '${baseUrl()}';
            </script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/jquery-1.7.2.min.js"></script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/mousetrap.min.js"></script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/gollum.js"></script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/gollum.dialog.js"></script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/gollum.placeholder.js"></script>
            <script type="text/javascript" src="${baseUrl()}/assets/javascript/editor/gollum.editor.js"></script>

    <title>${title}</title>
</head>
<body>
<#nested/>
</body>
</html>
</#macro>
