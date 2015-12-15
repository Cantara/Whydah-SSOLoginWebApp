package net.whydah.sso.authentication;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 3/10/12
 */

@Deprecated //  Use UserCredential in Typelib
public interface UserCredential {
    String toXML();
}
