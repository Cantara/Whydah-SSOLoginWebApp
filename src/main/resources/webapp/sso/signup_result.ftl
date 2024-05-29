<!DOCTYPE html>
<html>
<head>
    <title>Whydah User Signup</title>
    <link rel="stylesheet" href="/sso/css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
     <link rel="icon" href="/sso/images/favicon.ico" type="image/x-icon"/>
</head>
<body>
<div id="page-content">
    <div id="logo">
        <img src="${logoURL}" alt="Whydah Signup Page"/>
        <!--<h2>Whydah Signup</h2> -->
    </div>

    <div class="login-box">
        <#if error??>
            <p class="error">${error!}</p>
        </#if>
    <form method="post" class="new_user_session">
        <p>Signup - You will soon receive an email containing a link you can follow to complete the signup process..</p>
        <h4>Username:  ${username!} </h4>
    </form>
     <br/>
     
     <input type="hidden" name="redirectURI" value="${redirectURI}"/>
     <a href="${redirectURI}"><img src="/sso/images/history-back.svg" alt="back" /></a>
</div>
</div>
</body>
</html>