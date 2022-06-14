<!DOCTYPE html>
<html>
<head>
    <title>SSO Login</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <link rel="stylesheet" href="css/whydah.css" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
    <!--
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.3.0/font/bootstrap-icons.css" />
    -->
</head>
<body>
    
    <div id="page-content">
        <div id="logo">
            <img src="${logoURL!}" alt="Site logo"/><br>
        </div>
        <#if loginError??>
            <p class="error">${loginError!}</p>
        </#if>
            
        
            <div class="login-box">
            <#if redirectURI??>
            	<#if "${redirectURI}"?contains("oauth2/user?oauth_session=")>
            	   <a href="${redirectURI}&cancelled=true"><img src="/sso/images/history-back.svg" alt="back" /></a>
            	<#else>
            	    <a href="${redirectURI!"welcome"}"><img src="/sso/images/history-back.svg" alt="back" /></a>
            	</#if>
            </#if>	
            	
                <#if userpasswordLoginEnabled == true>
                <form action="action" class="new_user_session" name="getusertoken" method="post">
                    <div id="normal-login">
                        <h4><label for="user_session_login">Username</label></h4>
                        <input id="user_session_login" name="user" type="text" placeholder="Username" autofocus>
                        <h4><label for="user_session_password">Password</label></h4>
                        <input id="user_session_password" name="password" type="password" autocomplete="off" placeholder="Password">
                         <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                        <input type="hidden" name="redirectURI" value="${redirectURI!"welcome"}">
                        <input class="button button-login" name="commit" type="submit" value="Login"/>
                    </div>
                    <p style="float: left">
                        <input name="user_session[remember_me]" type="hidden" value="0"/>
                        <input checked="checked" id="user_session_remember_me" name="user_session[remember_me]" type="checkbox" value="1"/>
                        <label for="user_session_remember_me">Remember me</label>
                    </p>
                    <p style="float:right">
                    	<#if redirectURI??>
                    		<a href="resetpassword?redirectURI=${redirectURI}" class="new_password">Forgot password</a>		        	         
			            <#else>
			            	<a href="resetpassword" class="new_password">Forgot password</a>
			            </#if>
                    </p>
                    <br style="clear: both;"/><br/>
                    <#if signupEnabled == true>
        	            <#if redirectURI??>
        	                <p id="signup">Not registered? <a href="signup?redirectURI=${redirectURI}">Register here!</a></p>             
                        <#else>
            	            <p id="signup">Not registered? <a href="signup">Register here!</a></p>
                        </#if>             
                    </#if>
                </form> 
                </#if>
                <br/><hr/>
                <#if googleLoginEnabled == true || netIQLoginEnabled == true || rebelLoginEnabled == true || facebookLoginEnabled == true>
                <h4>OR Login with</h4>
                </#if>
                <#if googleLoginEnabled == true>
                <form action="googlelogin" class="new_user_session" name="googlelogin" method="post">
                    <#if redirectURI??>
                      <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                    </#if>
                    <input type="hidden" name="loginusername" value="" id="v2_gg_loginusername" />
                    <input name="commit" type="submit" value="Google" class="button button-login button-google"/>
                 </form>
                 </#if>
                 
                 <#if facebookLoginEnabled == true>
                        <form action="fblogin" class="new_user_session" name="fbgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                            <input name="commit" type="submit" value="Facebook" class="button button-login button-facebook"/>
                        </form>
                </#if>
                <#if netIQLoginEnabled == true>
                    <div class="login-page-type" data-title="NetIQ login" id="ssoLoginNetIQ">
                        <form action="netiqlogin" class="new_user_session" name="netiqgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                            <input name="commit" type="submit" value="NetIQ" class="button button-login button-netiq"/>
                        </form>
                    </div>
                </#if>
                
               
                <#list whydahLoginIntegrationProviders>
					    <#items as provider>
					      <#if provider.enabled == true>
						    <form action="whydahlogin/${provider.provider}" class="new_user_session" method="post">
		                        <#if redirectURI??>
		                          <input type="hidden" name="redirectURI" value="${redirectURI}"/>
		                        </#if>
		                        <input type="hidden" name="loginusername" value="" id="v2_rebel_loginusername" />
		                        <input name="commit" type="submit" value="${provider.displayText}" class="button button-login button-${provider.provider}"/>
	                    	</form>  
	                      </#if>
					    </#items>
				
				</#list>
                 
            </div>
            
           
        <#if personasshortcutEnabled == true>
            <br/><br/><hr><br/>
            <div class="login-box">
             		<h4>Personas login shortcuts</h4>
                
                	<#list personas>
					    <#items as persona>
					        <form action="action" class="new_user_session personaform" name="getusertoken" method="post">
		                        <input id="user_session_login" name="user" type="hidden" value="${persona.userName}" >
		                        <input id="user_session_password" name="password" type="hidden" value="${persona.password}">
		                        <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
		                        <input type="hidden" name="redirectURI" value="${redirectURI!"welcome"}">
		                        <input id="user_session_remember_me" name="user_session[remember_me]" type="hidden" value="1"/>
		                        <input class="button button-login" name="commit" type="submit" value="${persona.displayText}"/>                                        
		                     </form>
		                    
					    </#items>
					  
					    <#else>
					    	<p>No personas</p>
					</#list>


            </div> 
        </#if>

       

    </div>
</body>
</html>
