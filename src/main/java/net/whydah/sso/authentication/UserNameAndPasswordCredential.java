package net.whydah.sso.authentication;

import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;

@Deprecated //  Use UserCredential in Typelib
public class UserNameAndPasswordCredential implements UserCredential {
    private UserName userName;
    private Password password;

    public UserNameAndPasswordCredential(String userName, String password) {
        this.userName = new UserName(userName);
        this.password = new Password(password);
    }

    public String getUserName() {
        return userName.getInput();
    }

    public void setUserName(String userName) {
        this.userName = new UserName(userName);
    }

    public String getPassword() {
        return password.getInput();
    }

    public void setPassword(String password) {
        this.password = new Password(password);
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
        return "UserNameAndPasswordCredential{" + "userName='" + getUserName() + '}';
    }
}
