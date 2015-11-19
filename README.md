# role-annotation-sprinboot-mongodb
===================================

Library providing annotation-based configuration support for Role Access Security on Spring Boot Controllers Methods.
Roles configuration is read on MongoDB based on database, collection and path configuration.

> This project was developer as a tutorial on Annotations and is shown on the following article on my blog site [Dino Lupo Blog](http://dinolupo.github.io).

## Current version
`1.0.0`

## Usage

* create a Jar with gradle jar:

```./gradlew jar```

import the library in your project:

> Gradle:

  ```Groovy
  dependencies {
        ...
  		compile name: 'com.github.dinolupo:role-annotation-springboot-mongodb:1.0.0'
        ...
  }
  ```
> Maven:

  ```xml
  <dependency>
    <groupId>com.github.dinolupo</groupId>
    <artifactId>role-annotation-springboot-mongodb</artifactId>
    <version>1.0.0-RELEASE</version>
    <scope>runtime</scope>
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

dinolupo:
  annotations:
    security:
      collection: User
      role-path: roles
      username-path: username
```

* Annotate Spring Boot root application (or any @Configuration class) with `@EnableRoleChecking` annotation
* Autowire the SecurityInterceptor
* Add the interceptor and configure the protected pattern if needed with regex urls

> Spring Boot root Application code sample

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

> Spring Boot Controller and annotated method code sample

```java
@Controller
public class SampleController {
}

    @Role("read-user")
    public ResponseEntity<Object> readOnlyMethod(HttpServletRequest request, HttpServletResponse response) {
    	// this method will be accessed only if the current logged in user Principal has the role "read-user"
    }
    
    @Role({"read-user","write-user"})
    public ResponseEntity<Object> getAllUsers(HttpServletRequest request, HttpServletResponse response) {
    	// this method will be accessed only if the current logged in user Principal has one of the roles "read-user" and "write-user"
    }
}    
```