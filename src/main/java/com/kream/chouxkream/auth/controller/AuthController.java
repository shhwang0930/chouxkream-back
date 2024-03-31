package com.kream.chouxkream.auth.controller;

import com.kream.chouxkream.auth.JwtUtils;
import com.kream.chouxkream.auth.service.AuthService;
import com.kream.chouxkream.common.model.entity.ResponseMessage;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.kream.chouxkream.auth.constants.AuthConst.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @PostMapping("/jwt-reissue")
    public ResponseEntity<?> jwtReissue(HttpServletRequest request, HttpServletResponse response) {

        // get refresh token
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token is null", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(REFRESH_TOKEN_TYPE)) {
                refreshToken = cookie.getValue();
            }
        }

        // refresh token null 체크
        if (refreshToken == null) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token is null", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // refresh token 만료일 체크
        try {
            jwtUtils.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token expired", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // token type 체크
        String tokenType = jwtUtils.getType(refreshToken);
        if (!tokenType.equals(REFRESH_TOKEN_TYPE)) {

            ResponseMessage responseMessage = new ResponseMessage(400, "invalid refresh token type", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // Redis에 저장되어 있는지 체크
        Boolean isExists = authService.isExistRefreshToken(refreshToken);
        if (!isExists) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token does not exist on the server", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // make new jwt token
        String email = jwtUtils.getEmail(refreshToken);
        String role = jwtUtils.getRole(refreshToken);

        String newAccessToken = jwtUtils.createJwt(ACCESS_TOKEN_TYPE, email, role, ACCESS_TOKEN_EXPIRED_MS);
        String newRefreshToken = jwtUtils.createJwt(REFRESH_TOKEN_TYPE, email, role, REFRESH_TOKEN_EXPRED_MS);

        authService.deleteRefreshToken(refreshToken);
        authService.saveRefreshToken(newRefreshToken, email);

        // response
        response.setHeader(ACCESS_TOKEN_TYPE, newAccessToken);
        response.addCookie(createCookie(REFRESH_TOKEN_TYPE, newRefreshToken));
        ResponseMessage responseMessage = new ResponseMessage(200, "", null);
        return ResponseEntity.status(HttpServletResponse.SC_OK).body(responseMessage);

    }

    /**
     * 이메일(아이디) 변경 후 JWT 토큰 재발급
     */
    @PostMapping("/jwt-reissue/update-email")
    public ResponseEntity<?> jwtReissueAfterUpdateEmail(HttpServletRequest request, HttpServletResponse response,
                                                        @RequestParam("updateEmail") String updateEmail) {

        // get refresh token
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token is null", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(REFRESH_TOKEN_TYPE)) {
                refreshToken = cookie.getValue();
            }
        }

        // refresh token null 체크
        if (refreshToken == null) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token is null", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // refresh token 만료일 체크
        try {
            jwtUtils.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token expired", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // token type 체크
        String tokenType = jwtUtils.getType(refreshToken);
        if (!tokenType.equals(REFRESH_TOKEN_TYPE)) {

            ResponseMessage responseMessage = new ResponseMessage(400, "invalid refresh token type", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // Redis에 저장되어 있는지 체크
        Boolean isExists = authService.isExistRefreshToken(refreshToken);
        if (!isExists) {

            ResponseMessage responseMessage = new ResponseMessage(400, "refresh token does not exist on the server", null);
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(responseMessage);
        }

        // make new jwt token
        String role = jwtUtils.getRole(refreshToken);

        String newAccessToken = jwtUtils.createJwt(ACCESS_TOKEN_TYPE, updateEmail, role, ACCESS_TOKEN_EXPIRED_MS);
        String newRefreshToken = jwtUtils.createJwt(REFRESH_TOKEN_TYPE, updateEmail, role, REFRESH_TOKEN_EXPRED_MS);

        authService.deleteRefreshToken(refreshToken);
        authService.saveRefreshToken(newRefreshToken, updateEmail);

        // response
        response.setHeader(ACCESS_TOKEN_TYPE, newAccessToken);
        response.addCookie(createCookie(REFRESH_TOKEN_TYPE, newRefreshToken));
        ResponseMessage responseMessage = new ResponseMessage(200, "", null);
        return ResponseEntity.status(HttpServletResponse.SC_OK).body(responseMessage);
    }

    private Cookie createCookie(String key, String value) {

        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        // cookie.setSecure(true);
        // cookie.setPath("/");
        cookie.setHttpOnly(true);

        return cookie;
    }
}
