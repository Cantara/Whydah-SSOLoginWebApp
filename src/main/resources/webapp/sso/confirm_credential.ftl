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
	
	            <#if confirmError??>
	              <p class="error">${confirmError!}</p>
	            </#if>
				
                <input type="radio" name="accepted" value="yes" checked="checked" onclick="radioCheck(this)">Yes, it is me<br>
                <input type="radio" name="accepted" value="no" onclick="radioCheck(this)"> No, register new<br>

	            <form name="existinguser_form" method="POST" class="new_user_session" accept-charset="utf-8" action="${service?switch('azuread', '/sso/aad_credential_confirm', 'google', '/sso/google_credential_confirm', 'whydah', '/sso/whydah_credential_confirm', 'oidcProviderVipps', '/sso/vipps/credential_confirm', 'oidcProviderGoogle', '/sso/google/credential_confirm', 'oidcProviderMicrosoft', '/sso/microsoft/credential_confirm' )}">
	                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
	                <input name="redirectURI" type="hidden" value="${redirectURI!}">
					<input name="whydahOauth2Provider" type="hidden" value="${whydahOauth2Provider!}">
					<input id="username" name="username" size="30" type="hidden" value="${username!}">
	                <input id="password" name="password" size="30" type="password" placeholder="Password"/>
					<input name="newRegister" type="hidden" value="false" />
	                <input class="button button-login" name="commit" type="submit" value="Confirm"/>
	 		       
	            </form>
                <form name="newuser_form" method="POST" class="new_user_session" accept-charset="utf-8" action="${service?switch('azuread', '/sso/aad_credential_confirm', 'google', '/sso/google_credential_confirm', 'whydah', '/sso/whydah_credential_confirm', 'oidcProviderVipps', '/sso/vipps/credential_confirm', 'oidcProviderGoogle', '/sso/google/credential_confirm', 'oidcProviderMicrosoft', '/sso/microsoft/credential_confirm' )}">
	                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
	                <input name="redirectURI" type="hidden" value="${redirectURI!}">
					<input name="whydahOauth2Provider" type="hidden" value="${whydahOauth2Provider!}">
					<input name="newRegister" type="hidden" value="true" />
					
	                <input id="username" name="username" size="30" type="hidden" value="${username!}">
	                <input class="button button-login" name="commit" type="submit" value="Register new"/>
	            </form>
	        </div>
</div>
<script>
    document.newuser_form.style.display = 'none';
    function radioCheck(myRadio) {
    
        document.existinguser_form.style.display = myRadio.value === 'yes'? "block" :"none";
        document.newuser_form.style.display = myRadio.value === 'no'? "block" :"none";
    }

</script>
</body>
</html>