package it.temotec.annotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import com.mongodb.MongoClient;

@Configuration
@EnableConfigurationProperties(RoleConfigurationProperties.class)
@ComponentScan
public class RoleConfiguration implements ImportAware  {
	
	@Autowired
	MongoProperties mongoProperties;
	
	@Autowired
    RoleConfigurationProperties configProps;
	
	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableRoleChecking.class.getName()));
        if (annotationAttributes == null) {
            throw new IllegalArgumentException("@Role is not present on importing class " + importMetadata.getClassName());
        }
	}
	
	// While working with spring 1.3.0. Mx or RCx the mongo driver of spring boot data mongodb is v. 2.x and is not working with morphia 1.0.1 dependence
	// of mongo driver 3.1.0. To solve the problem, I created the MongoClient bean instead of using the @Autowired annotation on a property.
	// when Spring Boot will be GA it should be upgraded using mongo driver 3.1.0 and the following code could be deleted and use a MongoClient property that
	// automatically use the spring MongoProperties bean.
	@Bean
	public MongoClient getMongoClient() {
		final MongoClient mongoClient = new MongoClient(mongoProperties.getHost());
		return mongoClient;
	}
	
	@Bean
	public SecurityInterceptor getSecurityInterceptor() {
		final SecurityInterceptor securityInterceptor = new SecurityInterceptor();
		return securityInterceptor;
	}
	
	@Bean 
	LoggingInterceptor getLoggingInterceptor() {
		final LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		return loggingInterceptor;
	}
	
}
