package com.lec.spring.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lec.spring.domain.User;
import com.lec.spring.domain.oauth.KakaoOauthToken;
import com.lec.spring.domain.oauth.KakaoProfile;
import com.lec.spring.service.UserService;
import com.lec.spring.util.U;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {
    // kakao 로그인
    @Value("${app.oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${app.oauth2.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${app.oauth2.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${app.oauth2.kakao.user-info-uri}")
    private String KakaoUserInfoUri;

    @Value("${app.oauth2.password}")
    private String oauth2Password;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/kakao/callback")  // kakao 가 보내준 code 값 받아오기
    public String getCode(String code){
        //------------------------------------------------------------------
        // ■ code 값 확인
        //   code 값을 받았다는 것은 인증 완료 되었다는 뜻..
                System.out.println("\n<<카카오 인증 완료>>\ncode: " + code);
        //----------------------------------------------------------------------
        // ■ Access token 받아오기 <= code 값 사용
        // 이 Access token 을 사용하여  Kakao resource server 에 있는 사용자 정보를 받아오기 위함.
        KakaoOauthToken token = kakaoAccessToken(code);

        //------------------------------------------------------------------
        // ■ 사용자 정보 요청 <= Access Token 사용
        KakaoProfile profile = kakaoUserInfo(token.getAccess_token());

        //---------------------------------------------------
        // ■ 회원가입 시키기  <= KakaoProfile (사용자 정보) 사용
        User kakaoUser = registerKakaoUser(profile);


        //---------------------------------------------------
        // ■ 로그인 처리
        loginKakaoUser(kakaoUser);

        return "redirect:/";
    }
    public KakaoOauthToken kakaoAccessToken(String code){
        // 카카오서버쪽으로 POST 방식 요청, key-value
        RestTemplate rt = new RestTemplate();

        // header 준비
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        // body  준비
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code); // 인증 직후 받은 code 값 사용!!
        // header 와 body 를 담은 HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
            new HttpEntity<>(params, headers);
        // 요청

        ResponseEntity<String> response = rt.exchange(
                kakaoTokenUri, // Access Token 발급 URI
                HttpMethod.POST, // request method
                kakaoTokenRequest, // HttpEntity (body header)
                String.class // 응답받을 타입

        );

        System.out.println("카카오 AccessToken 요청 응답: " + response);
        System.out.println("카카오 AccessToken 응답 body: " + response.getBody());

//        =================================================================
//        JSON -> Java Object
        ObjectMapper mapper = new ObjectMapper();
        KakaoOauthToken token = null;
        try {
            token = mapper.readValue(response.getBody(), KakaoOauthToken.class);
            // AccessToken 확인
            System.out.println("카카오 Access Token: " + token.getAccess_token());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return token;
    }

    // kakao 사용자 정보 요청
    public KakaoProfile kakaoUserInfo(String accessToken){
        RestTemplate rt = new RestTemplate();

        // header 준비
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "Content-type: application/x-www-form-urlencoded;charset=utf-8");

        // HttpEntity 생성
        HttpEntity<MultiValueMap<String, String>> kakaoProfileRequest =
                new HttpEntity<>(headers);
        // 요청
        ResponseEntity<String>response = rt.exchange(KakaoUserInfoUri,
                HttpMethod.POST,
                kakaoProfileRequest,
                String.class);

        System.out.println("카카오 사용자 profile 요청 응답: " + response);
        System.out.println("카카오 사용자 profile 응답 body: " + response.getBody());

        // ===================================
        // 사용자 정보 ( JSON ) -> JAVA 로 받아내기
        ObjectMapper mapper = new ObjectMapper();
        KakaoProfile profile = null;
        try {
            profile = mapper.readValue(response.getBody(), KakaoProfile.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("""
                [카카오 회원정보]
                id: %s
                nicname: %s
                """.formatted(profile.getId(),profile.getKakaoAccount().getProfile().getNickname()));
        return profile;
    }

    //-----------------------------------------------------------------------------
    // 회원가입 시키기  (username, password, email, name 필요)
    // Kakao 로그인 한 회원을 User 에 등록하기
    public User registerKakaoUser(KakaoProfile profile){
        // 새로이 가입시킬 username 을 셍성 (unique 해야 한다!)
        String provider = "KAKAO";
        String providerId = "" + profile.getId();

        String username = provider + "_" + providerId;
        String name = profile.getKakaoAccount().getProfile().getNickname();
        String password = oauth2Password;

        System.out.println("""
           [카카오 인증 회원 정보]
             username: %s
             name: %s
             password: %s  
             provider: %s
             providerId: %s            
           """.formatted(username, name, password, provider, providerId));

        // 회원가입 진행하기 전에
        // 이미 가입한 회원인지, 혹은 비가입자인지 체크하여야 한다.
        User user = userService.findByUsername(username);
        if(user == null){ // 미가입자인 경우에만 회원 가입 진행
            User newUser = User.builder()
                    .username(username)
                    .name(name)
                    .password(password)
                    .provider(provider)
                    .providerId(providerId)
                    .build();

            int cnt  = userService.register(newUser);  // 회원 가입! INSERT
            if(cnt > 0){
                System.out.println("[Kakao 인증 회원 가입 성공]");
                user = userService.findByUsername(username);
            }else{
                System.out.println("[Kakao 인증 회원 가입 실패]");
            }
        } else{
            System.out.println("[Kakao 인증. 이미 가입된 회원입니다.]");
        }

        return user;

    }

    public void loginKakaoUser(User kakaoUser){
        UsernamePasswordAuthenticationToken authenticationToken
                = new UsernamePasswordAuthenticationToken(kakaoUser.getUsername(), oauth2Password);
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);

        U.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        System.out.println("Kakao 인증 로그인 처리 완료");
    }


}
