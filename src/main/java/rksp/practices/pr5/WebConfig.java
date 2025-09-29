package rksp.practices.pr5;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {
    @Value("${app.upload-path:/upload-files}") String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry reg) {
        String loc = "file:" + (uploadPath.endsWith("/") ? uploadPath : uploadPath + "/");
        reg.addResourceHandler("/upload-files/**").addResourceLocations(loc);
    }
}
