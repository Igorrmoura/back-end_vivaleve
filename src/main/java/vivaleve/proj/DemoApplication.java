package vivaleve.proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// As datas (createdAt etc.) são gravadas sem fuso. Fixar UTC garante que elas significam
		// sempre a mesma coisa, rodando no Render (já é UTC) ou num computador no Brasil.
		// O JSON sai com "Z" (HorarioUtcSerializer) e o navegador mostra no horário local.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(DemoApplication.class, args);
	}

}
