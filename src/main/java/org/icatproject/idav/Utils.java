package org.icatproject.iDav;

import org.icatproject.iDav.exceptions.UnauthenticatedException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.icatproject.Datafile;

public class Utils {

	public static final String PROPERTIES_FILENAME = "idav.properties";
	
	public static UsernamePassword getUsernamePasswordFromAuthString(String authString) throws UnauthenticatedException {
		// note - we should already know that the username password string
		// is in the right format because this will have been checked on first login
		String usernameColonPassword = new String(Base64.decodeBase64(authString));
		try {
			String[] usernamePasswordParts = usernameColonPassword.split(":",2);
			return new UsernamePassword(usernamePasswordParts[0], usernamePasswordParts[1]);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new UnauthenticatedException("Unexpected format of auth string: " + authString);
		}
	}

	public static String getDatafileAsShortString(Datafile df) {
		return "[name:" + df.getName() + ", id:" + df.getId() + ", size:" + df.getFileSize() + "]";
	}
	
	public static void main(String[] args) throws Exception {
		String username = "user1";
		String password = "pass1pass2";
		String authString = new String(Base64.encodeBase64(new String(username+password).getBytes()));
		System.out.println("authString = [" + authString + "]");
		UsernamePassword userPass = getUsernamePasswordFromAuthString(authString);
		System.out.println(userPass);
	}
	
	public static String escapeStringForIcatQuery(String inString) {
		// escape single quotes by adding an additional single quote
		// see http://stackoverflow.com/questions/9891205/escape-character-in-jpql
		return StringUtils.replace(inString, "'", "''");
	}

	public static String getStartAndEndOfSessionId(String sessionId) {
		String startChars = sessionId.substring(0,5);
		String endChars = sessionId.substring(sessionId.length()-5);
		return startChars + "..." + endChars;
	}
}
