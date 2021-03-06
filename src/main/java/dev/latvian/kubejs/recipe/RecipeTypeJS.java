package dev.latvian.kubejs.recipe;

import dev.latvian.kubejs.docs.ID;
import dev.latvian.kubejs.util.UtilsJS;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RecipeTypeJS
{
	public final IRecipeSerializer serializer;
	public final Supplier<RecipeJS> factory;
	private final String string;

	public RecipeTypeJS(IRecipeSerializer s, Supplier<RecipeJS> f)
	{
		serializer = s;
		factory = f;
		string = s.getRegistryName().toString();
	}

	public RecipeTypeJS(@ID String id, Supplier<RecipeJS> f)
	{
		this(Objects.requireNonNull(ForgeRegistries.RECIPE_SERIALIZERS.getValue(UtilsJS.getMCID(id))), f);
	}

	public boolean isCustom()
	{
		return false;
	}

	@Override
	public String toString()
	{
		return string;
	}

	@Override
	public int hashCode()
	{
		return string.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return string.equals(obj.toString());
	}
}