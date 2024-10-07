package net.mehvahdjukaar.every_compat.modules.forge.tropicraft;

import com.google.gson.JsonObject;
import net.mehvahdjukaar.every_compat.EveryCompat;
import net.mehvahdjukaar.every_compat.api.SimpleEntrySet;
import net.mehvahdjukaar.every_compat.api.SimpleModule;
import net.mehvahdjukaar.every_compat.dynamicpack.ServerDynamicResourcesHandler;
import net.mehvahdjukaar.moonlight.api.resources.RPUtils;
import net.mehvahdjukaar.moonlight.api.resources.ResType;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.set.wood.WoodTypeRegistry;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.tropicraft.Tropicraft;
import net.tropicraft.core.common.block.BoardwalkBlock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

//SUPPORT: v9.5.2-FINAL
public class TropicraftModule extends SimpleModule {

    public final SimpleEntrySet<WoodType, BoardwalkBlock> boardwalks;

    public TropicraftModule(String modId) {
        super(modId, "tc");

        boardwalks = SimpleEntrySet.builder(WoodType.class, "boardwalk",
                        () -> getModBlock("mangrove_boardwalk", BoardwalkBlock.class), () -> WoodTypeRegistry.getValue(new ResourceLocation("mangrove")),
                        w -> new BoardwalkBlock(copySlabProperties(w).noOcclusion())
                )
                .requiresChildren("slab")
                //TEXTURE: using planks
                //REASON: tropicraft has its own planks texture for mangrove, Below is use wood mods' planks texture
                .addModelTransform(m -> m.addModifier((s, resLoc, woodType) -> s.replace(
                        "\"tropicraft:block/mangrove_planks\"",
                        "\"" + woodType.getNamespace() + ":block/" + woodType.getTypeName() + "_planks\"")
                ))
                .addTag(BlockTags.MINEABLE_WITH_AXE, Registry.BLOCK_REGISTRY)
                .setTab(() -> Tropicraft.TROPICRAFT_ITEM_GROUP)
                .build();
        this.addEntry(boardwalks);

    }

    public BlockBehaviour.Properties copySlabProperties(WoodType wood) {
        Block slab = wood.getBlockOfThis("slab");
        return (slab != null) ? Utils.copyPropertySafe(slab) : Utils.copyPropertySafe(Blocks.OAK_SLAB);
    }
    @Override
    // RECIPES - Similar reason as .addMoelTransform()
    public void addDynamicServerResources(ServerDynamicResourcesHandler handler, ResourceManager manager) {
        super.addDynamicServerResources(handler, manager);

        ResourceLocation recipePath = modRes("mangrove_boardwalk");

        boardwalks.blocks.forEach((wood, block) -> {

            try (InputStream recipeStream = manager.getResource(ResType.RECIPES.getPath(recipePath))
                    .orElseThrow(() -> new FileNotFoundException("Failed to open the recipe @ " + recipePath)).open()) {

                JsonObject recipe = RPUtils.deserializeJson(recipeStream);

                JsonObject underKey = recipe.getAsJsonObject("key").getAsJsonObject("X");

                // Editing the JSON
                underKey.addProperty("item", Utils.getID(wood.getBlockOfThis("slab")).toString());
                recipe.getAsJsonObject("result").addProperty("item", Utils.getID(block).toString());

                // Adding to the resource
                String newPath = shortenedId() +"/"+ wood.getAppendableId() + "_boardwalk";

                handler.dynamicPack.addJson(EveryCompat.res(newPath), recipe, ResType.RECIPES);

            } catch (IOException e) {
                handler.getLogger().error("Failed to generate the boardwalk recipe");
            }
        });
    }
}