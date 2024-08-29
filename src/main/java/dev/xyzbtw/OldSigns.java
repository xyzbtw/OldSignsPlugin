package dev.xyzbtw;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.events.world.EventChunk;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.client.api.utils.WorldUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OldSigns module
 *
 * @author xyzbtw
 */
public class OldSigns extends ToggleableModule {

	public OldSigns() {
		super("OldSigns", ModuleCategory.RENDER);
		this.registerSettings(
				render,
				renderColor,
				tracer,
				tracerColor
		);
	}
	private BooleanSetting render = new BooleanSetting("Box", true);
	private ColorSetting renderColor = new ColorSetting("BoxColor", new Color(255, 166, 0, 100)).setVisibility(() -> render.getValue());

	private BooleanSetting tracer = new BooleanSetting("Tracer", true);
	private ColorSetting tracerColor = new ColorSetting("TracerColor", new Color(255, 166, 0, 100)).setVisibility(() -> tracer.getValue());

	private final Pattern fullYearsPattern = Pattern.compile("202[0-9]");
	private final Pattern fullDatesPattern = Pattern.compile("\\b(\\d{1,2}[-/\\. _,'+]\\d{1,2}[-/\\. _,'+]\\d{2,4}|\\d{4}[-/\\. _,'+]\\d{1,2}[-/\\. _,'+]\\d{1,2})\\b");
	CopyOnWriteArrayList<SignBlockEntity> signs = new CopyOnWriteArrayList<>();
	private final HashMap<ChunkPos, Boolean> chunkCache = new HashMap<>();

	@Subscribe
	public void onChunk(EventChunk event){
		if (mc.player == null || mc.level == null) return;

		checkOld(WorldUtils.getBlockEntities(false), mc.level.getChunk(event.getChunkPos().getMiddleBlockX(), event.getChunkPos().getMiddleBlockZ()));
	}
	@Subscribe
	public void onRender3D(EventRender3D event) {
		IRenderer3D renderer = event.getRenderer();
		renderer.begin(event.getMatrixStack());

		if (!signs.isEmpty()) {
			for (SignBlockEntity sign : signs) {
				if (sign == null) continue;

				if (tracer.getValue())
					renderer.drawLine(mc.player.getEyePosition(), sign.getBlockPos().getCenter(), tracerColor.getValueRGB());
				if (render.getValue())
					renderer.drawBox(sign.getBlockPos(), true, false, renderColor.getValueRGB());
			}

			List<SignBlockEntity> inRange = signs
					.stream()
					.filter(pos -> Vec3.atCenterOf(pos.getBlockPos()).closerThan(mc.player.position(), mc.options.renderDistance().get() * 16 + 32))
					.toList();
			signs.removeAll(inRange);
		}

		renderer.end();
	}


	@Override
	public void onEnable() {
		super.onEnable();
		if (mc.player == null || mc.level == null) return;

		BlockPos pos = mc.player.blockPosition();
		int viewDistance = mc.options.renderDistance().get();

		int startChunkX = (pos.getX() - (viewDistance * 16)) >> 4;
		int endChunkX = (pos.getX() + (viewDistance * 16)) >> 4;
		int startChunkZ = (pos.getZ() - (viewDistance * 16)) >> 4;
		int endChunkZ = (pos.getZ() + (viewDistance * 16)) >> 4;

		for (int x = startChunkX; x < endChunkX; x++) {
			for (int z = startChunkZ; z < endChunkZ; z++) {
				if (mc.level.hasChunkAt(x, z)) {
					LevelChunk chunk = mc.level.getChunk(x, z);
					checkOld(WorldUtils.getBlockEntities(false), chunk);
				}
			}
		}
	}

	private CopyOnWriteArrayList<SignBlockEntity> getNearbySigns(LevelChunk chunk) {
		CopyOnWriteArrayList<SignBlockEntity> signs = new CopyOnWriteArrayList<>();
		Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();

        blockEntities.forEach((pos, entity) -> {
			if (entity instanceof SignBlockEntity sbe) signs.add(sbe);
		});

		return signs;
	}

	private boolean isSignEmpty(SignBlockEntity sbe) {
		return !sbe.getFrontText().hasMessage(mc.player) && !sbe.getBackText().hasMessage(mc.player);
	}

