package controllers.oauth;

import models.Oauth;
import models.ServiceProvider;
import models.User;

import org.apache.commons.lang.StringUtils;

public class OauthRequest {

	public String clientId;
	public String provider;
	public String displayName;
	public String email;
	public String photo;
	
	public OauthRequest(String clientId, ServiceProvider provider, String displayName, String email, String photo){
		this.clientId = clientId;
		this.provider = provider.toString();
		this.displayName = displayName;
		this.email = email;
		this.photo = photo;
	}

	public boolean isValid() {
		return StringUtils.isNotBlank(this.clientId) && ServiceProvider.contains(this.provider) && StringUtils.isNotBlank(this.displayName)
				&& StringUtils.isNotBlank(this.email) && StringUtils.isNotBlank(this.photo);
	}

	public static void populateUserFromOauthRequest(OauthRequest oauthRequest, User user) {
		user.loginName = oauthRequest.clientId;
		user.setPassword(oauthRequest.clientId);
		user.displayName = oauthRequest.displayName;
		user.email = oauthRequest.email;
		user.photo = oauthRequest.photo;
		user.photoProvider = ServiceProvider.valueOf(oauthRequest.provider);
	}
	
	public static Oauth createFromOauthRequest(OauthRequest oauthRequest, User user) {
		Oauth oauth = new Oauth();
		oauth.clientId = oauthRequest.clientId;
		oauth.user = user;
		oauth.provider = ServiceProvider.valueOf(oauthRequest.provider);
		return oauth;
	}
}
