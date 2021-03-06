package dev.latvian.kubejs;

import dev.latvian.kubejs.block.BlockRegistryEventJS;
import dev.latvian.kubejs.block.KubeJSBlockEventHandler;
import dev.latvian.kubejs.client.KubeJSClient;
import dev.latvian.kubejs.entity.KubeJSEntityEventHandler;
import dev.latvian.kubejs.event.EventJS;
import dev.latvian.kubejs.fluid.FluidRegistryEventJS;
import dev.latvian.kubejs.fluid.KubeJSFluidEventHandler;
import dev.latvian.kubejs.integration.IntegrationManager;
import dev.latvian.kubejs.item.ItemRegistryEventJS;
import dev.latvian.kubejs.item.KubeJSItemEventHandler;
import dev.latvian.kubejs.net.KubeJSNet;
import dev.latvian.kubejs.player.KubeJSPlayerEventHandler;
import dev.latvian.kubejs.recipe.KubeJSRecipeEventHandler;
import dev.latvian.kubejs.script.ScriptFile;
import dev.latvian.kubejs.script.ScriptFileInfo;
import dev.latvian.kubejs.script.ScriptManager;
import dev.latvian.kubejs.script.ScriptPack;
import dev.latvian.kubejs.script.ScriptPackInfo;
import dev.latvian.kubejs.script.ScriptSource;
import dev.latvian.kubejs.script.ScriptType;
import dev.latvian.kubejs.server.KubeJSServerEventHandler;
import dev.latvian.kubejs.util.UtilsJS;
import dev.latvian.kubejs.world.KubeJSWorldEventHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author LatvianModder
 */
@Mod(KubeJS.MOD_ID)
public class KubeJS
{
	public static KubeJS instance;
	public static final String MOD_ID = "kubejs";
	public static final String MOD_NAME = "KubeJS";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public final KubeJSCommon proxy;
	public static boolean nextClientHasClientMod = false;

	public static ScriptManager startupScriptManager, clientScriptManager;

	public KubeJS()
	{
		Locale.setDefault(Locale.US);

		try
		{
			if (!Class.forName("org.spongepowered.asm.mixin.Mixin").isAnnotation())
			{
				throw new ClassNotFoundException();
			}
		}
		catch (ClassNotFoundException ex)
		{
			throw new RuntimeException("Mixins not found! Please install MixinBootstrap mod!");
		}

		instance = this;
		startupScriptManager = new ScriptManager(ScriptType.STARTUP);
		clientScriptManager = new ScriptManager(ScriptType.CLIENT);
		//noinspection Convert2MethodRef
		proxy = DistExecutor.runForDist(() -> () -> new KubeJSClient(), () -> () -> new KubeJSCommon());

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);

		new KubeJSOtherEventHandler().init();
		new KubeJSServerEventHandler().init();
		new KubeJSWorldEventHandler().init();
		new KubeJSPlayerEventHandler().init();
		new KubeJSEntityEventHandler().init();
		new KubeJSBlockEventHandler().init();
		new KubeJSItemEventHandler().init();
		new KubeJSRecipeEventHandler().init();
		new KubeJSFluidEventHandler().init();

		File folder = getGameDirectory().resolve("kubejs").toFile();

		if (!folder.exists())
		{
			folder.mkdirs();
		}

		proxy.init(folder);

		File startupFolder = new File(folder, "startup");

		if (!startupFolder.exists())
		{
			startupFolder.mkdirs();

			try
			{
				try (PrintWriter scriptsJsonWriter = new PrintWriter(new FileWriter(new File(startupFolder, "scripts.json"))))
				{
					scriptsJsonWriter.println("{");
					scriptsJsonWriter.println("	\"scripts\": [");
					scriptsJsonWriter.println("		{\"file\": \"example.js\"}");
					scriptsJsonWriter.println("	]");
					scriptsJsonWriter.println("}");
				}

				try (PrintWriter exampleJsWriter = new PrintWriter(new FileWriter(new File(startupFolder, "example.js"))))
				{
					exampleJsWriter.println("console.info('Hello, World! (You will only see this line once in console, during startup)')");
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		startupScriptManager.unload();

		if (new File(startupFolder, "scripts.json").exists())
		{
			LOGGER.warn("KubeJS no longer uses scripts.json file, please delete it! To reorder scripts, add '// priority: 10' on top of them. Default priority is 0.");
		}

		ScriptPack pack = new ScriptPack(startupScriptManager, new ScriptPackInfo("startup", ""));
		loadScripts(pack, startupFolder, "");

		for (ScriptFileInfo fileInfo : pack.info.scripts)
		{
			ScriptSource scriptSource = info -> new FileReader(new File(startupFolder, info.file));

			Throwable error = fileInfo.preload(scriptSource);

			if (error == null)
			{
				pack.scripts.add(new ScriptFile(pack, fileInfo, scriptSource));
			}
			else
			{
				LOGGER.error("Failed to pre-load script file " + fileInfo.location + ": " + error);
			}
		}

		pack.scripts.sort(null);
		startupScriptManager.packs.put(pack.info.namespace, pack);

		startupScriptManager.load();

		new BlockRegistryEventJS().post(ScriptType.STARTUP, KubeJSEvents.BLOCK_REGISTRY);
		new ItemRegistryEventJS().post(ScriptType.STARTUP, KubeJSEvents.ITEM_REGISTRY);
		new FluidRegistryEventJS().post(ScriptType.STARTUP, KubeJSEvents.FLUID_REGISTRY);
	}

	private void loadScripts(ScriptPack pack, File dir, String path)
	{
		File[] files = dir.listFiles();

		if (files != null && files.length > 0)
		{
			for (File file : files)
			{
				if (file.isDirectory())
				{
					loadScripts(pack, file, path.isEmpty() ? file.getName() : (path + "/" + file.getName()));
				}
				else if (file.getName().endsWith(".js"))
				{
					pack.info.scripts.add(new ScriptFileInfo(pack.info, path.isEmpty() ? file.getName() : (path + "/" + file.getName())));
				}
			}
		}
	}

	public static String appendModId(String id)
	{
		return id.indexOf(':') == -1 ? (MOD_ID + ":" + id) : id;
	}

	public static Path getGameDirectory()
	{
		return FMLPaths.GAMEDIR.get();
	}

	public static void verifyFilePath(Path path) throws IOException
	{
		if (!path.normalize().toAbsolutePath().startsWith(getGameDirectory()))
		{
			throw new IOException("You can't access files outside Minecraft directory!");
		}
	}

	public static void verifyFilePath(File file) throws IOException
	{
		verifyFilePath(file.toPath());
	}

	private void setup(FMLCommonSetupEvent event)
	{
		UtilsJS.init();
		IntegrationManager.init();
		KubeJSNet.init();
		new EventJS().post(ScriptType.STARTUP, KubeJSEvents.INIT);
	}

	private void loadComplete(FMLLoadCompleteEvent event)
	{
		new EventJS().post(ScriptType.STARTUP, KubeJSEvents.POSTINIT);
	}
}