package net.whydah.sso.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason="Not authorized")
public class UnauthorizedException extends RuntimeException {
}
