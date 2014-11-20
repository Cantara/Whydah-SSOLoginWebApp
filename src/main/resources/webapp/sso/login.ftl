<!DOCTYPE html>
<html>
<head>
    <title>Whydah Login</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <link rel="stylesheet" href="css/whydah.css" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
</head>
<body>
    <div style="display:none;">
        FacebookLogin is <#if facebookLoginEnabled == true> enabled<#else> disabled</#if>.
        NetIQLogin is <#if netIQLoginEnabled == true> enabled<#else> disabled</#if>.
        OpenID Login is <#if openidLoginEnabled == true> enabled<#else> disabled</#if>.
        Omni Login is <#if omniLoginEnabled == true> enabled<#else> disabled</#if>.
        Userpassword Login is <#if userpasswordLoginEnabled == true> enabled<#else> disabled</#if>.
    </div>
    
    <div id="page-content">
        <div id="logo">
            <img src="${logoURL!}" alt="Site logo"/><br>
            <h2>Whydah SSO login</h2>
        </div>
        <#if loginError??>
            <p class="error">${loginError!}</p>
        </#if>
            
        <#if userpasswordLoginEnabled == true>
            <div class="login-box">
                <form action="action" class="new_user_session" name="getusertoken" method="post">
                    <div id="normal-login">
                        <h4><label for="user_session_login">Username</label></h4>
                        <input id="user_session_login" name="user" type="text" placeholder="Username" autofocus>
                        <h4><label for="user_session_password">Password</label></h4>
                        <input id="user_session_password" name="password" type="password" autocomplete="off" placeholder="Password">
                        <input type="hidden" name="redirectURI" value="${redirectURI!"welcome"}">
                        <input class="button button-login" name="commit" type="submit" value="Login"/>
                    </div>
                    <p style="float: left">
                        <input name="user_session[remember_me]" type="hidden" value="0"/>
                        <input checked="checked" id="user_session_remember_me" name="user_session[remember_me]" type="checkbox" value="1"/>
                        <label for="user_session_remember_me">Remember me</label>
                    </p>
                    <p style="float:right">
                        <a href="resetpassword" class="new_password">Forgot password</a>
                    </p>
                </form> 
            </div>
        </#if>
    
        <#if facebookLoginEnabled == true || netIQLoginEnabled == true || omniLoginEnabled == true>
            <div class="login-box">
                <h4>Log in with</h4>
                <#if facebookLoginEnabled == true>
                        <form action="fblogin" class="new_user_session" name="fbgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                            <input name="commit" type="submit" value="Facebook" class="button button-login"/>
                        </form>
                </#if>
                <#if netIQLoginEnabled == true>
                    <div class="login-page-type" data-title="NetIQ login" id="ssoLoginNetIQ">
                        <form action="netiqlogin" class="new_user_session" name="netiqgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                            <input name="commit" type="submit" value="${netIQtext!NetIQ}" class="button button-login"/>
                        </form>
                    </div>
                </#if>
                <#if omniLoginEnabled == true>
                    <div class="login-page-type" data-title="BankID / minID login" id="ssoLoginOmni">
                        <div style="display: block;">
                            <p>
                                <a href=" "><img alt="Log in with minID" src="images/MinID.png"/></a>
                                <a href=" k"><img alt="Log in with BankID" src="images/BankID.png"/></a>
                            </p>
                        </div>
                    </div>
                </#if>
            </div>
        </#if>
    
        <#if openidLoginEnabled == true>
            <div class="login-box">
                <form action="action" class="new_user_session" name="getusertoken" method="post">
                    <div id="openid-login" >
                        <p>
                            <label for="user_session_openid_identifier">openID URL</label>
                            <input id="user_session_openid_identifier" name="user_session[openid_identifier]" size="30" type="text"/>
                            <input class="button button-login" name="commit" type="submit" value="Login"/>
                        </p>
                    </div>
                    <p id="remember" style="float: left;">
                        <input name="user_session[remember_me]" type="hidden" value="0"/>
                        <input checked="checked" id="user_session_remember_me" name="user_session[remember_me]" type="checkbox" value="1"/>
                        <label for="user_session_remember_me">Remember me</label>
                    </p>
                    <p style="float:right">
                        <a href="resetpassword" class="new_password">Forgot password</a>
                    </p>
                </form>
            </div> 
        </#if>

        <#if signupEnabled == true>
             <p id="signup">Not registered? <a href="signup">Register here!</a></p>
        </#if>

    </div>
</body>
</html>
