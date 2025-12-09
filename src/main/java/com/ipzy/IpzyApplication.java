package com.ipzy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// TODO: tato126 사용자 탈퇴시 연결된 api 연결 해제 필요
// TODO: tato126 로그아웃 후에 로그인시에 다시 로그인 화면이 나타나도록 해야함
@SpringBootApplication
public class IpzyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpzyApplication.class, args);
    }
}
