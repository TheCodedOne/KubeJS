package dev.latvian.kubejs.recipe.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.latvian.kubejs.item.EmptyItemStackJS;
import dev.latvian.kubejs.item.ItemStackJS;
import dev.latvian.kubejs.item.ingredient.IngredientJS;
import dev.latvian.kubejs.recipe.RecipeExceptionJS;
import dev.latvian.kubejs.recipe.RecipeJS;
import dev.latvian.kubejs.script.ScriptType;
import dev.latvian.kubejs.util.ListJS;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class CustomRecipeJS extends RecipeJS
{
	public static final Supplier<RecipeJS> FACTORY = CustomRecipeJS::new;

	private String inputKey;
	private int inputType;
	private String outputKey;
	private int outputType;

	public CustomRecipeJS()
	{
		inputKey = "";
		inputType = -1;
		outputKey = "";
		outputType = -1;
	}

	@Override
	public void create(ListJS args)
	{
		throw new RecipeExceptionJS("Can't create custom recipe for type " + id + "!");
	}

	private boolean addInput(String k)
	{
		JsonElement e = json.get(k);

		if (e == null || e.isJsonNull())
		{
			return false;
		}

		if (e.isJsonArray())
		{
			for (JsonElement e1 : e.getAsJsonArray())
			{
				IngredientJS i = IngredientJS.ingredientFromRecipeJson(e1);

				if (!i.isEmpty())
				{
					inputItems.add(i);
				}
			}

			inputKey = k;
			inputType = 1;
			return true;
		}

		IngredientJS i = IngredientJS.ingredientFromRecipeJson(e);

		if (!i.isEmpty())
		{
			inputItems.add(i);
			inputKey = k;
			inputType = 0;
			return true;
		}

		return false;
	}

	private boolean addOutput(String k)
	{
		JsonElement e = json.get(k);

		if (e == null || e.isJsonNull())
		{
			return false;
		}

		if (e.isJsonArray())
		{
			for (JsonElement e1 : e.getAsJsonArray())
			{
				ItemStackJS i = ItemStackJS.resultFromRecipeJson(e1);

				if (!i.isEmpty())
				{
					outputItems.add(i);
				}
			}

			outputKey = k;
			outputType = 1;
			return true;
		}

		ItemStackJS i = ItemStackJS.resultFromRecipeJson(e);

		if (!i.isEmpty())
		{
			if (e.isJsonPrimitive())
			{
				outputType = 2;

				if (json.has("count"))
				{
					i.count(json.get("count").getAsInt());
				}
			}
			else
			{
				outputType = 0;
			}

			outputKey = k;
			outputItems.add(i);
			return true;
		}

		return false;
	}

	@Override
	public void deserialize()
	{
		inputItems.clear();
		outputItems.clear();
		inputKey = "";
		inputType = -1;
		outputKey = "";
		outputType = -1;

		if (originalRecipe == null || originalRecipe.isDynamic())
		{
			return;
		}

		try
		{
			if (!addInput("ingredient")
					&& !addInput("ingredients")
					&& !addInput("in")
					&& !addInput("input")
					&& !addInput("inputs")
					&& !addInput("itemInput")
					&& !addInput("infusionInput")
			)
			{
				ScriptType.SERVER.console.logger.debug("! " + this + ": Couldn't find any input items!");
			}
		}
		catch (Exception ex)
		{
		}

		try
		{
			if (!addOutput("result")
					&& !addOutput("results")
					&& !addOutput("out")
					&& !addOutput("output")
					&& !addOutput("outputs")
					&& !addOutput("itemOutput")
					&& !addOutput("mainOutput")
					&& !addOutput("secondaryOutput")
			)
			{
				ScriptType.SERVER.console.logger.debug("! " + this + ": Couldn't find any output items!");
			}
		}
		catch (Exception ex)
		{
		}
	}

	@Override
	public void serialize()
	{
		if (inputType != -1 && !inputKey.isEmpty())
		{
			if (inputType == 1)
			{
				JsonArray a = new JsonArray();

				for (IngredientJS in : inputItems)
				{
					a.add(in.toJson());
				}

				json.add(inputKey, a);
			}
			else if (inputType == 0)
			{
				json.add(inputKey, (inputItems.isEmpty() ? EmptyItemStackJS.INSTANCE : inputItems.get(0)).toJson());
			}
		}

		if (outputType != -1 && !outputKey.isEmpty())
		{
			if (outputType == 1)
			{
				JsonArray a = new JsonArray();

				for (ItemStackJS in : outputItems)
				{
					a.add(in.toResultJson());
				}

				json.add(outputKey, a);
			}
			else if (outputType == 2)
			{
				json.addProperty(outputKey, (outputItems.isEmpty() ? EmptyItemStackJS.INSTANCE : outputItems.get(0)).getId());
				json.addProperty("count", (outputItems.isEmpty() ? EmptyItemStackJS.INSTANCE : outputItems.get(0)).getCount());
			}
			else if (outputType == 0)
			{
				json.add(outputKey, (outputItems.isEmpty() ? EmptyItemStackJS.INSTANCE : outputItems.get(0)).toResultJson());
			}
		}
	}
}