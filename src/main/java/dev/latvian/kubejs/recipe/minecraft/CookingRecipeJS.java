package dev.latvian.kubejs.recipe.minecraft;

import dev.latvian.kubejs.item.ItemStackJS;
import dev.latvian.kubejs.item.ingredient.IngredientJS;
import dev.latvian.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.kubejs.recipe.RecipeJS;
import dev.latvian.kubejs.util.ListJS;
import net.minecraft.item.crafting.IRecipeSerializer;

/**
 * @author LatvianModder
 */
public class CookingRecipeJS extends RecipeJS
{
	public enum Type
	{
		SMELTING(IRecipeSerializer.SMELTING),
		BLASTING(IRecipeSerializer.BLASTING),
		SMOKING(IRecipeSerializer.SMOKING),
		CAMPFIRE(IRecipeSerializer.CAMPFIRE_COOKING);

		public final IRecipeSerializer serializer;

		Type(IRecipeSerializer s)
		{
			serializer = s;
		}
	}

	public final Type cookingType;

	public CookingRecipeJS(Type c)
	{
		cookingType = c;
	}

	@Override
	public void create(ListJS args)
	{
		ItemStackJS result = ItemStackJS.of(args.get(0));

		if (result.isEmpty())
		{
			throw new RecipeExceptionJS("Cooking recipe result " + args.get(0) + " is not a valid item!");
		}

		outputItems.add(result);

		IngredientJS ingredient = IngredientJS.of(args.get(1));

		if (ingredient.isEmpty())
		{
			throw new RecipeExceptionJS("Cooking recipe ingredient " + args.get(1) + " is not a valid ingredient!");
		}

		inputItems.add(ingredient);

		if (args.size() >= 3)
		{
			xp(((Number) args.get(2)).floatValue());
		}

		if (args.size() >= 4)
		{
			cookingTime(((Number) args.get(3)).intValue());
		}
	}

	@Override
	public void deserialize()
	{
		ItemStackJS result = ItemStackJS.resultFromRecipeJson(json.get("result"));

		if (result.isEmpty())
		{
			throw new RecipeExceptionJS("Cooking recipe result " + json.get("result") + " is not a valid item!");
		}

		outputItems.add(result);

		IngredientJS ingredient = IngredientJS.ingredientFromRecipeJson(json.get("ingredient"));

		if (ingredient.isEmpty())
		{
			throw new RecipeExceptionJS("Cooking recipe ingredient " + json.get("ingredient") + " is not a valid ingredient!");
		}

		inputItems.add(ingredient);
	}

	@Override
	public void serialize()
	{
		json.add("ingredient", inputItems.get(0).toJson());
		json.add("result", outputItems.get(0).toResultJson());
	}

	public CookingRecipeJS xp(float xp)
	{
		json.addProperty("experience", Math.max(0F, xp));
		return this;
	}

	public CookingRecipeJS cookingTime(int time)
	{
		json.addProperty("cookingtime", Math.max(0, time));
		return this;
	}
}