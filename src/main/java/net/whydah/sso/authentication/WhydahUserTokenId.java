package net.whydah.sso.authentication;

@Deprecated  // use UserTokenID from TypeLib
public class WhydahUserTokenId {
    private final String usertokenid;

    private WhydahUserTokenId(String usertokenid) {
        this.usertokenid = usertokenid;
    }

    public String getUsertokenid() {
        return usertokenid;
    }

    public boolean isValid() {
        return usertokenid != null;
    }

    public static WhydahUserTokenId fromTokenId(String usertokenid) {
        return new WhydahUserTokenId(usertokenid);
    }

    public static WhydahUserTokenId invalidTokenId() {
        return new WhydahUserTokenId(null);
    }

    @Override
    public String toString() {
        return usertokenid;
    }
}
