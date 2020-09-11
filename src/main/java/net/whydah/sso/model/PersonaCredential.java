package net.whydah.sso.model;

import net.whydah.sso.user.types.UserCredential;

public class PersonaCredential extends UserCredential {
	
	public PersonaCredential()
	{
		
	}
	
	private String displayText="";
	private String description="";
	private String firstName="";
	private String lastName="";
	private String email="";
	private String phone="";

	public PersonaCredential(String userName, String password, String displayText, String decription) {
		super(userName, password);
		if(this.displayText.length()==0) {
			if(this.firstName.length()!=0 ) {
				this.displayText = this.firstName;
			} 
			if(this.lastName.length()!=0) {
				this.displayText += " " + this.lastName;
			}
			if(this.displayText.length()==0) {
				this.displayText = userName;
			}
		} else {
			this.displayText = displayText;
		}
		this.setDescription(decription);
	}
	
	public String getDisplayText() {
		if(this.displayText.length()==0) {
			this.displayText = this.getUserName();
		}
		return displayText;
	}

	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhoner(String phoneNumber) {
		this.phone = phoneNumber;
	}
	
	
}
