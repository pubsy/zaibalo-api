package controllers.posts;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import controllers.posts.service.PostsService;
import controllers.posts.service.impl.PostsServiceImpl;
import models.Post;
import models.PostRating;
import models.Similarity;
import models.User;
import play.db.jpa.GenericModel.JPAQuery;
import play.db.jpa.JPABase;
import play.mvc.Http.Header;
import play.mvc.Util;
import play.mvc.With;
import ch.halarious.core.HalBaseResource;
import ch.halarious.core.HalResource;

import com.google.gson.GsonBuilder;

import controllers.BasicController;
import controllers.security.Secured;
import controllers.security.Security;

@With(Security.class)
public class Posts extends BasicController {

    private static PostsService postsService = new PostsServiceImpl();

	public static void getPostsByTag(String name, String sort, int from, int limit) {
		renderPostsListJson(sort, from, limit, "content like ?1", "%#" + name + "%");
	}
	
	public static void getPosts(String sort, int from, int limit) {
		renderPostsListJson(sort, from, limit, "");
	}

    public static void getRecommendedPosts(){
        response.setContentTypeIfNotSet("application/json");

        User user = Security.getAuthenticatedUser();

        if(user == null){

        }

        List<Post> postsList = postsService.getRecommendedPosts(user);
        PostsListResource postsListResource = PostsListResource.convertToPostsListResource(postsList, null);
        renderJSON(convertToHalResponse(postsListResource));
    }

	public static void getPostsCount(){
		long count = Post.count();
		renderCountJson(count);
	}

	public static void getPostsByTagCount(String name){
		long count = Post.count("content like ?1", "%#" + name + "%");
		renderCountJson(count);
	}
	
	@Secured
	public static void createPost() {
		User user = Security.getAuthenticatedUser();

		PostRequest postJSON = new GsonBuilder().create().
				fromJson(new InputStreamReader(request.body), PostRequest.class);
		
		if(!postJSON.isValid()){
			badRequest();
		}
		
		Post post = new Post();
		post.content = postJSON.content;
		post.author = user;
		post.save();

		String location = request.host + "/posts/" + post.id;
		response.headers.put("Location", new Header("Location", location));
		response.setContentTypeIfNotSet("application/json");
		response.status = 201;
		
		HalBaseResource postResponseJSON = PostResource.convertSinglePostResponse(post, user);
		renderJSON(convertToHalResponse(postResponseJSON));
	}

	public static void getPost(long id) {
		Post post = Post.findById(id);
		if (post == null) {
			notFound();
		}
		User user = Security.getAuthenticatedUser();
	
		HalResource postResponseJSON = PostResource.convertSinglePostResponse(post, user);
		renderJSON(convertToHalResponse(postResponseJSON));
	}

	@Secured
	public static void editPost(long id) {
		PostRequest postJSON = new GsonBuilder().create().
				fromJson(new InputStreamReader(request.body), PostRequest.class);
		Post post = Post.findById(id);
		if (post == null) {
			notFound();
		}

		Security.verifyOwner(post.author);

		post.content = postJSON.content;
		post.save();

		response.setContentTypeIfNotSet("application/json");
		User user = Security.getAuthenticatedUser();
		
		HalBaseResource postResponseJSON = PostResource.convertSinglePostResponse(post, user);
		renderJSON(convertToHalResponse(postResponseJSON));
	}

	@Secured
	public static void deletePost(long id) {
		Post post = Post.findById(id);
		if (post == null) {
			notFound();
		}

		Security.verifyOwner(post.author);

		post.delete();
		ok();
	}

	@Util
	public static void renderPostsListJson(String sort, int from, int limit, String query, Object... params) {
		if("created_at".equals(sort)){
			query += " order by creationDate desc";
		}
		JPAQuery postsQuery = Post.find(query, params);
		
		limit = (limit == 0) ? 10 : limit;
		List<Post> postsList = postsQuery.from(from).fetch(limit);
		User user = Security.getAuthenticatedUser();
		
		PostsListResource postsListResource = PostsListResource.convertToPostsListResource(postsList, user);
		renderJSON(convertToHalResponse(postsListResource));
	}

	@Util
	public static void renderCountJson(long count) {
		renderJSON("{\"count\":" + count +"}");
	}
}