	private void checkOld(List<BlockEntity> theseSigns, LevelChunk chunk) {
		if (mc.level == null) return;
		ArrayList<String> lines = new ArrayList<>();
		for(BlockEntity signBlockEntity : theseSigns) {
			if (!(signBlockEntity instanceof SignBlockEntity)) continue;
			boolean couldBeOld = false;
			ResourceKey<Level> dimension = mc.level.dimension();
			if (!String.join(" ", lines).contains("**Pre-1.19 Sign restored by 0xTas' SignHistorian**")) {
				WoodType woodType = WoodType.BAMBOO;
				Block block = signBlockEntity.getBlockState().getBlock();
				if (block instanceof SignBlock signBlock) woodType = SignBlock.getWoodType(signBlock);

				if (woodType == WoodType.OAK) {
					CompoundTag metadata = signBlockEntity.getUpdateTag();
					if (!metadata.toString().contains("{\"extra\":[") && !lines.isEmpty()) {
						String testString = String.join(" ", lines);
						Matcher fullYearsMatcher = fullYearsPattern.matcher(testString);

						if (!fullYearsMatcher.find()) {
							boolean invalidDate = false;
							Matcher dateMatcher = fullDatesPattern.matcher(testString);
							while (dateMatcher.find()) {
								String dateStr = dateMatcher.group();
								LocalDate date = parseDate(dateStr);
								if (date != null && date.getYear() > 2015) invalidDate = true;
							}
							if (!invalidDate) couldBeOld = !inNewChunk(chunk, mc, dimension);
						}
					}
				}
			}

			if (!couldBeOld) return;
			signs = getNearbySigns(chunk);
		}

	}
	@Nullable
	private LocalDate parseDate(String dateStr) {
		String[] formats = {
				"M/d/yy", "M/dd/yy", "MM/d/yy", "MM/dd/yy",
				"M/d/yyyy", "M/dd/yyyy", "MM/d/yyyy", "MM/dd/yyyy",
				"d/M/yy", "d/MM/yy", "dd/M/yy", "dd/MM/yy", "d/M/yyyy",
				"d/MM/yyyy", "dd/M/yyyy", "dd/MM/yyyy", "yyyy/M/d", "yyyy/MM/d",
				"yyyy/M/dd", "yyyy/MM/dd", "yyyy/d/M", "yyyy/dd/M", "yyyy/d/MM", "yyyy/dd/MM",
		};
		for (String format : formats) {
			LocalDate date;
			try {
				date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
			} catch (Exception ignored) {
				continue;
			}
			return date;
		}
		return null;
	}
	private boolean inNewChunk(LevelChunk chunk, Minecraft mc, ResourceKey<Level> dimension) {
		if (mc.level == null) return false;
		ChunkPos chunkPos = chunk.getPos();
		if (chunkCache.containsKey(chunkPos)) {
			return chunkCache.get(chunkPos);
		}

		if (dimension == Level.NETHER) {
			BlockPos startPosDebris = chunkPos.getBlockAt(0, 0, 0);
			BlockPos endPosDebris = chunkPos.getBlockAt(15, 118, 15);

			int newBlocks = 0;
			for (BlockPos pos : BlockPos.betweenClosed(startPosDebris, endPosDebris)) {
				if (newBlocks >= 13) {
					chunkCache.put(chunkPos, true);
					return true;
				}
				Block block = mc.level.getBlockState(pos).getBlock();
				if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.BLACKSTONE || block == Blocks.BASALT
						|| block == Blocks.WARPED_NYLIUM || block == Blocks.CRIMSON_NYLIUM || block == Blocks.SOUL_SOIL) ++newBlocks;
			}
			chunkCache.put(chunkPos, (newBlocks >= 13));
			return newBlocks >= 13;
		} else if (dimension == Level.OVERWORLD){
			BlockPos startPosAltStones = chunkPos.getBlockAt(0, 0, 0);
			BlockPos endPosAltStones = chunkPos.getBlockAt(15, 128, 15);

			int newBlocks = 0;
			for (BlockPos pos : BlockPos.betweenClosed(startPosAltStones, endPosAltStones)) {
				if (newBlocks >=  33) {
					chunkCache.put(chunkPos, true);
					return true;
				}
				Block block = mc.level.getBlockState(pos).getBlock();
				if (block == Blocks.ANDESITE || block == Blocks.GRANITE || block == Blocks.DIORITE) ++newBlocks;
			}
			chunkCache.put(chunkPos, (newBlocks >= 33));
			return newBlocks >= 33;
		} else if (dimension == Level.END) {
			ResourceKey<Biome> biome = mc.level
					.getBiome(new BlockPos(chunkPos.getMiddleBlockX(), 64, chunkPos.getMiddleBlockZ()))
					.unwrapKey().orElseGet(() -> Biomes.GROVE);
			boolean bl = !(biome == Biomes.THE_END || biome == Biomes.PLAINS);
			chunkCache.put(chunkPos, bl);
			return bl;
		}

		chunkCache.put(chunkPos, true);
		return true;
	}

}
