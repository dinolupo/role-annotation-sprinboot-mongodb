# role-annotation-sprinboot-mongodb
===================================

Library providing annotation-based configuration support for Role Access Security on Spring Boot Controllers Methods.
Roles configuration is read on MongoDB based on database, collection and path configuration.

> This project was developer as a tutorial on Annotations and is shown on the following article on my blog site [Dino Lupo Blog](http://dinolupo.github.io).

## Usage

* create a Jar with gradle jar:

```./gradlew jar```

import the library in your project:

> Gradle:

  ```Groovy
  dependencies {
        ...
  		compile name: 'temotec-annotations-0.1.SNAPSHOT'
        ...
  }
  ```
> Maven:

  ```xml
  <dependency>
    <groupId>sample</groupId>
    <artifactId>com.sample</artifactId>
    <version>1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/temotec-annotations-0.1.SNAPSHOT.jar</systemPath>
  </dependency>
  ```
  

* Add the following required properties

> in Spring Boot's `application.properties` or `application.yml` Example:

```yml
spring:
  data:
    mongodb:
      database: UserRegistrationDB
      host: localhost
      port: 27017

temotec:
  annotations:
    security:
      collection: User
      role-path: roles
      username-path: username
```

* Annotate Spring Boot root application (or any @Configuration class) with `@EnableRoleChecking` annotation
* Autowire the SecurityInterceptor
* Add the interceptor and configure the protected pattern if needed with regex urls

> Complete root application code sample

```java
@SpringBootApplication
@Controller
@EnableCasClient
@EnableRoleChecking
public class Application extends WebMvcConfigurerAdapter {
		
	@Autowired
	SecurityInterceptor securityInterceptor;
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class).run(args);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
	    registry.addInterceptor(securityInterceptor).addPathPatterns("/protected/*");
	}
		
}
```

**SAMPLE** for using this **annotation**:

	@Role({"read-user","write-user"})
    @Role("read-user")