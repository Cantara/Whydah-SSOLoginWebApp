/*global $ */

(function () {
  'use strict';
  var registerLink = $('#signup > a');
  var registerForm = $('#id_registrer');
  var signInForm = $('#id_signin');
  var findButton = registerForm.find('.button-id-find');
  var phoneInput = registerForm.find('input[name="username"]');
  var personaliaContainer = registerForm.find('#personalia');

  var userLogInPhone = $('#user_session_login');
  var pinInput = $('#user_session_password');
  var getPinbtn = $('#getPin');
  var signInbtn = $('#signIn');

  function signInOID() {
    var phoneNumber = Number(userLogInPhone.val());
    if (!isNaN(phoneNumber) && userLogInPhone.val().length === 8) {
      $.ajax({
        method: 'GET',
        url: '/oidsso/getPin?phoneNo=' + phoneNumber,
        beforeSend: function () {
          getPinbtn.text('Sender pin');
        },
        complete: function () {
          pinInput.show();
          getPinbtn.hide();
          signInbtn.show();
          pinInput.focus();
        }
      }).then(function (response) {

      });
    }
  }

  function printIllegalInput(fieldID, errorText) {
    $('#' + fieldID).text(errorText);
    $('#' + fieldID + '.userNoticeField').show();
  }

  function isValidPhoneNr(phoneNr) {
    var phoneNumber = Number(phoneNr);
    var isNotaNumber = isNaN(phoneNumber);

    if (!isNotaNumber && phoneNr.length === 8) {
      return true;
    }
    return false;
  }

  function isValidEmail(email) {
    if (email.length === 0) {
      return false;
    }
    var regex = /^([a-zA-Z0-9_.+-])+\@(([a-zA-Z0-9-])+\.)+([a-zA-Z0-9]{2,4})+$/;
    return regex.test(email);
  }

  function isValidPin(pin) {
    var pincode = Number(pin);
    var isNotaNumber = isNaN(pincode);

    if (!isNotaNumber && pin.length === 4) {
      return true;
    }
    return false;
  }

  function clearRegErrors() {
    $('#NoticeWrongPhoneInputReg').hide();
    $('#NoticeWrongFirstNameInputReg').hide();
    $('#NoticeWrongLastNameInputReg').hide();
    $('#NoticeWrongAddressInputReg').hide();
    $('#NoticeWrongZipCodeInputReg').hide();
    $('#NoticeWrongCityInputReg').hide();
    $('#NoticeWrongEmailInputReg').hide();
    $('#NoticeWrongPinInputReg').hide();
  }

  function clearSignInErrors() {
    $('#NoticeWrongPhoneInput').hide();
    $('#NoticeWrongPinInput').hide();
  }

  //Validate signin form
  $(function () {
    signInForm.submit(function () {
      clearSignInErrors();

      var phoneNumber = userLogInPhone.val();
      if (phoneNumber === 'useradmin') {
        return true;
      }
      var pin = pinInput.val();
      var error = 0;
      if (!isValidPhoneNr(phoneNumber)) {
        printIllegalInput('NoticeWrongPhoneInput', 'Vennligst fyll inn et gyldig mobilnummer');
        error = error + 1;
      }
      if (!isValidPin(pin)) {
        printIllegalInput('NoticeWrongPinInput', 'PIN-koden m\u00E5 v\u00E6re 4 siffer');
        error = error + 1;
      }
      return error === 0;
    });
  });
  //Validate register form
  $(function () {
    registerForm.submit(function () {
      clearRegErrors();
      var phoneNumber = $('#phone').val();
      var email = $('#email').val();
      var pin = $('#pin').val();
      var firstName = $('#FirstName').val();
      var lastName = $('#LastName').val();
      var streetAddress = $('#streetAddress').val();
      var  zipCode = $('#zipCode').val();
      var city = $('#city').val();
      var oidAddress =$('#oidaddress');
      if (oidAddress.val().length === 0){
        oidAddress.val(address.val());
      }
      var error = 0;
      if (!isValidPhoneNr(phoneNumber)) {
        printIllegalInput('NoticeWrongPhoneInputReg', 'Vennligst fyll inn et gyldig mobilnummer');
        error = error + 1;
      }

      if (firstName.length === 0) {
        printIllegalInput('NoticeWrongFirstNameInputReg', 'Fornavn kan ikke v\u00E6re tom');
        error = error + 1;
      }

      if (lastName.length === 0) {
        printIllegalInput('NoticeWrongLastNameInputReg', 'Etternavn kan ikke v\u00E6re tomt');
        error = error + 1;
      }

      if (streetAddress.length === 0) {
        printIllegalInput('NoticeWrongAddressInputReg', 'Gateadresse kan ikke v\u00E6re tomt');
        error = error + 1;
      }
      if (zipCode.length === 0) {
        printIllegalInput('NoticeWrongZipCodeInputReg', 'Postnummer kan ikke v\u00E6re tomt');
        error = error + 1;
      }
      if (city.length === 0) {
        printIllegalInput('NoticeWrongCityInputReg', 'Poststed kan ikke v\u00E6re tomt');
        error = error + 1;
      }
      if (!isValidEmail(email)) {
        printIllegalInput('NoticeWrongEmailInputReg', 'Vennligst fyll inn en gyldig E-postadresse');
        error = error + 1;
      }

      if (!isValidPin(pin)) {
        printIllegalInput('NoticeWrongPinInputReg', 'PIN-koden m\u00E5 v\u00E6re 4 siffer');
        error = error + 1;
      }

      return error === 0;
    });
  });
  getPinbtn.click(function () {
    var userInput = userLogInPhone.val();
    if (userInput === 'useradmin') {
      pinInput.show();
      signInbtn.show();
      getPinbtn.hide();
      pinInput.focus();
    }
    else if (!isValidPhoneNr(userInput)) {
      printIllegalInput('NoticeWrongPhoneInput', 'Vennligst fyll inn et gyldig mobilnummer');

    } else {
      userExists(userInput);
    }
  });

  registerLink.click(function () {
    registerForm.show();
  });

  userLogInPhone.keydown(function (event) {
    if (event.keyCode === 13) {
      event.preventDefault();
      getPinbtn.click();
    }
  });

  phoneInput.keydown(function (event) {
    if (event.keyCode === 13) {
      event.preventDefault();
      findButton.click();
    }
  });
  function setValueOrEmpty(value) {
    if (typeof value !== 'undefined' && value !== null) {
      return value;
    }
    return '';
  }

  function userExists(phoneNumber) {
    $.ajax({
      method: 'GET',
      url: '/oidsso/user_exist?username=' + phoneNumber,
      beforeSend: function () {
        getPinbtn.text('Sender pin');
      }
    }).then(function (response) {


      if (response === 'true') {
        signInOID();

        pinInput.show();
        signInbtn.show();
        getPinbtn.hide();
        pinInput.focus();
      } else {
        findUserInfo();
        personaliaContainer.show();
        $('#login-item').hide();
        $('#register-item').show();
      }
    });
  }

  function findUserInfo() {
    var phoneNumber = Number(userLogInPhone.val());
    phoneInput.val(phoneNumber);
    if (!isNaN(phoneNumber) && userLogInPhone.val().length === 8) {

      $.ajax({
        method: 'GET',
        url: '/oidsso/opplysningenlookup?phoneNo=' + phoneNumber,
        beforeSend: function () {
          findButton.text('Henter opplysninger');
        },
        complete: function () {
          findButton.hide();
        }
      }).then(function (response) {
        if (response) {

          var objJSON = $.parseJSON(response);
          var result = objJSON.Results[0];

          if (result) {
            personaliaContainer.find('input#FirstName').val(setValueOrEmpty(result.FirstName));
            personaliaContainer.find('input#LastName').val(setValueOrEmpty(result.LastName));
            personaliaContainer.find('input#streetAddress').val(setValueOrEmpty(result.Addresses[0].Street) + " " +
                                                                setValueOrEmpty(result.Addresses[0].HouseNumber) + setValueOrEmpty(result.Addresses[0].HouseLetter));
            personaliaContainer.find('input#zipcode').val(setValueOrEmpty(result.Addresses[0].Zip));
            personaliaContainer.find('input#city').val(setValueOrEmpty(result.Addresses[0].City));
            personaliaContainer.find('input#oidaddress').val((result.Addresses[0].FormattedAddress));
            personaliaContainer.find('input#cellphone').val(setValueOrEmpty(phoneNumber));
          } else {
            personaliaContainer.find('input#cellphone').val(setValueOrEmpty(phoneNumber));
          }


        }
      });
    }
  }

}());
