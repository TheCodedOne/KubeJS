package latmod.cmdscripts;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.*;
import ftb.lib.EventBusHelper;
import latmod.cmdscripts.cmd.*;

@Mod(modid = CmdScripts.MOD_ID, name = "CommandScripts", version = "@VERSION@", dependencies = "required-after:FTBL", acceptableRemoteVersions = "*")
public class CmdScripts
{
	protected static final String MOD_ID = "CommandScripts";
	
	@Mod.Instance(CmdScripts.MOD_ID)
	public static CmdScripts inst;
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		EventBusHelper.register(new CmdScriptsEventHandler());
	}
	
	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent e)
	{
		e.registerServerCommand(new CommandScript());
		e.registerServerCommand(new CommandFillBlocks());
		e.registerServerCommand(new CommandSignText());
	}
}