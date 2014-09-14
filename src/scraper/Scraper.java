package scraper;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.mjorm.MongoDao;
import com.googlecode.mjorm.MongoDaoImpl;
import com.googlecode.mjorm.annotations.AnnotationsDescriptorObjectMapper;
import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.JauntException;
import com.jaunt.UserAgent;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/*
 * Scrapes through allrecipes.com and generates recipe objects based on their recipes.
 */
public class Scraper {
	public static void main(String[] args) {
		List<Recipe> recipes = new ArrayList<Recipe>();
		for (int currentPage = 1; currentPage <= 1000; currentPage++) {
			String recipesPageUrl = "http://allrecipes.com/recipes/main.aspx?Page="
					+ currentPage;
			try {
				UserAgent userAgent = new UserAgent(); // open HTML from a
														// String.
				userAgent.visit(recipesPageUrl);
				// getting the recipe icon element
				Elements recipeDivs = userAgent.doc
						.findEvery("< id=divGridItemWrapper>");
				// iterate through elements and scrape recipe information
				for (Element recipeDiv : recipeDivs) {
					// get link
					Element a = recipeDiv.findFirst("<a>");
					String recipeUrl = a.getAt("href");
					// scrape info
					UserAgent recipeAgent = new UserAgent();
					recipeAgent.visit(recipeUrl);
					// title
					Element recipeTitle = recipeAgent.doc
							.findFirst("< id=itemTitle>");
					String recipeTitleString = recipeTitle.getText();
					// imageUrl
					Element recipeImage = recipeAgent.doc
							.findFirst("< id=imgPhoto>");
					String recipeImageUrl = recipeImage.getAt("src");
					// description
					Element recipeDescription = recipeAgent.doc
							.findFirst("< id=lblDescription>");
					String recipeDescriptionString = recipeDescription
							.getText();
					// get tags (at the moment only the ingredients of the
					// recipe...)
					ArrayList<String> tags = new ArrayList<String>();
					Elements recipeIngredients = recipeAgent.doc
							.findEvery("< class=ingredient-name>");
					for (Element recipeIngredient : recipeIngredients) {
						String recipeIngredientString = recipeIngredient
								.getText();
						recipeIngredientString = cleanIngredientText(recipeIngredientString);
						tags.add(recipeIngredientString);
					}
					// create Recipe object
					Recipe recipe = new Recipe();
					recipe.setTitle(recipeTitleString);
					recipe.setImageUrl(recipeImageUrl);
					recipe.setRecipeUrl(recipeUrl);
					recipe.setDescription(recipeDescriptionString);
					recipe.setTags(tags);
					recipes.add(recipe);
				}
			} catch (JauntException e) {
				System.err.println(e);
			}
			
			// Mongo mostly from https://code.google.com/p/mongo-java-orm/#Annotations_Mapping
			// and http://www.javawebdevelop.com/283765/
			
			// connect to mongo
			Mongo mongo;
			try {
				mongo = new Mongo(new MongoURI("insert uri here..."));
				// create object mapper and add classes
				AnnotationsDescriptorObjectMapper objectMapper = new AnnotationsDescriptorObjectMapper();
				objectMapper.addClass(Recipe.class);
				// create MongoDao
				MongoDao dao = new MongoDaoImpl(mongo.getDB("dbName"), objectMapper); //"recipes" is db name
				for(Recipe recipe : recipes) {
					recipe = dao.createObject("recipes", recipe);
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

			
		}
	}
	
	public static String cleanIngredientText (String ingredientText) {
		// strip text after "," and "-", and " to "
		int commaIndex = ingredientText.indexOf(",");
		if (commaIndex != -1) {
			ingredientText = ingredientText
					.substring(0, commaIndex);
		}
		int dashIndex = ingredientText.indexOf(" - ");
		if (dashIndex != -1) {
			ingredientText = ingredientText
					.substring(0, dashIndex);
		}
		int toIndex = ingredientText.indexOf(" to ");
		if (dashIndex != -1) {
			ingredientText = ingredientText
					.substring(0, toIndex);
		}
		return ingredientText;
	}

}
