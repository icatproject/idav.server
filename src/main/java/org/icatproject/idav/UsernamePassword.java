package org.icatproject.idav;

public class UsernamePassword {

	private String username;
	private String password;

	public UsernamePassword(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	@Override
	public String toString() {
		return "UsernamePassword [username=" + username 
				+ ", password=" + password + "]";
	}
	
}
