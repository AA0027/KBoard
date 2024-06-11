package com.lec.spring.service;

import com.lec.spring.domain.Authority;
import com.lec.spring.domain.User;
import com.lec.spring.repository.AuthorityRepository;
import com.lec.spring.repository.UserRepository;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private PasswordEncoder encoder;
    UserRepository userRepo;
    AuthorityRepository authRepo;
    @Autowired
    public UserServiceImpl(SqlSession session){
        userRepo = session.getMapper(UserRepository.class);
        authRepo = session.getMapper(AuthorityRepository.class);
    }



    @Override
    public User findByUsername(String username) {
        return userRepo.findByUsername(username.toUpperCase());
    }

    @Override
    public boolean isExist(String username) {
        return userRepo.findByUsername(username.toUpperCase()) != null;
    }

    @Override
    public int register(User user) {
        user.setUsername(user.getUsername().toUpperCase());  // DB 에는 username 을 대문자로 저장
        user.setPassword(encoder.encode(user.getPassword()));
        userRepo.save(user); // 신규회원 저장. id 값 받아옴

        // 신규회원은  ROLE_MEMBER 권한을 기본적으로 부여하기
        authRepo.addAuthority(user.getId(),authRepo.findByName("ROLE_MEMBER").getId());
        return 1;
    }

    @Override
    public List<Authority> selectAuthoritiesById(Long id) {
        return authRepo.findByUser(userRepo.findById(id));
    }
}
