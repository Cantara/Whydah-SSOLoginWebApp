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
                <form id="form" method="post" class="new_user_session">
                    <p>You will receive an email containing instructions how to set a new password.</p>
                    <h4><label for="username">Username</label></h4>
                    <#if redirectURI??>
		               	<input type="hidden" name="redirectURI" value="${redirectURI}"/>
		             </#if>
		            
                    <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                    <input id="username" name="username" size="30" type="text" placeholder="Username"/>
                    <input id = "submitbutton" class="button button-login" name="commit" type="submit" value="Request new password" onclick="submitAction(event);"/>
                </form>
            </div>
        </div>
        <script>
			function submitAction(e) {
			   e.preventDefault();
			   document.getElementById("submitbutton").disabled = true;
			   document.getElementById("submitbutton").value = 'Submitting, please wait...';
			   document.getElementById("form").submit();
			}
	  </script>
    </body>
</html>