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
                <img src="${logoURL}" alt="Whydah Password reset"/>
                <h2>Request new password</h2>
            </div>

            <div class="login-box">
                <#if error??>
                    <p class="error">${error!}</p>
                </#if>
                <form method="post" class="new_user_session">
                    <p>You will receive an email containing instructions how to set a new password.</p>
                    <h4><label for="username">Username</label></h4>
                    <input id="username" name="username" size="30" type="text" placeholder="Username"/>
                    <input class="button button-login" name="commit" type="submit" value="Request new password"/>
                </form>
            </div>
        </div>
    </body>
</html>