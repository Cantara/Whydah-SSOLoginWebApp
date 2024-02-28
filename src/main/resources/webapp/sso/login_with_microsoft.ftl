<!DOCTYPE html>
<html>
    <head>
        <title>Login with Microsoft</title>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    	<link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css">
    	<link rel="stylesheet" href="css/whydah.css" type="text/css"/>
    	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    	<link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
       
    </head>
    <body>
        <div id="page-content">
            <div id="logo">
                <img src="${logoURL}" alt=""/>
            </div>

            <div class="login-box">
                <#if error??>
                    <p class="error">${error!}</p>
                </#if>
                <form id="form" action="aadlogin" class="new_user_session" name="aadlogin" method="post">
                    <p>You will be directed to Microsoft login page for authenticating your account.</p>
                    <h4><label for="username">Your Microsoft email</label></h4>
                    <#if redirectURI??>
		               	<input type="hidden" name="redirectURI" value="${redirectURI}"/>
		            </#if>
		             
                    <input id="loginusername" name="loginusername" size="30" type="text" placeholder="Enter your Microsoft email"/>
                    
                    <button id="submitbutton" class="customBtn" type="submit" onclick="submitAction(event);>
				      <span class="icon" style="background: url('/sso/images/microsoft-normal.png') transparent 5px 50% no-repeat;"></span>
				      <span class="buttonText">Continue to sign in with Microsoft</span>
				    </button>
                
                </form>
            </div>
        </div>
        <script>
			function submitAction(e) {
			   e.preventDefault();
			   document.getElementById("submitbutton").disabled = true;
			   document.getElementById("submitbutton").value = 'Directing to Microsoft login page, please wait...';
			   document.getElementById("form").submit();
			}
	  </script>
    </body>
</html>