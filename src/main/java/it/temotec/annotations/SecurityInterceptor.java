package it.temotec.annotations;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Component
public class SecurityInterceptor extends HandlerInterceptorAdapter {
	
	@Autowired
	MongoClient mongoClient;
	
	@Autowired
	RoleConfigurationProperties roleProperties;
	
	@Autowired
	MongoProperties mongoProperties;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		
		String connectedUser = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
		
		// if no user is logged in, then exit
		if (connectedUser == null){
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		
		// cast the annotated spring handler method
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		// get method using reflection
		Method method = handlerMethod.getMethod();
		// check if the method is annotated
		if (handlerMethod.getMethod().isAnnotationPresent(Role.class)) {
			Annotation annotation = method.getAnnotation(Role.class);
			Role casRole = (Role) annotation;
			
			//System.out.printf("Access :%s\n", casRole.access());
			String[] roles = casRole.access();
			
			System.out.println("Prova:" + roles.length);
			
			MongoDatabase db = mongoClient.getDatabase(mongoProperties.getDatabase());
	        MongoCollection<Document> coll = db.getCollection(roleProperties.getCollection(), Document.class);
			//Bson filter = and(eq(roleProperties.getUsernamePath(), connectedUser), eq(roleProperties.getRolePath(), casRole.access()));
			Bson filter = and(eq(roleProperties.getUsernamePath(), connectedUser), in(roleProperties.getRolePath(), casRole.access()));
			
			long found = coll.count(filter);
			
			System.out.printf("Found: %d\n", found);

			if (0 == found) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}
		}
		
		return true;
	}
}
