<!DOCTYPE html>
<html>
<head>
    <title>Whydah Reset Password</title>
    <link rel="stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
</head>
<body>
<div id="page-content">
    <div id="logo">
        <img src="${logoURL}" alt="Whydah Signup Page"/>
        <h2>Whydah Signup</h2>
    </div>

    <div class="login-box">
        <#if error??>
        <p class="error">${error!}</p>
         </#if>
    <form method="post" class="new_user_session">
        <p>Signup - You will receive an email containing a link you can follow to complete the signup process..</p>
        <h4><label for="username">Username</label></h4>
        <input id="username" name="username" size="30" type="text" placeholder="Username"/>
    </form>
</div>
</div>
</body>
        </html>