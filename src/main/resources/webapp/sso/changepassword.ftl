<!DOCTYPE html>
<html>
<head>
    <title>Whydah Login - Change Password</title>
    <link rel="stylesheet" href="../css/whydah.css" TYPE="text/css"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <meta charset="utf-8"/>
</head>
<body>
<div id="page-content">
    <div id="login-page">
        <div id="logo">
            <img src="${logoURL}" alt="Whydah Sign on"/>
            <h2>New password</h2>
        </div>
        <div class="login-box">
            <#if error??>
                <p class="error">${error!}</p>
            </#if>

            <form action="../dochangepassword/${token}" method="post" class="new_user_session" name="changePasswordForm">
            	<#if redirectURI??>
               		<input type="hidden" name="redirectURI" value="${redirectURI}"/>
             	</#if>
                <h4><label for="user_session_password">New password for <em>${username}</em></label></h4>
                <input id="user_session_password" name="newpassword" size="30" type="password" onkeyup="hideErrors();"/>
                <h4><label for="user_session_password_2">Repeat password</label></h4>
                <input id="user_session_password_2" name="newpassword" size="30" type="password" onkeyup="hideErrors();checkPwMatch();"/>
        		<input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                <p class="error" id="pwMatchError" style="display:none">Passwords do not match.</p>
                <p class="error" id="pwEmptyError" style="display:none">Please enter a password.</p>
                <button class="button" type="button" value="Change password" onClick="changePassword();">Change password</button>
                <p>Note: Use of the 1000 most common passwords are prohibited for you own safety. Please create
                a safe password. The longer the password the more safe it is. Passwords with
                12-20 characters are considered ok.</p>
            </form>
            <script>
                function changePassword(){
                    var pw1 = document.getElementById('user_session_password').value;
                    var pw2 = document.getElementById('user_session_password_2').value;
                    if(pw1==''){
                        document.getElementById('pwEmptyError').style.display = '';
                    } else if(pw1!=pw2) {
                        document.getElementById('pwMatchError').style.display = '';
                    } else {
                        document.forms.changePasswordForm.submit();
                    }
                }
                function hideErrors(){
                    document.getElementById('pwEmptyError').style.display = 'none';
                    document.getElementById('pwMatchError').style.display = 'none';
                }
                function checkPwMatch(){
                    var pw1 = document.getElementById('user_session_password').value;
                    var pw2 = document.getElementById('user_session_password_2').value;
                    if(pw1!=pw2) {
                        document.getElementById('pwMatchError').style.display = '';
                    }
                }
            </script>
        </div>
    </div>
</div>
</body>
</html>
