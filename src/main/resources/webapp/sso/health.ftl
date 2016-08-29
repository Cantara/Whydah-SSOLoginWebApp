<!DOCTYPE html>
<html>
<head>
    <title>INN Health</title>
    <link rel="stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=320, initial-scale=1, maximum-scale=1"/>
</head>
<body>
<pre>
${health!{}}
</pre>
<ul>
    <li>
        <a href="${securitytokenservice!{}}">${securitytokenservice!{}}</a>
    </li>
    <li>
        <a href="${useradminservice!{}}">${useradminservice!{}}</a>
    </li>
    <li>
        <a href="${crmservice!{}}">${crmservice!{}}</a>
    </li>
</ul>
</body>
</html>