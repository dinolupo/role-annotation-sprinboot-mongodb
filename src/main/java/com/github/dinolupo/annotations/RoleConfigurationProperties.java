package com.github.dinolupo.annotations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "dinolupo.annotations.security", ignoreUnknownFields = false)
public class RoleConfigurationProperties {
	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getUsernamePath() {
		return usernamePath;
	}

	public void setUsernamePath(String usernamePath) {
		this.usernamePath = usernamePath;
	}

	public String getRolePath() {
		return rolePath;
	}

	public void setRolePath(String rolePath) {
		this.rolePath = rolePath;
	}

	@NotNull
	String collection;
	
	@NotNull
	String usernamePath;
	
	@NotNull
	String rolePath;
}
