package es.qopuir.basicfitbot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class).listeners(new ApplicationPreparedListener());
    }

    public static void main(String[] args) {
    	new SpringApplicationBuilder(Application.class).listeners(new ApplicationPreparedListener()).run(args);
    }
}