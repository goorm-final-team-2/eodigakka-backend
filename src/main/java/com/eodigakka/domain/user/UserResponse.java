package com.eodigakka.domain.user;

public record UserResponse(Long id, String nickname, String profileImage) {

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getNickname(), user.getProfileImage());
  }
}
