package com.example.springjpa;

import com.example.springjpa.entities.User;
import com.example.springjpa.repository.UserRepository;
import com.example.springjpa.services.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringJpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringJpaApplication.class, args);


	}

@Bean
public CommandLineRunner demo(UserService userService) {
	return (args) -> {
		// Create and save a new user
		User user = new User();
		user.setName("John Doe");
		user.setEmail("bkcasd@gmail.com");
		user.setAge(30);
		//userService.saveUser(user);

		// Find and print all users
		System.out.println("All users: " + userService.findAllUsers());

		// Find user by ID
		userService.findUserById(1L).ifPresent(u ->
				System.out.println("Found user: " + u));

		// Update user
		userService.findUserById(1L).ifPresent(u -> {
			u.setAge(31);
			userService.updateUser(u);
			System.out.println("Updated user: " + u);
		});

		// Find by email
		User foundByEmail = userService.findByEmail("john@example.com");
		System.out.println("Found by email: " + foundByEmail);
	};
}
}
