<!DOCTYPE html>
<html>
<head>
    <title>O.ID Login</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" ></link>
    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" ></script>

    <link rel="stylesheet" href="css/whydah.css" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
    <meta name="detectify-verification" content="3696809a6c4e05d6f4385ce717fea51c"/>
    <META HTTP-EQUIV="x-content-type-options" CONTENT="nosniff">
    <META HTTP-EQUIV="x-frame-options" CONTENT="SAMEORIGIN">
    <META HTTP-EQUIV="x-xss-protection" CONTENT="1; mode=block">


</head>
<body>
<div >
    <div id="logo">
        <!--<img src="${logoURL!}" alt="Site logo"/><br> -->
    </div>
<#if loginError??>
    <p class="error">${loginError!}</p>
</#if>

    <div class="jumbotron container">
        <div class="row">

            <div class="col-xs-1 ">

            </div>
            <div class="col-xs-10">
                    <div class="col-xs-5 ">
                        <p class="header-text">Min informasjon</p>
                        <form action="crmdata" method="post">
                            <input name="CSRFtoken" type="hidden" value="${CSRFtoken!}">
                            <div class="form-inline">
                                <div class="form-group">
                                    <label class="sr-only" for="inputFirstname">Fornavn</label>
                                    <input type="text" class="form-control" id="inputFirstname" name="firstname" placeholder="Fornavn" value="${CRMcustomer.firstname!}">
                                </div>
                                <div class="form-group">
                                    <label class="sr-only" for="inputMiddlename">Mellomnavn</label>
                                    <input type="text" class="form-control" id="inputMiddlename" name="middlename" placeholder="Mellomnavn" value="${CRMcustomer.middlename!}">
                                </div>
                                <div class="form-group">
                                    <label class="sr-only" for="inputLastname">Etternavn</label>
                                    <input type="text" class="form-control" id="inputLastname" name="lastname" placeholder="Etternavn" value="${CRMcustomer.lastname!}">
                                </div>
                            </div>

                            <div class="form-inline">

                                    <label>Telefon</label>
                                    <ol class="list-group">
                                    <#list CRMcustomer.phonenumbers?keys as phoneKey>
                                        <li class="list-group-item">
                                            <label for="${phoneKey}_defaultPhoneLabel"><span class="label label-info">${phoneKey}</span></label>
                                            <#if !CRMcustomer.phonenumbers[phoneKey].verified><span class="glyphicon glyphicon-warning-sign text-danger" title="Ikke bekreftet"></span></#if>
                                            <input id="phone" name="${phoneKey}_phone" type="text" value="${CRMcustomer.phonenumbers[phoneKey].phonenumber!}">
                                            <input name="phoneLabel" type="hidden" value="${phoneKey}">
                                            <input type="hidden" value="${CRMcustomer.phonenumbers[phoneKey].verified?string}">
                                            <div>Bruk som standardnummer:
                                                <input id="${phoneKey}_defaultPhoneLabel" name="defaultphone" type="radio" value="${phoneKey}" <#if phoneKey == CRMcustomer.defaultPhoneLabel!>checked</#if>  >
                                            </div>

                                        </li>
                                    </#list>
                                    </ol>
                            </div>


                            <div class="form-inline">

                                    <label>Epost</label>
                                    <ol class="list-group">
                                    <#list CRMcustomer.emailaddresses?keys as emailKey>
                                        <li class="list-group-item">
                                            <label for="${emailKey}_defaultEmailLabel"><span class="label label-info">${emailKey}</span></label>
                                            <#if !CRMcustomer.emailaddresses[emailKey].verified><span class="glyphicon glyphicon-warning-sign text-danger" title="Ikke bekreftet"></span></#if>
                                            <input name="emailLabel" type="hidden" value="${emailKey}">
                                            <input id="${emailKey}_email" name="${emailKey}_email" type="text" value="${CRMcustomer.emailaddresses[emailKey].emailaddress!}">
                                            <input type="hidden" value="${CRMcustomer.emailaddresses[emailKey].verified?string}">
                                            <div class="">Bruk som standardadresse <input id="${emailKey}_defaultEmailLabel"
                                                                                          name="defaultEmail" type="radio" value="${emailKey}"
                                                <#if emailKey == CRMcustomer.defaultEmailLabel> checked</#if>  ></div>
                                        </li>
                                    </#list>
                                    </ol>
                            </div>

                            <div class="form-inline">

                                    <ol class="list-group">
                                    <label >Leveringsadresse</label>
                                    <#list CRMcustomer.deliveryaddresses?keys as adr>
                                        <li class="list-group-item">
                                            <span class="label label-info">${adr}</span> <br />
                                            <input name="addressLabel" type="hidden" value="${adr}">
                                            <input name="${adr}_addressLine1" value="${CRMcustomer.deliveryaddresses[adr].addressLine1!}" placeholder="Adresselinje 1">
                                            <input name="${adr}_addressLine2" value="${CRMcustomer.deliveryaddresses[adr].addressLine2!}" placeholder="Adresselinje 2">
                                            <input name="${adr}_postalCode" value="${CRMcustomer.deliveryaddresses[adr].postalcode!}" placeholder="Postnummer">
                                            <input name="${adr}_postalCity" value="${CRMcustomer.deliveryaddresses[adr].postalcity!}" placeholder="Poststed">
                                            <div class="">Lever varene til denne adressen <input id="${adr}_defaultAddressLabel" name="defaultAddress" type="radio" value="${adr}" <#if adr == CRMcustomer.defaultAddressLabel> checked</#if>  ></div>
                                        </li>
                                    </#list>
                                    </ol>
                            </div>

                            <div class="form-group">
                                <input class="button button-login" name="commit" type="submit" value="Bekreft opplysninger">
                            </div>
                        </form>

                    </div>
                    <div class="col-xs-2">

                    </div>
                    <div class="col-xs-5 ">
                        
                    </div>
            </div>
            <div class="col-xs-1 ">

            </div>
        </div>
    </div>
</div>
<script src="js/oid.js"></script>
</body>
<footer>
</footer>

</html>
