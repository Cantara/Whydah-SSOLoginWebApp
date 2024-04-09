<!DOCTYPE html>
<html>
<head>
<title>Whydah Login</title>
<link rel = "stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=320, initial-scale=1, maximum-scale=1"/>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
</head>
<body>
    <div id="page-content">
        <div id="signup-page">
            <div id="logo">
            	<img src="${logoURL!'/sso/images/single-dot.gif'}" alt="Whydah User Registration"/>
                <h2>Register new user</h2>
            </div>

        <div class="login-box">
			<a href="${redirectURI}"><img src="/sso/images/history-back.svg" alt="back" /></a>
            <#if error??><p class="error">${error!}</p></#if>

            <form method="POST" class="new_user_session" accept-charset="utf-8">
                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                <input type="hidden" name="redirectURI" value="${redirectURI}"/>

                <h4><label for="username">Username (*):</label></h4>
                <input id="username" name="username" size="30" type="text" placeholder="Username" required minlength="3" value="${username!}"/>

                <h4><label for="firstname">First Name (*):</label></h4>
                <input id="firstname" name="firstname" size="30" type="text" placeholder="Firstname" value="${firstname!}" required/>

                <h4><label for="lastname">Last Name (*):</label></h4>
                <input id="lastname" name="lastname" size="30" type="text" placeholder="Lastname" value="${lastname!}" required/>

                <h4><label for="user">Email (*):</label></h4>
                <#if useremail?? && useremail?has_content>
               		 <input id="useremail" name="useremail" size="30" type="email" placeholder="Email" value="${useremail!}" readonly/>
                <#else>
                 	<input id="useremail" name="useremail" size="30" type="email" placeholder="Email" value="" required/>
                
                </#if>
                

                <h4><label for="cellphone">Cell phone: (optional)</label></h4>
                <input id="cellphone" name="cellphone" size="30" type="text" placeholder="Cellphone" value="${cellphone!}"/>
				<!--
                <p>You will receive an email with instructions of how to set your password.</p> !-->

                <input class="button button-login" name="commit" type="submit" value="Register"/>

            </form>
            
            <br/><hr/>
                
                <#if googleLoginEnabled == true && signuppageGoogleOn == true>
                <form action="googlelogin" class="new_user_session" name="googlelogin" method="post">
                    <#if redirectURI??>
                      <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                    </#if>
                    <input type="hidden" name="loginusername" value="" />
                    <button class="customBtn" type="submit">
				      <span class="icon" style="background: url('/sso/images/g-normal.png') transparent 5px 50% no-repeat;"></span>
				      <span class="buttonText">Continue with Google</span>
				    </button>
				    
                    
                 </form>
                 </#if>
                
                <#if microsoftLoginEnabled == true  && signuppageMicrosoftOn == true>
                <form action="aadprelogin" class="new_user_session" name="aadprelogin" method="post">
                    <#if redirectURI??>
                      <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                    </#if>
                   
                    <button class="customBtn" type="submit">
				      <span class="icon" style="background: url('/sso/images/microsoft-normal.png') transparent 5px 50% no-repeat;"></span>
				      <span class="buttonText">Continue with Azure AD</span>
				    </button>
				    
                 </form>
                 </#if>
                 
                 <#if facebookLoginEnabled == true && signuppageFacebookOn == true>
                        <form action="fblogin" class="new_user_session" name="fbgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                         <button class="customBtn" type="submit">				     
					       <span class="buttonText">Continue with Facebook</span>
					    </button>
				    
                         <!--<input name="commit" type="submit" value="Facebook" class="button button-login button-facebook"/> -->
                        </form>
                </#if>
                <#if netIQLoginEnabled == true && signuppageNetIQOn == true>
                    <div class="login-page-type" data-title="NetIQ login" id="ssoLoginNetIQ">
                        <form action="netiqlogin" class="new_user_session" name="netiqgetusertoken" method="post">
                            <#if redirectURI??>
                                <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                            </#if>
                             <button class="customBtn" type="submit">				     
					       		<span class="buttonText">Continue with NetIQ</span>
					    	 </button>
					    	 <!--
                            <input name="commit" type="submit" value="NetIQ" class="button button-login button-netiq"/> -->
                        </form>
                    </div>
                </#if>
                
               <#if signuppageWhydahIntegrationProviderOn == true>
                <#list whydahLoginIntegrationProviders>
					    <#items as provider>
					      <#if provider.enabled == true>
						    <form action="whydahlogin/${provider.provider}" class="new_user_session" method="post">
		                        <#if redirectURI??>
		                          <input type="hidden" name="redirectURI" value="${redirectURI}"/>
		                        </#if>
		                        <input type="hidden" name="loginusername" value="" />
		                        <button class="customBtn" type="submit">
		                            <#if provider.logo??>
						            	<#if "${provider.logo}"?starts_with("data:image") || "${provider.logo}"?starts_with("http")>
						            	   <span class="icon" style="background: url('${provider.logo}') transparent 5px 50% no-repeat;"></span>					            	  
						            	<#else>
						            	   <span class="icon" style="background: url('/sso/images/${provider.logo}') transparent 5px 50% no-repeat;"></span>
						            	</#if>
						            </#if>	
		                        	
				      				<span class="buttonText">Continue with ${provider.displayText}</span>
		                        </button>
		                        <!--
		                        <input name="commit" type="submit" value="${provider.displayText}" class="button button-login button-${provider.provider}"/> -->
	                    	</form>  
	                      </#if>
					    </#items>
				
				</#list>
				</#if>
                 
            
        </div>
    </div>
</div>
</body>
</html>