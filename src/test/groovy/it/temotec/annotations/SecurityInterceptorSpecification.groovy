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

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

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

@TestPropertySource('classpath:application.yml')
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes=[RoleConfiguration.class])
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
	int PORT = 12345

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

	def 'spec 0 autowired of SecurityInterceptor works correctly'() {
		given:

		expect:
		securityInterceptor != null
		mongoClient != null

	}

	@Unroll
	def 'spec 1 preHandle must return #boolValue and HTTP Status #httpStatus when method is annotated with @Role(\'write\') and principalName=#principalName, dbUsername=#dbUsername, dbRoles=#dbRoles'() {

		given:
		MongoDatabase db = mongoClient.getDatabase('UserRegistrationDB')
		MongoCollection<Document> coll = db.getCollection('User', Document.class)
		coll.drop()
		Document user = new Document('_id', 1)
				.append('username', dbUsername)
				.append('roles', dbRoles)
		coll.insertOne(user)

		// Mock HttpServletRequest
		MockHttpServletRequest request = new MockHttpServletRequest()
		request.addHeader('Accept', 'application/json')
		request.method = 'POST'
		request.requestURI = '/some/url'
		request.contentType = MediaType.APPLICATION_JSON

		// Mock HttpServletResponse
		MockHttpServletResponse response = new MockHttpServletResponse()

		when:
		// set the user principal in the mocked request
		Principal principal = new MockPrincipal(principalName, new HashSet<>(), 'key', 'signature', 'HMAC-SHA-1', 'signaturebase', 'token')
		request.setUserPrincipal(principal)

		// call the handle passing an anonymous object with the annotated method
		boolean retValue = securityInterceptor.preHandle(request, response, new HandlerMethod (
				new Object() {
					@Role('write')
					void test() {}
				}, 'test')
				)

		then:
		retValue == boolValue
		response.getStatus() == httpStatus

		where:
		principalName	|	dbUsername	|	dbRoles				|	boolValue	|	httpStatus
		'admin'			|	'admin'		|	['write','read']	|	true		|	HttpServletResponse.SC_OK
		'user'			|	'user'		|	['read','write']	|	true		|	HttpServletResponse.SC_OK
		'one'			|	'one'		|	['write']			|	true		|	HttpServletResponse.SC_OK
		'guest'			|	'guest'		|	['read']			|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'guest'			|	'guest'		|	[]					|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'guest'			|	'admin'		|	['write']			|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'admin'			|	'guest'		|	['write']			|	false		|	HttpServletResponse.SC_UNAUTHORIZED

	}

	@Unroll
	def 'spec 2 preHandle must return #boolValue and HTTP Status #httpStatus when method is annotated with array of roles: @Role([\'write\',[\'read\',[\'public\']) and principalName=#principalName, dbUsername=#dbUsername, dbRoles=#dbRoles'() {

		given:
		MongoDatabase db = mongoClient.getDatabase('UserRegistrationDB')
		MongoCollection<Document> coll = db.getCollection('User', Document.class)
		coll.drop()
		Document user = new Document('_id', 1)
				.append('username', dbUsername)
				.append('roles', dbRoles)
		coll.insertOne(user)

		// Mock HttpServletRequest
		MockHttpServletRequest request = new MockHttpServletRequest()
		request.addHeader('Accept', 'application/json')
		request.method = 'POST'
		request.requestURI = '/some/url'
		request.contentType = MediaType.APPLICATION_JSON

		// Mock HttpServletResponse
		MockHttpServletResponse response = new MockHttpServletResponse()

		when:
		// set the user principal in the mocked request
		Principal principal = new MockPrincipal(principalName, new HashSet<>(), 'key', 'signature', 'HMAC-SHA-1', 'signaturebase', 'token')
		request.setUserPrincipal(principal)

		// call the handle passing an anonymous object with the annotated method
		boolean retValue = securityInterceptor.preHandle(request, response, new HandlerMethod (
				new Object() {
					@Role(['write','read', 'public' ])
					void test() {}
				}, 'test')
				)

		then:
		retValue == boolValue
		response.getStatus() == httpStatus

		where:
		principalName	|	dbUsername	|	dbRoles				|	boolValue	|	httpStatus
		'admin'			|	'admin'		|	['write']			|	true		|	HttpServletResponse.SC_OK
		'admin'			|	'admin'		|	['read']			|	true		|	HttpServletResponse.SC_OK
		'admin'			|	'admin'		|	['public']			|	true		|	HttpServletResponse.SC_OK
		'user2'			|	'user2'		|	['read', 'write']	|	true		|	HttpServletResponse.SC_OK
		'user2'			|	'user2'		|	['other', 'write']	|	true		|	HttpServletResponse.SC_OK
		'user2'			|	'user2'		|	['other', 'public']	|	true		|	HttpServletResponse.SC_OK
		'guest'			|	'guest'		|	['notpresent']		|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'guest'			|	'guest'		|	['no1','no2']		|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'guest'			|	'guest'		|	[]					|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'guest'			|	'admin'		|	['write']			|	false		|	HttpServletResponse.SC_UNAUTHORIZED
		'admin'			|	'guest'		|	['write']			|	false		|	HttpServletResponse.SC_UNAUTHORIZED

	}

	def 'spec 3 checking SecurityInterceptor.preHandle on method annotated with no role annotation'() {
		given:
		// Mock HttpServletRequest
		MockHttpServletRequest request = new MockHttpServletRequest()
		request.addHeader('Accept', 'application/json')
		request.method = 'POST'
		request.requestURI = '/some/url'
		request.contentType = MediaType.APPLICATION_JSON

		// Mock HttpServletResponse
		MockHttpServletResponse response = new MockHttpServletResponse()

		when:
		// set the user principal in the mocked request
		Principal principal = new MockPrincipal('test', new HashSet<>(), 'key', 'signature', 'HMAC-SHA-1', 'signaturebase', 'token')
		request.setUserPrincipal(principal)

		// call the handle passing an anonymous object with the annotated method
		boolean retValue = securityInterceptor.preHandle(request, response, new HandlerMethod (
				new Object() {
					void test() {}
				}, 'test')
				)

		then:
		retValue == true

	}

	def 'spec 4 checking SecurityInterceptor.preHandle on method annotated with no role annotation and no user principal logged'() {
		given:
		// Mock HttpServletRequest
		MockHttpServletRequest request = new MockHttpServletRequest()
		request.addHeader('Accept', 'application/json')
		request.method = 'POST'
		request.requestURI = '/some/url'
		request.contentType = MediaType.APPLICATION_JSON

		// Mock HttpServletResponse
		MockHttpServletResponse response = new MockHttpServletResponse()

		when:
		request.setUserPrincipal(null)

		// call the handle passing an anonymous object with the method not annotated
		boolean retValue = securityInterceptor.preHandle(request, response, new HandlerMethod (
				new Object() {
					void test() {}
				}, 'test')
				)

		then:
		retValue == false

	}
}
