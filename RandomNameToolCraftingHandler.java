package nurseangel.RandomNameTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ICraftingHandler;

/**
 * アイテムをクラフトしたときに反応するハンドラ
 */
public class RandomNameToolCraftingHandler implements ICraftingHandler
{
    Random random = new Random();

    private int minLength;
    private int maxLength;
    private String itemName = "";

    // 各アイテム
    private List<String> listParticle;
    private List<String> listSword;
    private List<String> listSwordEnd;
    private List<String> listHoe;
    private List<String> listHoeEnd;

    private int listParticleSize;

    private boolean isSMP; // SMPか否か
    private boolean isSmpChecked = false;

    /**
     * コンストラクタ
     *
     * @param listHoeEnd
     * @param listHoe
     * @param listSwordEnd
     * @param listSword
     * @param listParticle
     */
    public RandomNameToolCraftingHandler(int minLength, int maxLength, List<String> listParticle, List<String> listSword, List<String> listSwordEnd, List<String> listHoe, List<String> listHoeEnd)
    {
        this.minLength = minLength; // 最小
        this.maxLength = maxLength; // 最大
        this.listParticle = listParticle;
        this.listSword = listSword;
        this.listSwordEnd = listSwordEnd;
        this.listHoe = listHoe;
        this.listHoeEnd = listHoeEnd;
        listParticleSize = listParticle.size();
    }

    /**
     * クラフトした
     *
     * @param EntityPlayer
     *            プレイヤー。EntityPlayerMP/EntityClientPlayerMPの二回
     * @param ItemStack
     *            クラフトしたアイテム
     * @param IInventory
     *            プレイヤーのインベントリ？
     */
    @Override
    public void onCrafting(EntityPlayer player, ItemStack itemStack, IInventory craftMatrix)
    {
        /*
         * 剣と鍬が対象
         */
        Item item = itemStack.getItem();

        // 剣
        if (listSword != null)
        {
            if (item instanceof ItemSword)
            {
                setSwordName(player, itemStack);
            }
        }

        // 鍬
        if (listHoe != null)
        {
            if (item instanceof ItemHoe)
            {
                setHoeName(player, itemStack);
            }
        }
    }

    private void setSwordName(EntityPlayer player, ItemStack itemStack)
    {
        setName(player, itemStack, listSword, listSwordEnd);
    }

    private void setHoeName(EntityPlayer player, ItemStack itemStack)
    {
        setName(player, itemStack, listHoe, listHoeEnd);
    }

    private void setName(EntityPlayer player, ItemStack itemStack, List<String> listItem, List<String> listItemEnd)
    {
        // SSP/SMPの判断
        if (!isSmpChecked)
        {
            if (player instanceof EntityPlayerMP)
            {
                isSmpChecked = true;

                if (this.itemName.length() < 1)
                {
                    // SSPであればEntityClientPlayerMP→EntityPlayerMPと呼ばれ、EntityClientPlayerMPで必ずitemNameに何か入れる
                    // そのためEntityPlayerMPでitemNameが無ければSMPと判断する
                    isSMP = true;
                }
            }
        }

        /*
         * FIXME 実はSMPのクライアント側はisSMPを正しくチェックできてない。
         * SMPの場合EntityClientPlayerMPとEntityPlayerMPが別の場所で呼ばれるので、isSMPが共有できてないため。
         * そのためEntityClientPlayerMPで名前が付く→EntityPlayerMPで設定した別名に上書きされる、という変な処理になる。
         * これをどうにかするにはEntityClientPlayerMPで付けた名前を送信するとかしないといけないみたい？
         * ContainerRepairを見たけどよくわからんかった。
         *
         */

        // SSPかつサーバ側の処理であれば
        if (!isSMP && player instanceof EntityPlayerMP)
        {
            // EntityClientPlayerMPで設定した値を返す
            itemStack.setItemName(this.itemName);
            return;
        }

        // ディープコピー
        List<String> list = new ArrayList<String>(listItem);
        int listSize = list.size();
        String itemNameLocal = ""; // 最終名
        String name = "";
        // ランダム数繰り返し 最後の一回は別にするので-1
        int ran = random.nextInt(maxLength - minLength) + minLength - 1;

        for (int i = 0; i < ran; i++)
        {
            int pos = random.nextInt(listSize);
            name = list.get(pos);

            // もし\pがあれば助詞は無し
            if (name.endsWith(CONFIG_NOPARTICLE))
            {
                itemNameLocal += replaceLast(name);
            }
            else
            {
                // なければ概ね助詞あり
                itemNameLocal += name;
                int particleRnd = random.nextInt(10);

                if (particleRnd < 7)
                {
                    itemNameLocal += listParticle.get(random.nextInt(listParticleSize));
                }
            }

            // 重複防止に複製のリストから単語を削除
            list.remove(pos);
            listSize--;
        }

        // 最後だけはPARTICLEリストから
        itemNameLocal += listItemEnd.get(random.nextInt(listItemEnd.size()));

        // 名前を設定する
        if (isSMP)
        {
            // SMPであれば
            if (player instanceof EntityPlayerMP)
            {
                // サーバ側の場合のみ変更する
                itemStack.setItemName(itemNameLocal);
            }
            else
            {
                // クライアント側は何もしない
            }
        }
        else
        {
            // シングルであれば名前を設定し、かつインスタンスにも保存
            itemStack.setItemName(itemNameLocal);
            this.itemName = itemNameLocal;
        }
    }

    private static final String CONFIG_NOPARTICLE = "\\p";

    /**
     * 末尾の\pを削除するだけ
     *
     * @param string
     * @return
     */
    private String replaceLast(String string)
    {
        int pos = string.lastIndexOf(CONFIG_NOPARTICLE);

        if (pos > -1)
        {
            return string.substring(0, pos) + string.substring(pos + CONFIG_NOPARTICLE.length(), string.length());
        }
        else
        {
            return string;
        }
    }

    /**
     * 焼いた
     */
    @Override
    public void onSmelting(EntityPlayer player, ItemStack item)
    {
        // なにもしない
    }
}
