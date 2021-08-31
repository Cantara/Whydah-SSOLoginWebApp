<!DOCTYPE HTML>
<html>
<head>
	<title>Whydah Login - Change password</title>
    <link rel="stylesheet" href="../css/whydah.css" TYPE="text/css"/>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
</head>
<body>
<div id="page-content">
    <div id="login-page">
        <div id="logo">
            <img src="${logoURL}" alt="Whydah sign on"/>
            <h2>Oops, a timeout has occurred</h2>
        </div>
        <div class="login-box">
            URL for password reset is no longer valid.<br/><br/>
            <#if redirectURI??>
              Please <a href="../resetpassword?redirectURI=${redirectURI}">reset password</a> and try again.            
            <#else>
            	Please <a href="../resetpassword">reset password</a> and try again.
            </#if> 
        </div>
    </div>
</div>
</body>
</html>