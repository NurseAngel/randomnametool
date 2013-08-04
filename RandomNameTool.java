package mods.nurseangel.randomnametool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import mods.nurseangel.randomnametool.proxy.CommonProxy;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class RandomNameTool {
	@SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.SERVER_PROXY_CLASS)
	public static CommonProxy proxy;

	private Configuration cfg;
	private boolean isEnabled = false;
	private boolean isEnabledSword = false;
	private boolean isEnabledHoe = false;
	private int maxLength;
	private int minLength;
	private File swordNameFile;

	/**
	 * コンストラクタ的なもの
	 *
	 * @param event
	 */
	@Mod.EventHandler
	public void modPreInit(FMLPreInitializationEvent event) {
		// コンフィグを読み込む
		cfg = new Configuration(event.getSuggestedConfigurationFile());

		try {
			cfg.load();
			// 全体の有効無効
			isEnabled = cfg.get(Configuration.CATEGORY_GENERAL, "Enable", true).getBoolean(true);
			// 個別アイテムの有効無効
			isEnabledSword = cfg.get(Configuration.CATEGORY_GENERAL, "EnableSword", true).getBoolean(true);
			isEnabledHoe = cfg.get(Configuration.CATEGORY_GENERAL, "EnableHoe", true).getBoolean(true);
			// 最小/最大長
			minLength = cfg.get(Configuration.CATEGORY_GENERAL, "minLength", 1).getInt();
			maxLength = cfg.get(Configuration.CATEGORY_GENERAL, "maxLength", 10).getInt();

			// 補正
			if (maxLength < 1) {
				maxLength = 1;
			} else if (maxLength > 100) {
				maxLength = 100;
			}

			if (minLength < 1) {
				minLength = 1;
			} else if (minLength > 100) {
				minLength = 100;
			}

			if (minLength > maxLength) {
				minLength = maxLength;
			}
		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, Reference.MOD_NAME + "Config Load failed... ");
		} finally {
			cfg.save();
		}

		// 名前ファイル用のファイル名
		this.swordNameFile = new File(event.getModConfigurationDirectory(), Reference.MOD_NAME + ".txt");
	}

	/**
	 * メイン処理的なもの
	 *
	 * @param event
	 */
	@Mod.EventHandler
	public void modInit(FMLInitializationEvent event) {
		// 全体の有効
		if (!isEnabled || (!isEnabledSword && !isEnabledHoe)) {
			return;
		}

		// 名前ファイルを読み込み
		try {
			readNameList();
		} catch (IOException e) {
			FMLLog.log(Level.SEVERE, Reference.MOD_NAME + " Name File Read failed... ");
			return;
		}

		// 個別の有効無効
		if (!isEnabledSword) {
			listSword = null;
			listSwordEnd = null;
		}

		if (!isEnabledHoe) {
			listHoe = null;
			listHoeEnd = null;
		}

		// ハンドラをセット
		ICraftingHandler handler = new RandomNameToolCraftingHandler(minLength, maxLength, listParticle, listSword, listSwordEnd, listHoe, listHoeEnd);
		GameRegistry.registerCraftingHandler(handler);
	}

	private List<String> listSword = new ArrayList<String>();
	private List<String> listSwordEnd = new ArrayList<String>();
	private List<String> listHoe = new ArrayList<String>();
	private List<String> listHoeEnd = new ArrayList<String>();
	private List<String> listParticle = new ArrayList<String>();
	private List<String> listParticleEnd = new ArrayList<String>(); // dummy

	/**
	 * 名前ファイルから読み込む
	 *
	 * @return List<String> 名前の入ったリスト
	 * @throws IOException
	 */
	private void readNameList() throws IOException {
		/**
		 * もうちょっとこう 良いやりかたがありそうな気がするんだがどうだろう
		 */
		BufferedReader br = new BufferedReader(new FileReader(swordNameFile));
		// シャドーコピー
		List<String> now = listSword;
		List<String> nowEnd = listSwordEnd;
		String str;

		while (true) {
			// 読み込み
			str = br.readLine();

			if (str == null) {
				break;
			}

			// 文字のない行、#ではじまる行は無視
			str = str.trim();

			if (str.length() < 1 || str.startsWith(CONFIG_COMMENT)) {
				continue;
			}

			// 特定の文字があれば代入先リストを変更
			if (str.startsWith(CONFIG_CHANGE)) {
				if (str.equals(CONFIG_HOE)) {
					now = listHoe;
					nowEnd = listHoeEnd;
				} else if (str.equals(CONFIG_PARTICLE)) {
					now = listParticle;
					nowEnd = listParticleEnd;
				}

				continue;
			}

			// リストに挿入
			if (str.endsWith(CONFIG_END)) {
				nowEnd.add(replaceLast(str));
			} else {
				now.add(str);
			}
		}

		br.close();
	}

	private static final String CONFIG_COMMENT = "#";
	private static final String CONFIG_CHANGE = "//";
	private static final String CONFIG_HOE = "//Hoe";
	private static final String CONFIG_PARTICLE = "//Particle";
	private static final String CONFIG_END = "\\n";

	private String replaceLast(String string) {
		int pos = string.lastIndexOf(CONFIG_END);

		if (pos > -1) {
			return string.substring(0, pos) + string.substring(pos + CONFIG_END.length(), string.length());
		} else {
			return string;
		}
	}
}
