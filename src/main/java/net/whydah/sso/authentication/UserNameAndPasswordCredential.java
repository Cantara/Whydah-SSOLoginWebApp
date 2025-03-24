package net.whydah.sso.authentication;

import net.whydah.sso.ddd.model.user.Password;
import net.whydah.sso.ddd.model.user.UserName;
import net.whydah.sso.user.types.UserCredential;

// @Deprecated //  Use UserCredential in Typelib
public class UserNameAndPasswordCredential extends UserCredential {
    private UserName userName;
    private Password password;

    public UserNameAndPasswordCredential(String userName, String password) {
        this.userName = new UserName(userName);
        this.password = new Password(password);
    }

    public UserNameAndPasswordCredential(UserName userName, Password password) {
        this.userName = userName;
        this.password = password;
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
            return """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>\s
                     \
                        <usercredential>
                            <params>
                                <username>\
                    </username>
                                <password>\
                    </password>
                            </params>\s
                        </usercredential>
                    """;
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


    @Override
    public String toString() {
        return "UserNameAndPasswordCredential{" + "userName='" + getUserName() + '}';
    }
}
