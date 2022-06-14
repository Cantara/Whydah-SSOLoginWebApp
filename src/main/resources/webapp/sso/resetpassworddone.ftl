<!DOCTYPE html>
<html>
<head>
    <title>Whydah Login</title>
    <link rel="stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
</head>
<body>

<div id="page-content">
    <div id="login-page">
        <div id="logo">
            <img src="${logoURL}" alt="Whydah Log In"/>

        </div>
        <div style="margin:15px;padding:0;display:inline"></div>
        <div>
            <h4>Username:  ${username!} </h4>
            Your password has been reset. You will soon receive an email containing instructions on how to acquire a new password.</br></br>
           
            <#if redirectURI??>
              <input type="hidden" name="redirectURI" value="${redirectURI}"/>
     		  <a href="${redirectURI}"><img src="/sso/images/history-back.svg" alt="back" /></a>       	         
			<#else>
			  <a href="/sso/welcome" class="new_password">Forgot password</a>
			</#if>
			            
           
     
        </div>
    </div>
</div>
</body>
</html>