//def setup() {}          // run before every feature method
//def cleanup() {}        // run after every feature method
//def setupSpec() {}     // run before the first feature method
//def cleanupSpec() {}   // run after the last feature method
package it.temotec.annotations

import java.security.Principal

import javax.servlet.http.HttpServletResponse;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.web.method.HandlerMethod

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.IntegrationTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Primary
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.http.*
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
//import org.springframework.security.*
//import org.springframework.security.role.*
//import static org.springframework.

import spock.lang.Shared
import spock.lang.Specification

import org.bson.Document;
import org.junit.runner.RunWith

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase
import de.flapdoodle.embed.mongo.MongoImportExecutable
import de.flapdoodle.embed.mongo.MongoImportProcess
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodProcess
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.MongoImportStarter

import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder
import de.flapdoodle.embed.mongo.config.IMongoImportConfig
import de.flapdoodle.embed.mongo.config.MongoImportConfigBuilder
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder
import de.flapdoodle.embed.process.extract.UserTempNaming
import de.flapdoodle.embed.mongo.Command
import de.flapdoodle.embed.mongo.distribution.Version




//@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource('classpath:application.yml')
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes=[MongoProperties.class, RoleConfiguration.class])
class SecurityInterceptorSpecification extends Specification {

	public static class MockPrincipal implements Principal {
		public String name;
		public Collection<GrantedAuthority> authorities;
		public MockPrincipal(String name, Collection<GrantedAuthority> authorities, String consumerKey, String signature, String signatureMethod, String signatureBaseString, String token) {
			this.name = name;
			this.authorities = authorities;
		}
		
		@Override
		public String getName() {
			return name;
		}
		
		public Collection<? extends GrantedAuthority> getAuthorities() {
			return authorities;
		}
	}
	
	
	@Autowired
	SecurityInterceptor securityInterceptor

	@Shared
	int PORT = 27017

	@Shared
	private MongodStarter starter = MongodStarter.getDefaultInstance()
	@Shared
	private MongodExecutable mongoExecutable
	@Shared
	private MongodProcess mongoProcess
	@Shared
	private MongoClient mongoClient

	// create de.flapdoodle.embed.mongo instance
	def setupSpec(){
		mongoExecutable = starter.prepare(new MongodConfigBuilder()
				.version(Version.Main.PRODUCTION)
				.net(new Net(PORT, false))
				.build())
		mongoProcess = mongoExecutable.start()
		mongoClient = new MongoClient("localhost", PORT)
	}

	// stop the instance of embedded mongo
	def cleanupSpec(){
		mongoClient.close()
		mongoProcess.stop()
		mongoExecutable.stop()
	}

	def setup() {
	}

	def cleanup() {

	}

	def 'test spec 1 autowired of SecurityInterceptor works correctly'() {
		given:
		
		expect:
		securityInterceptor != null
		mongoClient != null

	}

//	public MockHttpSession makeAuthSession(String username, String... roles) {
//		if (StringUtils.isEmpty(username)) {
//			username = "azeckoski";
//		}
//		MockHttpSession session = new MockHttpSession();
//		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
//		Collection<GrantedAuthority> authorities = new HashSet<>();
//		if (roles != null && roles.length > 0) {
//			for (String role : roles) {
//				authorities.add(new SimpleGrantedAuthority(role));
//			}
//		}
//		//Authentication authToken = new UsernamePasswordAuthenticationToken("azeckoski", "password", authorities); // causes a NPE when it tries to access the Principal
//		Principal principal = new MockPrincipal(username, authorities,
//				'key', 'signature', 'HMAC-SHA-1', 'signaturebase', 'token');
//		Authentication authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
//		SecurityContextHolder.getContext().setAuthentication(authToken);
//		return session;
//	}
		
	def 'test spec 0 security interceptor verify the correct Role Annotated method'() {

		given:
		MongoDatabase db = mongoClient.getDatabase('UserRegistrationDB')
		MongoCollection<Document> coll = db.getCollection('User', Document.class)
		coll.drop()
		
		Document user = new Document('_id', 1)
							.append('username', 'admin')
							.append('hashedPassword', '$2a$10$Jdl9SQ6IrFalG0YAhVNmg.k5dy5wbQUMXYoix738MQQaC.TCnIUUK')
							.append('salt', 'ULCvfdr9x1QbN55HELSci3QEB2a7nMSS6NPClxW')
							.append('email', 'admin@temotec.it')
							.append('firstname', 'Obi')
							.append('lastname', 'One Kenobi')
							.append('roles', ['write','read', 'guest', 'admin' ])							 
		
		coll.insertOne(user)
		
		when:
		Document read = coll.find(new Document('_id', 1)).first()
		
		// Mock HttpServletRequest
		MockHttpServletRequest request = new MockHttpServletRequest()
		request.addHeader('Authorization', 'Some Token')
		request.addHeader('Accept', 'application/json')
		request.method = 'POST'
		request.requestURI = '/some/url'
		request.contentType = MediaType.APPLICATION_JSON
		Principal principal = new MockPrincipal('carciofo', new HashSet<>(), 'key', 'signature', 'HMAC-SHA-1', 'signaturebase', 'token')
		request.setUserPrincipal(principal)
		
		// Mock HttpServletResponse
		MockHttpServletResponse response = new MockHttpServletResponse()
		
		def retValue = securityInterceptor.preHandle(request, response, new HandlerMethod (
																				new Object() {
																					@Role('write')
																					void test() {}
																				}, 'test')
														)
		then:
		read.getInteger('_id') == 1
		read.getString('lastname') == 'One Kenobi'
		
		retValue == false
		response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED

	}
 
	//@Ignore('demo method')
	def 'Test Specification'() {
		given:
		def a = 0

		when:
		a = a + 1

		then:
		a==1
		a!=0
		a>0
		println 'test specification complete'
	}


}
