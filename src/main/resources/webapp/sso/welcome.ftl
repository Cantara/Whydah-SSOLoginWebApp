<!DOCTYPE html>
<html>
<head>
    <title>Whydah</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <link rel="stylesheet" href="css/whydah.css?20141216" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
    <script src="js/jquery-1.11.1.min.js"></script>
</head>
<body>

    <div id="page-content">

        <div id="logo">
            <img src="${logoURL}" alt="Site logo"/>
            <h2>Welcome ${realname!"Whydah user"}</h2>
        </div>

        <div class="wide-box">
            <h3>Your roles</h3>
            <table id="roles">
                <thead>
                    <tr>
                        <th>Application</th>
                        <th>Organization</th>
                        <th>Role</th>
                        <th>Value</th>
                    </tr>
                </thead>
                <tbody>

                </tbody>
            </table>

            <h3>Your profile</h3>
            <strong>Phone:</strong> ${phonenumber!"Whydah number"} <br/>
            <strong>Name:</strong> ${realname!"Whydah user"} <br/>
            <strong>Email:</strong> ${email!"Whydah email"} <br/>
            <strong>Security level:</strong> ${securitylevel!"0"} <br/>
            <br/>
            <p><a href="logout" class="button">Log out</a></p>
            <p><small>Logout affects all Whydah-connected services</small></p>

            <br/>
            <h3>Elevate your session security</h3>
            <div id="secureproviders">
               <img src="images/BankID.png" alt="BankID"/>
               <img src="images/MinID.png" alt="MinID"/>
               <img src="images/icon-multifactor-authentication.png" alt="Multi-factor Authentication"/>
            </div>
            <br/><br/>

            <small>Whydah, SSO WebApp version: ${version!"<i>Unknown</i>"} </small>
        </div>

    </div>

    <script>
        var appLinks = ${appLinks!"{}"};
        var userTokenXml = '${usertoken?replace("(\r\n)+", "",'r')}';
        var $userToken = $( $.parseXML(userTokenXml) );
        var apps = $userToken.find('application');
        var roleTableContent = '';
        $.each(apps, function(index, app){
            var $app = $(app);
            var a = {};
            a.applicationName = $app.find('applicationName').text();
            if(appLinks[a.applicationName]) a.applicationName = '<a href="'+appLinks[a.applicationName]+'">'+a.applicationName+'</a>';
            a.organizationName = $app.find('organizationName').text();
            a.roleName = $app.find('role').attr('name');
            a.roleValue = $app.find('role').attr('value');
            roleTableContent += '<tr><td data-th="Application">'+a.applicationName
                +'</td><td data-th="Organization">'+a.organizationName
                +'</td><td data-th="Role">'+a.roleName
                +'</td><td data-th="Value">'+a.roleValue+'</td></tr>';
        });
        roleTableContent = roleTableContent || '<tr><td colspan="4">No roles found.</td></tr>';
        $('#roles tbody').html(roleTableContent);
    </script>

</body>
</html>