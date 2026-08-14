package ctn.informatica.sca.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppConfigInitializer {

    @Autowired
    private ServletContext servletContext;

    @PostConstruct
    public void init() {
        AppConfig.init(servletContext);
    }
}