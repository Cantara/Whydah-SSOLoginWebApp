package net.whydah.sso.authentication;

import net.whydah.sso.authentication.UserCredential;

@Deprecated //  Use UserCredential in Typelib
public class UserNameAndPasswordCredential implements UserCredential {
    private String userName;
    private String password;

    public UserNameAndPasswordCredential(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toXML(){
        if (userName== null){
            return templateToken;   //TODO Er ikke disse to helt identiske?
        } else {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
            "<usercredential>\n" +
            "    <params>\n" +
            "        <username>" + getUserName()+ "</username>\n" +
            "        <password>" + getPassword() + "</password>\n" +
            "    </params> \n" +
            "</usercredential>\n";
        }
    }

    String templateToken = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> \n " +
            "    <usercredential>\n" +
            "        <params>\n" +
            "            <username>" + getUserName() + "</username>\n" +
            "            <password>" + getPassword() + "</password>\n" +
            "        </params> \n" +
            "    </usercredential>\n";

    @Override
    public String toString() {
        return "UserNameAndPasswordCredential{" + "userName='" + userName + '}';
    }
}
