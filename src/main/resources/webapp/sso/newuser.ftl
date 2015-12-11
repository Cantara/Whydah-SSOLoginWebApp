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

            <#if error??><p class="error">${error!}</p></#if>

            <form method="POST" class="new_user_session">

                <h4><label for="user">Email:</label></h4>
                <input id="useremail" email="email" size="30" type="text" placeholder="Email"/>

                <h4><label for="username">Username:</label></h4>
                <input id="username" name="username" size="30" type="text" placeholder="Username"/>

                <h4><label for="firstname">First Name:</label></h4>
                <input id="firstname" name="firstname" size="30" type="text" placeholder="Firstname"/>

                <h4><label for="lastname">Last Name:</label></h4>
                <input id="lastname" name="lastname" size="30" type="text" placeholder="Lastname"/>

                <h4><label for="cellphone">Cell phone:</label></h4>
                <input id="cellphone" name="cellphone" size="30" type="text" placeholder="Cellphone"/>

                <p>You will receive an email with instructions of how to set your password.</p>

                <input class="button button-login" name="commit" type="submit" value="Register"/>

            </form>
        </div>
    </div>
</div>
</body>
</html>