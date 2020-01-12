package com.chenhm.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.beust.jcommander.JCommander;
import com.chenhm.blog.runner.BlogRunner;

@SpringBootApplication
public class Application {

    public static void main(String[] argv) throws Exception {
        Args args = new Args();
        JCommander jcmdr = JCommander.newBuilder().addObject(args).build();
        jcmdr.parse(argv);

        if(args.isHelp()){
            jcmdr.usage();
            return;
        }

        SpringApplication app = new SpringApplication(Application.class);
        if (!args.isDev()){
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        app.run(argv).getBean(BlogRunner.class).run(args);
    }

}
