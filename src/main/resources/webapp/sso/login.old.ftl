<!DOCTYPE html>
<html>
<head>
    <title>SSO Login</title>
    <meta charset="utf-8"/>
    
    
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css">
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
                
                <#if googleLoginEnabled == true>
                <form action="googlelogin" class="new_user_session" name="googlelogin" method="post">
                    <#if redirectURI??>
                      <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                    </#if>
                    <input type="hidden" name="loginusername" value=""  />
                    <button class="customBtn" type="submit">
				      <span class="icon" style="background: url('/sso/images/g-normal.png') transparent 5px 50% no-repeat;"></span>
				      <span class="buttonText">Continue with Google</span>
				    </button>
				    
                    
                 </form>
                 </#if>
                 
                 <#if microsoftLoginEnabled == true>
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
                 
               
                 <#if facebookLoginEnabled == true>
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
                <#if netIQLoginEnabled == true>
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
                
               
                <#list whydahLoginIntegrationProviders>
					    <#items as provider>
					      <#if provider.enabled == true>
						    <form action="whydahlogin/${provider.provider}" class="new_user_session" method="post">
		                        <#if redirectURI??>
		                          <input type="hidden" name="redirectURI" value="${redirectURI}"/>
		                        </#if>
		                        <input type="hidden" name="loginusername" value=""  />
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

                <#if oidcProviderVippsEnabled?? && oidcProviderVippsEnabled == true>
                    <a href="vipps/login"><img alt="Log in with Vipps button"  src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjE0IiBoZWlnaHQ9IjQzIiB2aWV3Qm94PSIwIDAgMjE0IDQzIiBmaWxsPSJub25lIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgo8cmVjdCB3aWR0aD0iMjE0IiBoZWlnaHQ9IjQzIiByeD0iNSIgZmlsbD0iI0ZGNUIyNCIvPgo8cGF0aCBkPSJNMTguNjQzNSAyOFYxNC45NjUxSDIwLjk3NFYyNi4wMzk4SDI2Ljk1NFYyOEgxOC42NDM1Wk0zMi44NDI0IDI4LjE5ODdDMjkuODc5NSAyOC4xOTg3IDI4LjA5MDkgMjYuMjY1NiAyOC4wOTA5IDIzLjA2NzlWMjMuMDQ5OEMyOC4wOTA5IDE5Ljg3OTIgMjkuOTA2NiAxNy45MzcgMzIuODQyNCAxNy45MzdDMzUuNzg3MiAxNy45MzcgMzcuNTkzOCAxOS44NzAxIDM3LjU5MzggMjMuMDQ5OFYyMy4wNjc5QzM3LjU5MzggMjYuMjY1NiAzNS43OTYyIDI4LjE5ODcgMzIuODQyNCAyOC4xOTg3Wk0zMi44NDI0IDI2LjM3NEMzNC40MDUxIDI2LjM3NCAzNS4yOTA0IDI1LjE1NDUgMzUuMjkwNCAyMy4wNzY5VjIzLjA1ODhDMzUuMjkwNCAyMC45ODEyIDM0LjM5NjEgMTkuNzUyNyAzMi44NDI0IDE5Ljc1MjdDMzEuMjc5NiAxOS43NTI3IDMwLjM4NTQgMjAuOTgxMiAzMC4zODU0IDIzLjA1ODhWMjMuMDc2OUMzMC4zODU0IDI1LjE1NDUgMzEuMjc5NiAyNi4zNzQgMzIuODQyNCAyNi4zNzRaTTQzLjg5NzggMzEuNDY4OEM0MS4yOTYyIDMxLjQ2ODggMzkuNzMzNCAzMC4zNTc3IDM5LjUwNzYgMjguNjk1NkwzOS41MTY2IDI4LjY2ODVINDEuNzY1OUw0MS43NzUgMjguNjk1NkM0MS45Mjg1IDI5LjMyNzkgNDIuNjc4MyAyOS43NzA1IDQzLjkzMzkgMjkuNzcwNUM0NS40Njk1IDI5Ljc3MDUgNDYuMzYzOCAyOS4wNTY5IDQ2LjM2MzggMjcuNzkyMlYyNS45MDQzSDQ2LjIxMDNDNDUuNjQxMiAyNi45NjEyIDQ0LjU2NjIgMjcuNTM5MyA0My4yMDIyIDI3LjUzOTNDNDAuNyAyNy41MzkzIDM5LjE0NjMgMjUuNjA2MiAzOS4xNDYzIDIyLjgwNTlWMjIuNzg3OEMzOS4xNDYzIDE5LjkzMzMgNDAuNyAxNy45NjQxIDQzLjI0NzQgMTcuOTY0MUM0NC42MTE0IDE3Ljk2NDEgNDUuNjc3MyAxOC42MzI2IDQ2LjIzNzQgMTkuNzUyN0g0Ni4zNTQ4VjE4LjEyNjdINDguNjA0MVYyNy43NTYxQzQ4LjYwNDEgMzAuMDMyNSA0Ni43NzkzIDMxLjQ2ODggNDMuODk3OCAzMS40Njg4Wk00My44OTc4IDI1Ljc1MDdDNDUuNDYwNSAyNS43NTA3IDQ2LjQgMjQuNTQ5MyA0Ni40IDIyLjgwNTlWMjIuNzg3OEM0Ni40IDIxLjA0NDQgNDUuNDUxNSAxOS44MzQgNDMuODk3OCAxOS44MzRDNDIuMzM1IDE5LjgzNCA0MS40NDk4IDIxLjA0NDQgNDEuNDQ5OCAyMi43ODc4VjIyLjgwNTlDNDEuNDQ5OCAyNC41NDkzIDQyLjMzNSAyNS43NTA3IDQzLjg5NzggMjUuNzUwN1pNNTUuNDA0OCAzMS40Njg4QzUyLjgwMzIgMzEuNDY4OCA1MS4yNDA1IDMwLjM1NzcgNTEuMDE0NiAyOC42OTU2TDUxLjAyMzcgMjguNjY4NUg1My4yNzI5TDUzLjI4MiAyOC42OTU2QzUzLjQzNTUgMjkuMzI3OSA1NC4xODUzIDI5Ljc3MDUgNTUuNDQwOSAyOS43NzA1QzU2Ljk3NjYgMjkuNzcwNSA1Ny44NzA4IDI5LjA1NjkgNTcuODcwOCAyNy43OTIyVjI1LjkwNDNINTcuNzE3M0M1Ny4xNDgyIDI2Ljk2MTIgNTYuMDczMiAyNy41MzkzIDU0LjcwOTIgMjcuNTM5M0M1Mi4yMDcgMjcuNTM5MyA1MC42NTMzIDI1LjYwNjIgNTAuNjUzMyAyMi44MDU5VjIyLjc4NzhDNTAuNjUzMyAxOS45MzMzIDUyLjIwNyAxNy45NjQxIDU0Ljc1NDQgMTcuOTY0MUM1Ni4xMTg0IDE3Ljk2NDEgNTcuMTg0MyAxOC42MzI2IDU3Ljc0NDQgMTkuNzUyN0g1Ny44NjE4VjE4LjEyNjdINjAuMTExMVYyNy43NTYxQzYwLjExMTEgMzAuMDMyNSA1OC4yODY0IDMxLjQ2ODggNTUuNDA0OCAzMS40Njg4Wk01NS40MDQ4IDI1Ljc1MDdDNTYuOTY3NSAyNS43NTA3IDU3LjkwNyAyNC41NDkzIDU3LjkwNyAyMi44MDU5VjIyLjc4NzhDNTcuOTA3IDIxLjA0NDQgNTYuOTU4NSAxOS44MzQgNTUuNDA0OCAxOS44MzRDNTMuODQyIDE5LjgzNCA1Mi45NTY4IDIxLjA0NDQgNTIuOTU2OCAyMi43ODc4VjIyLjgwNTlDNTIuOTU2OCAyNC41NDkzIDUzLjg0MiAyNS43NTA3IDU1LjQwNDggMjUuNzUwN1pNNjguNTU0NiAxNi40NzM2QzY3LjgxMzkgMTYuNDczNiA2Ny4yMDg2IDE1Ljg4NjUgNjcuMjA4NiAxNS4xNDU4QzY3LjIwODYgMTQuNDE0MSA2Ny44MTM5IDEzLjgxNzkgNjguNTU0NiAxMy44MTc5QzY5LjI4NjMgMTMuODE3OSA2OS44OTE1IDE0LjQxNDEgNjkuODkxNSAxNS4xNDU4QzY5Ljg5MTUgMTUuODg2NSA2OS4yODYzIDE2LjQ3MzYgNjguNTU0NiAxNi40NzM2Wk02Ny40MjU0IDI4VjE4LjEyNjdINjkuNjc0N1YyOEg2Ny40MjU0Wk03Mi4yMzg5IDI4VjE4LjEyNjdINzQuNDg4MVYxOS42MzUzSDc0LjY0MTdDNzUuMTExNCAxOC41Nzg0IDc2LjA1OTkgMTcuOTM3IDc3LjQ4NzIgMTcuOTM3Qzc5LjY5MTMgMTcuOTM3IDgwLjg5MjcgMTkuMjY0OSA4MC44OTI3IDIxLjYxMzVWMjhINzguNjQzNFYyMi4xMzc1Qzc4LjY0MzQgMjAuNjAxOCA3OC4wMjAxIDE5LjgyNSA3Ni42NTYxIDE5LjgyNUM3NS4zMTkyIDE5LjgyNSA3NC40ODgxIDIwLjc2NDQgNzQuNDg4MSAyMi4yNDU4VjI4SDcyLjIzODlaTTgzLjM0ODQgMjhWMTguMTI2N0g4NS41OTc3VjE5LjYzNTNIODUuNzUxM0M4Ni4yMjEgMTguNTc4NCA4Ny4xNjk1IDE3LjkzNyA4OC41OTY3IDE3LjkzN0M5MC44MDA4IDE3LjkzNyA5Mi4wMDIyIDE5LjI2NDkgOTIuMDAyMiAyMS42MTM1VjI4SDg5Ljc1M1YyMi4xMzc1Qzg5Ljc1MyAyMC42MDE4IDg5LjEyOTcgMTkuODI1IDg3Ljc2NTcgMTkuODI1Qzg2LjQyODggMTkuODI1IDg1LjU5NzcgMjAuNzY0NCA4NS41OTc3IDIyLjI0NThWMjhIODMuMzQ4NFpNOTkuMTkwMSAyOFYxOC4xMjY3SDEwMS40MzlWMTkuNjYyNEgxMDEuNTkzQzEwMi4wMTggMTguNTYwMyAxMDMuMDExIDE3LjkzNyAxMDQuMzAzIDE3LjkzN0MxMDUuNjQgMTcuOTM3IDEwNi42MTUgMTguNjIzNSAxMDcuMDQgMTkuNzE2NkgxMDcuMTk0QzEwNy42NzIgMTguNjQxNiAxMDguODEgMTcuOTM3IDExMC4xODQgMTcuOTM3QzExMi4xNzEgMTcuOTM3IDExMy4zNjMgMTkuMTU2NSAxMTMuMzYzIDIxLjIwN1YyOEgxMTEuMTE0VjIxLjc1ODFDMTExLjExNCAyMC40NzUzIDExMC41MjcgMTkuODI1IDEwOS4zMjUgMTkuODI1QzEwOC4xNTEgMTkuODI1IDEwNy4zOTIgMjAuNzAxMiAxMDcuMzkyIDIxLjgzMDNWMjhIMTA1LjE0M1YyMS41OTU1QzEwNS4xNDMgMjAuNTAyNCAxMDQuNDY2IDE5LjgyNSAxMDMuMzYzIDE5LjgyNUMxMDIuMjUyIDE5LjgyNSAxMDEuNDM5IDIwLjc2NDQgMTAxLjQzOSAyMS45OTI5VjI4SDk5LjE5MDFaTTEyMC4wNTYgMjguMTk4N0MxMTcuMTIgMjguMTk4NyAxMTUuMzQgMjYuMjI5NSAxMTUuMzQgMjMuMDg1OVYyMy4wNzY5QzExNS4zNCAxOS45Njk1IDExNy4xMzggMTcuOTM3IDExOS45NDcgMTcuOTM3QzEyMi43NTYgMTcuOTM3IDEyNC40OTEgMTkuOTA2MiAxMjQuNDkxIDIyLjg4NzJWMjMuNjI3OUgxMTcuNTlDMTE3LjYxNyAyNS4zOTg0IDExOC41NzQgMjYuNDI4MiAxMjAuMTAxIDI2LjQyODJDMTIxLjMyIDI2LjQyODIgMTIyLjAxNiAyNS44MTQgMTIyLjIzMyAyNS4zNjIzTDEyMi4yNiAyNS4yOTkxSDEyNC40MDFMMTI0LjM3MyAyNS4zODA0QzEyNC4wNTcgMjYuNjU0MSAxMjIuNzM4IDI4LjE5ODcgMTIwLjA1NiAyOC4xOTg3Wk0xMTkuOTc0IDE5LjY5ODVDMTE4LjcxOSAxOS42OTg1IDExNy43NzkgMjAuNTQ3NiAxMTcuNjA4IDIyLjExOTRIMTIyLjI5NkMxMjIuMTQyIDIwLjUwMjQgMTIxLjIzIDE5LjY5ODUgMTE5Ljk3NCAxOS42OTg1Wk0xMzAuMDk5IDI4LjE2MjZDMTI3LjYyNCAyOC4xNjI2IDEyNi4wNTIgMjYuMjAyNCAxMjYuMDUyIDIzLjA3NjlWMjMuMDU4OEMxMjYuMDUyIDE5LjkxNTMgMTI3LjU5NyAxNy45NjQxIDEzMC4wOTkgMTcuOTY0MUMxMzEuNDU0IDE3Ljk2NDEgMTMyLjU4MyAxOC42MzI2IDEzMy4wOTggMTkuNjk4NUgxMzMuMjUyVjE0LjMwNTdIMTM1LjUxVjI4SDEzMy4yNTJWMjYuNDU1M0gxMzMuMDk4QzEzMi41NTYgMjcuNTMwMyAxMzEuNDkgMjguMTYyNiAxMzAuMDk5IDI4LjE2MjZaTTEzMC44MDQgMjYuMjY1NkMxMzIuMzM5IDI2LjI2NTYgMTMzLjI4OCAyNS4wNTUyIDEzMy4yODggMjMuMDc2OVYyMy4wNTg4QzEzMy4yODggMjEuMDgwNiAxMzIuMzMgMTkuODYxMSAxMzAuODA0IDE5Ljg2MTFDMTI5LjI3NyAxOS44NjExIDEyOC4zMzggMjEuMDcxNSAxMjguMzM4IDIzLjA1ODhWMjMuMDc2OUMxMjguMzM4IDI1LjA2NDIgMTI5LjI2OCAyNi4yNjU2IDEzMC44MDQgMjYuMjY1NloiIGZpbGw9IndoaXRlIi8+CjxwYXRoIGZpbGwtcnVsZT0iZXZlbm9kZCIgY2xpcC1ydWxlPSJldmVub2RkIiBkPSJNMTk3IDIwLjY2MzZDMTk2LjM5MiAxOC4zNDUyIDE5NC45MTcgMTcuNDI0OSAxOTIuOTAzIDE3LjQyNDlDMTkxLjI3MiAxNy40MjQ5IDE4OS4yMjQgMTguMzQ1MiAxODkuMjI0IDIwLjU2MTNDMTg5LjIyNCAyMS45OTMgMTkwLjIxMyAyMy4xMTgyIDE5MS44MjcgMjMuNDA4MUwxOTMuMzU1IDIzLjY4MDZDMTk0LjM5NiAyMy44NjggMTk0LjY5MSAyNC4yNjAyIDE5NC42OTEgMjQuNzg4N0MxOTQuNjkxIDI1LjM4NTMgMTk0LjA0OSAyNS43MjYxIDE5My4wOTUgMjUuNzI2MUMxOTEuODQ1IDI1LjcyNjEgMTkxLjA2NCAyNS4yODMgMTkwLjk0MiAyNC4wMzg1TDE4OC43MzggMjQuMzc5NUMxODkuMDg1IDI2Ljc4MjggMTkxLjIzNyAyNy43NzE4IDE5My4xODEgMjcuNzcxOEMxOTUuMDIxIDI3Ljc3MTggMTk2Ljk4MiAyNi43MTQ3IDE5Ni45ODIgMjQuNTg0MUMxOTYuOTgyIDIzLjEzNSAxOTYuMDk3IDIyLjA3ODUgMTk0LjQ0OCAyMS43NzEyTDE5Mi43NjUgMjEuNDY0N0MxOTEuODI3IDIxLjI5NDMgMTkxLjUxNSAyMC44MzQgMTkxLjUxNSAyMC4zOTA4QzE5MS41MTUgMTkuODI4MiAxOTIuMTIyIDE5LjQ3MDYgMTkyLjk1NiAxOS40NzA2QzE5NC4wMTQgMTkuNDcwNiAxOTQuNzYxIDE5LjgyODIgMTk0Ljc5NSAyMS4wMDQ0TDE5NyAyMC42NjM2Wk0xNDcuOTk5IDI0LjMxMUwxNTAuMjkgMTcuNjgwNUgxNTIuOThMMTQ4Ljk4OCAyNy41MTU3SDE0Ni45OTJMMTQzIDE3LjY4MDZIMTQ1LjY5TDE0Ny45OTkgMjQuMzExWk0xNjIuMDc2IDIwLjQ5MzFDMTYyLjA3NiAyMS4yNzcxIDE2MS40NTEgMjEuODIyNiAxNjAuNzIyIDIxLjgyMjZDMTU5Ljk5MyAyMS44MjI2IDE1OS4zNjggMjEuMjc3MSAxNTkuMzY4IDIwLjQ5MzFDMTU5LjM2OCAxOS43MDkgMTU5Ljk5MyAxOS4xNjM3IDE2MC43MjIgMTkuMTYzN0MxNjEuNDUxIDE5LjE2MzcgMTYyLjA3NiAxOS43MDkgMTYyLjA3NiAyMC40OTMxSDE2Mi4wNzZaTTE2Mi40OTIgMjMuOTcwNUMxNjEuNTkgMjUuMTI5MyAxNjAuNjM1IDI1LjkzMDUgMTU4Ljk1MSAyNS45MzA1QzE1Ny4yMzMgMjUuOTMwNSAxNTUuODk3IDI0LjkwNzggMTU0Ljg1NSAyMy40MDc5QzE1NC40MzkgMjIuNzk0MiAxNTMuNzk2IDIyLjY1NzkgMTUzLjMyOCAyMi45ODE4QzE1Mi44OTQgMjMuMjg4NyAxNTIuNzkgMjMuOTM2NCAxNTMuMTg5IDI0LjQ5OUMxNTQuNjI5IDI2LjY2MzcgMTU2LjYyNSAyNy45MjQ5IDE1OC45NTEgMjcuOTI0OUMxNjEuMDg2IDI3LjkyNDkgMTYyLjc1MyAyNi45MDIzIDE2NC4wNTQgMjUuMTk3N0MxNjQuNTQgMjQuNTY3MSAxNjQuNTIzIDIzLjkxOTQgMTY0LjA1NCAyMy41NjE0QzE2My42MiAyMy4yMjAxIDE2Mi45NzggMjMuMzQgMTYyLjQ5MiAyMy45NzA1Wk0xNjguNDgxIDIyLjU3MjZDMTY4LjQ4MSAyNC41ODQxIDE2OS42NjIgMjUuNjQxIDE3MC45ODEgMjUuNjQxQzE3Mi4yMyAyNS42NDEgMTczLjUxNSAyNC42NTIyIDE3My41MTUgMjIuNTcyNkMxNzMuNTE1IDIwLjUyNjkgMTcyLjIzIDE5LjUzODUgMTcwLjk5OCAxOS41Mzg1QzE2OS42NjIgMTkuNTM4NSAxNjguNDgxIDIwLjQ3NiAxNjguNDgxIDIyLjU3MjZaTTE2OC40ODEgMTkuMDQ0NVYxNy42OTc1SDE2Ni4wMzRWMzAuOTI0OUgxNjguNDgxVjI2LjIyMDJDMTY5LjI5NyAyNy4zMTEzIDE3MC4zNTYgMjcuNzcxOCAxNzEuNTUzIDI3Ljc3MThDMTczLjc5MyAyNy43NzE4IDE3NS45NzkgMjYuMDMzIDE3NS45NzkgMjIuNDUzNUMxNzUuOTc5IDE5LjAyNzEgMTczLjcwNiAxNy40MjUxIDE3MS43NjIgMTcuNDI1MUMxNzAuMjE3IDE3LjQyNTEgMTY5LjE1OCAxOC4xMjM4IDE2OC40ODEgMTkuMDQ0NVpNMTgwLjIzMyAyMi41NzI2QzE4MC4yMzMgMjQuNTg0MSAxODEuNDEzIDI1LjY0MSAxODIuNzMyIDI1LjY0MUMxODMuOTgyIDI1LjY0MSAxODUuMjY2IDI0LjY1MjIgMTg1LjI2NiAyMi41NzI2QzE4NS4yNjYgMjAuNTI2OSAxODMuOTgyIDE5LjUzODUgMTgyLjc0OSAxOS41Mzg1QzE4MS40MTMgMTkuNTM4NSAxODAuMjMyIDIwLjQ3NiAxODAuMjMyIDIyLjU3MjZIMTgwLjIzM1pNMTgwLjIzMyAxOS4wNDQ1VjE3LjY5NzVIMTgwLjIzMkgxNzcuNzg1VjMwLjkyNDlIMTgwLjIzMlYyNi4yMjAyQzE4MS4wNDggMjcuMzExMyAxODIuMTA3IDI3Ljc3MTggMTgzLjMwNCAyNy43NzE4QzE4NS41NDQgMjcuNzcxOCAxODcuNzMxIDI2LjAzMyAxODcuNzMxIDIyLjQ1MzVDMTg3LjczMSAxOS4wMjcxIDE4NS40NTcgMTcuNDI1MSAxODMuNTEzIDE3LjQyNTFDMTgxLjk2OCAxNy40MjUxIDE4MC45MSAxOC4xMjM4IDE4MC4yMzMgMTkuMDQ0NVoiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo="/></a>
                </#if>
                <#if oidcProviderGoogleEnabled?? && oidcProviderGoogleEnabled == true>
                    <form action="google/login" class="new_user_session" name="google/login" method="post">
                        <#if redirectURI??>
                            <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                        </#if>
                        <input type="hidden" name="loginusername" value=""  />
                        <button class="customBtn" type="submit">
                            <span class="icon" style="background: url('/sso/images/g-normal.png') transparent 5px 50% no-repeat;"></span>
                            <span class="buttonText">Continue with Google</span>
				        </button>
                    </form>
                </#if>
                <#if oidcProviderMicrosoftEnabled?? && oidcProviderMicrosoftEnabled == true>
                    <form action="microsoft/login" class="new_user_session" name="microsoft/login" method="post">
                        <#if redirectURI??>
                            <input type="hidden" name="redirectURI" value="${redirectURI}"/>
                        </#if>

                        <button class="customBtn" type="submit">
                            <span class="icon" style="background: url('/sso/images/microsoft-normal.png') transparent 5px 50% no-repeat;"></span>
                            <span class="buttonText">Continue with Microsoft</span>
				        </button>

                    </form>
                </#if>

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
