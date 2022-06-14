<!DOCTYPE html>
<html>
<head>
    <title>Whydah Login</title>
    <link rel="stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=320, initial-scale=1, maximum-scale=1"/>
</head>
<body>
    <div id="page-content">
        <div id="signup-page">
            <div id="logo">
                <img src="${logoURL!}" alt="Whydah User Registration"/>
                <h2>Register new user</h2>
            </div>

        <div class="login-box">
			<a href="${redirectURI}"><img src="/sso/images/history-back.svg" alt="back" /></a>
            <#if error??><p class="error">${error!}</p></#if>

            <form method="POST" class="new_user_session" accept-charset="utf-8">
                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                <input type="hidden" name="redirectURI" value="${redirectURI}"/>

                <h4><label for="username">Username (*):</label></h4>
                <input id="username" name="username" size="30" type="text" placeholder="Username" required minlength="3"/>

                <h4><label for="firstname">First Name (*):</label></h4>
                <input id="firstname" name="firstname" size="30" type="text" placeholder="Firstname" required/>

                <h4><label for="lastname">Last Name (*):</label></h4>
                <input id="lastname" name="lastname" size="30" type="text" placeholder="Lastname" required/>

                <h4><label for="user">Email (*):</label></h4>
                <input id="useremail" name="useremail" size="30" type="email" placeholder="Email" required/>

                <h4><label for="cellphone">Cell phone: (optional)</label></h4>
                <input id="cellphone" name="cellphone" size="30" type="text" placeholder="Cellphone"/>
				<!--
                <p>You will receive an email with instructions of how to set your password.</p> !-->

                <input class="button button-login" name="commit" type="submit" value="Register"/>

            </form>
        </div>
    </div>
</div>
</body>
</html>