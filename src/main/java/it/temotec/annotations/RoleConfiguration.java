package it.temotec.annotations;

import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

@Configuration
@ComponentScan
@EnableConfigurationProperties(MongoAutoConfiguration.class)
public class RoleConfiguration implements ImportAware  {


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableRoleChecking.class.getName()));
		if (annotationAttributes == null) {
			throw new IllegalArgumentException("@Role is not present on importing class " + importMetadata.getClassName());
		}
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
