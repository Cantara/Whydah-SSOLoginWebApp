<!DOCTYPE html>
<html>
<head>
    <title>SSO Login</title>
    
    <link rel="stylesheet" href="/sso/css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=320, initial-scale=1, maximum-scale=1"/>
    <script src="/sso/js/jquery-1.11.1.min.js"></script>
    <link rel="icon" href="/sso/images/favicon.ico" type="image/x-icon"/>
</head>

<body>
    <div id="page-content">
       
      <div id="logo">
        <img src="${logoURL!}" alt="Site logo"/><br>
      </div>
      
      <p class="description-text">
        We will use your information provided as follows. Please confirm
      </p>
                     

        <div class="login-box">

            <#if signupError??>
              <p class="error">${signupError!}</p>
            </#if>
			
			<#assign pa = "/sso/${provider}/basicinfo_confirm">
            <form method="POST" class="new_user_session" action="${pa}" accept-charset="utf-8">
                <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                <input name="redirectURI" type="hidden" value="${redirectURI!}">
                <input name="username" type="hidden" value="${username!}"/>
                     
                <label for="username" class="label">${username!}</label>
                <br/>			
                <label for="firstName" class="label">Fornavn:</label>
                <input id="firstName" class="input" name="firstName" size="30" type="text" placeholder="Kari" value="${firstName!}"/>
                
                <label for="lastName" class="label">Etternavn:</label>
                <input id="lastName" class="input" name="lastName" size="30" type="text" placeholder="Normann" value="${lastName!}"/>
                
                <label for="email" class="label">E-post:</label>
                <#if jwtClaimAsUserName == "email">
				  	<input id="email" class="input" name="email" size="30" type="text" placeholder="Your email" value="${email!}" readonly/>
				<#else>
					<input id="email" class="input" name="email" size="30" type="text" placeholder="Your email" value="${email!} "/>
                 </#if>
                
                <label for="cellphone" class="label">Mobilnummer (required):</label>
                <#if jwtClaimAsUserName == "phone_number">
                	<input id="cellPhone" class="input" name="cellPhone" size="30" type="text" placeholder="+47 999 99 999" value="${cellPhone!}" readonly/>
                <#else>
                	<input id="cellPhone" class="input" name="cellPhone" size="30" type="text" placeholder="+47 999 99 999" value="${cellPhone!}"/>
                </#if>
                <input id="confirm" class="button button-login" name="commit" type="submit" value="Confirm"/>
            </form>
        </div>
    </div>
</div>

 
<script type="text/JavaScript">  
function checkSelect(checkboxElem) {
		var checkboxes = document.getElementsByClassName('ck_select');
		var count_checked = 0;
		var count_unchecked = 0;
		for (var i = 0; i < checkboxes.length; i++){
            if(checkboxes[i].checked) {
            	count_checked++;
            } else {
            	count_unchecked++;
            }
        }
        if(count_checked === checkboxes.length) {
        	document.getElementById('checkbox').checked = true;
        }
        
        if(count_unchecked === checkboxes.length) {
        	document.getElementById('checkbox').checked = false;
        } 
            	
	}
	function toggleCheck(checkboxElem) {
		 var checkboxes = document.getElementsByClassName('ck_select');
		 if (checkboxElem.checked) {    
		    
            for (var i = 0; i < checkboxes.length; i++){
                checkboxes[i].checked = true;
            }
                
		 } else {
		   
            for (var i = 0; i < checkboxes.length; i++){
                checkboxes[i].checked = false;
            }
		 }
	} 
(function() {   

 	let confirmbtn = document.getElementById('confirm');
 	
	let inputs = document.querySelectorAll('.input');
	
	inputs.forEach(inp => inp.addEventListener('input', checkInputs));
  
	function checkInputs(event) {
    	const inputArray = [...inputs];
    	const missingInput = inputArray.filter(inp => inp.value.length === 0).length > 0;
    	toggleButton(missingInput);
 	}
  
	function toggleButton(toggle) {
    	confirmbtn.disabled = toggle;
    	confirmbtn.style.cursor = toggle ? "not-allowed" : "pointer";
  	}
  	
  	checkInputs();
  
})();   
</script> 
</body>
</html>