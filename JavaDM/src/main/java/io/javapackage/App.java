package io.javapackage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import core.util.HttpUtils;

import java.io.IOException;

@SpringBootApplication
public class App {

	public static void main(String[] args) throws IOException {
		String githubUrl = "https://avatars.githubusercontent.com/u/52282402?v=4";
		String googleUrl = "https://en.wikipedia.org/wiki/tom";
		System.out.println(HttpUtils.processRedirects(googleUrl, 10));
//		SpringApplication.run(App.class, args);
	}

}
