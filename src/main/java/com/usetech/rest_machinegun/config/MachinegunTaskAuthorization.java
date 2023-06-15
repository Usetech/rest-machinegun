package com.usetech.rest_machinegun.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MachinegunTaskAuthorization {
	private MachinegunTaskAuthOAuth2 oauth2;
	private MachinegunTaskAuthBasic basic;

	public MachinegunTaskAuthOAuth2 getOauth2() {
		return oauth2;
	}

	public void setOauth2(MachinegunTaskAuthOAuth2 oauth2) {
		this.oauth2 = oauth2;
	}

	public MachinegunTaskAuthBasic getBasic() {
		return basic;
	}

	public void setBasic(MachinegunTaskAuthBasic basic) {
		this.basic = basic;
	}
}
