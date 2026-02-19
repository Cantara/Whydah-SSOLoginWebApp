package net.whydah.sso.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;

public final class ToStringHelper {

    private ToStringHelper() {}

    public static String toString(AuthenticationSuccessResponse authenticationSuccessResponse) {
        if (authenticationSuccessResponse != null) {
            Map<String, List<String>> map = authenticationSuccessResponse.toParameters();
            List<String> toStringContent = authenticationSuccessResponse.toParameters().keySet().stream().map(key -> key + "=" + map.get(key))
                    .collect(Collectors.toList());
            toStringContent.add("redirectionURI=[" + authenticationSuccessResponse.getRedirectionURI() + "]");
            return "AuthenticationSuccessResponse=" + toStringContent.stream().collect(Collectors.joining(", ", "{", "}"));
        }
        return "";
    }
}
