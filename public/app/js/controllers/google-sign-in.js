function GoogleSignInController($scope, $timeout) {
    // This flag we use to show or hide the button in our HTML.
    $scope.signedIn = false;

    // Here we do the authentication processing and error handling.
    // Note that authResult is a JSON object.
    $scope.processAuth = function(authResult) {
        // Do a check if authentication has been successful.
        if(authResult['access_token']) {
            // Successful sign in.
            $scope.signedIn = true;

            $scope.getUserInfo();

        } else if(authResult['error']) {
            // Error while signing in.
            $scope.signedIn = false;

            // Report error.
        }
    };

    // When callback is received, we need to process authentication.
    $scope.signInCallback = function(authResult) {
        $scope.$apply(function() {
            $scope.processAuth(authResult);
        });
    };

    // Render the sign in button.
    $scope.renderSignInButton = function() {
        gapi.signin.render('signInButton',
            {
                'callback': $scope.signInCallback, // Function handling the callback.
                'clientid': '357766608319-8sklovn68l82ir061h2deu2o28cnl42a.apps.googleusercontent.com', // CLIENT_ID from developer console which has been explained earlier.
                'requestvisibleactions': 'http://schemas.google.com/AddActivity', // Visible actions, scope and cookie policy wont be described now,
                                                                                  // as their explanation is available in Google+ API Documentation.
                'scope': 'https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email',
                'cookiepolicy': 'single_host_origin'
            }
        );
    }

    // Process user info.
    // userInfo is a JSON object.
    $scope.processUserInfo = function(userInfo) {
        var user = {
          clientId: userInfo.id,
          provider: 'GOOGLE_PLUS',
          displayName: userInfo.displayName,
          email: userInfo['emails'][0]['value'],
          photo: userInfo.image.url.split("?")[0]
        };

        var json = JSON.stringify(user);
        $.post('/oauth-login', json, function(data) {
            saveAuthValues(data.user.loginName, data.token);
        }, 'json');
    }

    // When callback is received, process user info.
    $scope.userInfoCallback = function(userInfo) {
        $scope.$apply(function() {
            $scope.processUserInfo(userInfo);
        });
    };

    // Request user info.
    $scope.getUserInfo = function() {
        gapi.client.request(
            {
                'path':'/plus/v1/people/me',
                'method':'GET',
                'callback': $scope.userInfoCallback
            }
        );
    };

    $scope.loadScript = function(sScriptSrc, oCallback) {
      var oHead = document.getElementsByTagName('head')[0];
      var oScript = document.createElement('script');
      oScript.type = 'text/javascript';
      oScript.src = sScriptSrc;
      // most browsers
      oScript.onload = oCallback;
      // IE 6 & 7
      oScript.onreadystatechange = function() {
        if (this.readyState == 'complete') {
          oCallback();
        }
      }
      oHead.appendChild(oScript);
    }

    // Call start function on load.
    $scope.loadScript('https://apis.google.com/js/client:plusone.js', $scope.renderSignInButton);
}
