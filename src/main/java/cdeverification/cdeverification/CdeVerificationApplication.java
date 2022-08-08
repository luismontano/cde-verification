package cdeverification.cdeverification;

import cdeverification.cdeverification.service.CDEVeritifactionService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.ObjectInputFilter;

@SpringBootApplication
public class CdeVerificationApplication implements CommandLineRunner {

	@Autowired
	private ApplicationContext applicationContext;

	public static void main(String[] args) {
		SpringApplication.run(CdeVerificationApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		CDEVeritifactionService CDEverificationService = applicationContext.getBean(CDEVeritifactionService.class);
		CDEverificationService.verifyCDE();
	}
}
