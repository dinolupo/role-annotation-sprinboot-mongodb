package com.github.dinolupo.annotations;

import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

@Configuration
@ComponentScan
@Import(MongoAutoConfiguration.class)
public class RoleConfiguration implements ImportAware  {


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableRoleChecking.class.getName()));
		Assert.notNull(annotationAttributes,
				"No " + EnableRoleChecking.class.getSimpleName()
						+ " annotation found on  '" + importMetadata.getClassName() + "'.");
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
