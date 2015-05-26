package controllers.posts;

import java.util.List;

import models.Post;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Response;
import play.test.Fixtures;
import play.test.FunctionalTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import controllers.ContentType;
import controllers.HttpMethod;
import controllers.RequestBuilder;

public class PostsTest extends FunctionalTest {

	private static final String BILLY_SECRET_TOKEN = "secret_token_321";
	private static final String BILLY_USERNAME = "billy";
	private static final int NOT_EXISTING_POST_ID = 133454552;

	@Before
	public void beforeTest() {
		Fixtures.deleteAllModels();
	}

	@Test
	public void testPostCreation() {
		Fixtures.loadModels("data/user.yml");

		Response response = new RequestBuilder()
			.withPath("/posts")
			.withHttpMethod(HttpMethod.POST)
			.withContentType(ContentType.APPLICATION_JSON)
			.withBody("{\"content\":\"test post content\"}")
			.send();

		assertStatus(201, response);
		assertContentType("application/json", response);
		assertEquals(1, Post.findAll().size());

		Post post = Post.find("byContent", "test post content").first();
		assertNotNull(post);
		assertNotNull(post.id);
		assertEquals("test post content", post.content);
		assertNotNull(post.creationDate);
		assertNotNull(post.author);

		assertHeaderEquals("Location", newRequest().host + "/posts/" + post.id, response);
	}

	@Test
	public void testGetAllPosts() {
		Fixtures.loadModels("data/posts.yml");

		Response response = GET("/posts");

		assertContentType("application/json", response);
		assertCharset("UTF-8", response);

		List<PostResponse> postsList = new Gson().fromJson(response.out.toString(), new TypeToken<List<PostResponse>>() {
		}.getType());
		assertEquals(2, postsList.size());
		
		PostResponse postOne = postsList.get(0);
		assertEquals("test content 1", postOne.content);
		assertEquals(1, postOne.attachments.size());

		PostResponse postTwo = postsList.get(1);
		assertEquals("test content 2", postTwo.content);
		assertEquals(0, postTwo.attachments.size());
	}

	@Test
	public void testGetPostsOrderedDesc() {
		Fixtures.loadModels("data/posts.yml");

		Response response = GET("/posts?sort=created_at");

		List<PostResponse> postsList = new Gson().fromJson(response.out.toString(), new TypeToken<List<PostResponse>>() {
		}.getType());
		assertEquals(2, postsList.size());
		assertEquals("test content 2", postsList.get(0).content);
		assertEquals("test content 1", postsList.get(1).content);
	}

	@Test
	public void testGetPostsLimited() {
		Fixtures.loadModels("data/posts.yml");

		Response response = GET("/posts?limit=1");

		List<PostResponse> postsList = new Gson().fromJson(response.out.toString(), new TypeToken<List<PostResponse>>() {
		}.getType());
		assertEquals(1, postsList.size());
		assertEquals("test content 1", postsList.get(0).content);
	}

	@Test
	public void testGetPostsPaginated() {
		Fixtures.loadModels("data/posts.yml");

		Response response = GET("/posts?from=1&limit=1");

		List<PostResponse> postsList = new Gson().fromJson(response.out.toString(), new TypeToken<List<PostResponse>>() {
		}.getType());
		assertEquals(1, postsList.size());
		assertEquals("test content 2", postsList.get(0).content);
	}

	@Test
	public void testGetSinglePost() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();

		Response response = GET("/posts/" + post.getId());
		String responseBody = response.out.toString();
		PostResponse json = new GsonBuilder().create().fromJson(responseBody, PostResponse.class);
		assertEquals(post.getId().longValue(), json.id);
		assertEquals("test content 1", json.content);
		assertEquals(post.author.id.longValue(), json.author.id);
		assertEquals(post.author.getDisplayName(), json.author.displayName);
		assertEquals(1238025600000l, json.creationTimestamp);
		assertEquals(0, json.comments.size());
	}

	@Test
	public void testGetPostNotFound() {
		Fixtures.loadModels("data/posts.yml");

		Response response = GET("/posts/" + NOT_EXISTING_POST_ID);
		assertIsNotFound(response);
	}

	@Test
	public void testPostCreationIsSecured() {
		Response response = POST("/posts", "application/json", new GsonBuilder().create().toJson(new PostRequest("test post content")));
		assertStatus(401, response);
	}

	@Test
	public void testPostEditingIsSecured() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();
		String body = new GsonBuilder().create().toJson(new PostRequest("new post content"));
		Response response = PUT("/posts/" + post.id, "application/json", body);
		assertStatus(401, response);
	}

	@Test
	public void testPostCanBeEditedByOwnerOnly() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();

		String bodyJson = new GsonBuilder().create().toJson(new PostRequest("new post content"));
		Response response = new RequestBuilder()
			.withPath("/posts/" + post.id)
			.withHttpMethod(HttpMethod.PUT)
			.withContentType(ContentType.APPLICATION_JSON)
			.withBody(bodyJson)
			.withUsername(BILLY_USERNAME)
			.withToken(BILLY_SECRET_TOKEN)
			.send();
		
		assertStatus(403, response);
	}

	@Test
	public void testPostEditing() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();

		String bodyJson = new GsonBuilder().create().toJson(new PostRequest("new post content"));
		
		Response response = new RequestBuilder()
		.withPath("/posts/" + post.id)
		.withHttpMethod(HttpMethod.PUT)
		.withContentType(ContentType.APPLICATION_JSON)
		.withBody(bodyJson)
		.send();
		
		assertIsOk(response);
		post.refresh();
		assertEquals("new post content", post.content);

		PostResponse postResponse = new GsonBuilder().create().fromJson(response.out.toString(), PostResponse.class);
		assertEquals(Long.valueOf(post.id), Long.valueOf(postResponse.id));
		assertEquals("new post content", postResponse.content);
		assertNotNull(postResponse.author);
		assertEquals("Superman", postResponse.author.displayName);
		assertEquals(Long.valueOf(post.author.id), Long.valueOf(postResponse.author.id));
	}

	@Test
	public void testPostDeletingIsSecured() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();
		Response response = DELETE("/posts/" + post.id);
		assertStatus(401, response);
	}

	@Test
	public void testPostCanBeDeletedByOwnerOnly() {
		Fixtures.loadModels("data/posts.yml");

		Post post = Post.find("byContent", "test content 1").first();

		Response response = new RequestBuilder()
			.withPath("/posts/" + post.id)
			.withHttpMethod(HttpMethod.DELETE)
			.withUsername(BILLY_USERNAME)
			.withToken(BILLY_SECRET_TOKEN)
			.send();
		
		assertStatus(403, response);
	}

	@Test
	public void testPostDeleting() {
		Fixtures.loadModels("data/posts.yml");

		int count = Post.findAll().size();
		assertEquals(2, count);
		Post post = Post.find("byContent", "test content 1").first();

		Response response = new RequestBuilder()
			.withPath("/posts/" + post.id)
			.withHttpMethod(HttpMethod.DELETE)
			.send();
		
		assertIsOk(response);

		count = Post.findAll().size();
		assertEquals(1, count);
	}
	
	@Test
	public void testEmptyPostCreationFails() {
		Fixtures.loadModels("data/user.yml");

		Response response = new RequestBuilder()
			.withPath("/posts")
			.withHttpMethod(HttpMethod.POST)
			.withContentType(ContentType.APPLICATION_JSON)
			.withBody("{\"content\":\"\"}")
			.send();

		assertStatus(400, response);
	}
}