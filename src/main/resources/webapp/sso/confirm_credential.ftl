<!DOCTYPE html>
<html>
<head>
    <title>SSO Login</title>
    <link rel="stylesheet" href="/sso/css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=320, initial-scale=1, maximum-scale=1"/>
</head>
<body>
<div id="page-content">
                          
            <div id="logo">
                <img src="${logoURL!}" alt="Site logo"/><br>
            </div>      
            <h2>${username!} already exists. If it is you, please confirm your credential.</h2>

	        <div class="login-box">
	
	            <#if loginErrorType??>
	              <#assign pa = "wrong ${loginErrorType}!">
	              <p class="error">${pa}</p>
	            </#if>
				
				<#assign pa = "/sso/${provider}/credential_confirm">
	            <form name="existinguser_form" method="POST" class="new_user_session" accept-charset="utf-8" action="${pa}">
	                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
	                <input name="redirectURI" type="hidden" value="${redirectURI!}">
	                <input id="clientId" name="clientId" size="30" type="hidden" value="${clientId!}">
					<input id="username" name="username" size="30" type="hidden" value="${username!}">
	                <input id="password" name="password" size="30" type="password" placeholder="Password"/>
	                
					<input name="newRegister" type="hidden" value="false" />
	                <input class="button button-login" name="commit" type="submit" value="Confirm"/>
	 		       
	            </form>
        
	       </div>
</div>

</body>
</html>