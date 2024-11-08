package com.hong.ForPaw;

import com.hong.ForPaw.domain.District;
import com.hong.ForPaw.domain.Province;
import com.hong.ForPaw.domain.User.UserRole;
import com.hong.ForPaw.domain.User.User;
import com.hong.ForPaw.domain.User.UserStatus;
import com.hong.ForPaw.repository.UserRepository;
import com.hong.ForPaw.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class ForPawApplication {

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(ForPawApplication.class, args);
	}

	@Profile("local")
	@Bean
	CommandLineRunner localServerStart(PasswordEncoder passwordEncoder, UserRepository userRepository){
		return args -> {
			//userRepository.saveAll(Arrays.asList(
			//
			//));
		};
	}

	private User newUser(String email, String name, String nickName, String password, UserRole userRole, String profileURL, Province province, District district, String subDistrict) {
		return User.builder()
				.email(email)
				.name(name)
				.nickName(nickName)
				.password(passwordEncoder.encode(password))
				.role(userRole)
				.profileURL(profileURL)
				.province(province)
				.district(district)
				.subDistrict(subDistrict)
				.build();
	}
}
