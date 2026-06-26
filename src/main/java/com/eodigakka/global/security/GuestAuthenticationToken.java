package com.eodigakka.global.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

public class GuestAuthenticationToken extends AbstractAuthenticationToken {

  private final GuestUser principal;

  public GuestAuthenticationToken(GuestUser principal) {
    this(principal, AuthorityUtils.createAuthorityList(SecurityAuthority.GUEST));
  }

  public GuestAuthenticationToken(
      GuestUser principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public GuestUser getPrincipal() {
    return principal;
  }
}
