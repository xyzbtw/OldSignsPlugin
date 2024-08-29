package dev.xyzbtw;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OldSigns module
 *
 * @author xyzbtw
 */
public class OldSigns extends ToggleableModule {

	public OldSigns() {
		super("OldSigns", ModuleCategory.RENDER);
		this.registerSettings(
				chat,
				render,
				renderColor,
				tracer,
				tracerColor
		);
	}
	private BooleanSetting chat = new BooleanSetting("Chat", true);
	private BooleanSetting render = new BooleanSetting("Box", true);
	private ColorSetting renderColor = new ColorSetting("BoxColor", new Color(255, 166, 0, 100)).setVisibility(() -> render.getValue());

	private BooleanSetting tracer = new BooleanSetting("Tracer", true);
	private ColorSetting tracerColor = new ColorSetting("TracerColor", new Color(255, 166, 0, 100)).setVisibility(() -> tracer.getValue());

	CopyOnWriteArrayList<SignBlockEntity> signs = new CopyOnWriteArrayList<>();

	@Subscribe
	public void onLoadChunk(EventChunk.Load event){
		if (mc.player == null || mc.level == null) return;

		checkOld(WorldUtils.getBlockEntities(false));
	}
	@Subscribe
	public void onUnloadChunk(EventChunk.Unload event) {
		if (mc.player == null || mc.level == null) return;

		ChunkPos unloadedChunkPos = event.getChunkPos();

		signs.removeIf(sign -> {
			ChunkPos signChunkPos = new ChunkPos(sign.getBlockPos());
			return signChunkPos.equals(unloadedChunkPos);
		});
	}
	@Subscribe
	public void onRender3D(EventRender3D event) {
		IRenderer3D renderer = event.getRenderer();
		renderer.begin(event.getMatrixStack());

		if (!signs.isEmpty()) {
			for (SignBlockEntity sign : signs) {
				if (sign == null) continue;
				if (tracer.getValue()) {
					Vec3 center = getScreenCenter(event.getPartialTicks());
					Vec3 offset = getTracerOffset(sign.getBlockState(), sign.getBlockPos());
					renderer.drawLine(center, offset, tracerColor.getValueRGB());
				}
				if (render.getValue())
					renderer.drawBox(sign.getBlockPos(), true, false, renderColor.getValueRGB());
			}
		}

		renderer.end();
	}
	public Vec3 getScreenCenter(float partialTicks) {
		Vector3f pos = new Vector3f(0, 0, 1);

		if (mc.options.bobView().get()) {
			PoseStack bobViewMatrices = new PoseStack();

			bobView(bobViewMatrices, partialTicks);
			pos.mulPosition(bobViewMatrices.last().pose().invert());
		}

		return new Vec3(pos.x, -pos.y, pos.z)
				.xRot(-(float) Math.toRadians(mc.gameRenderer.getMainCamera().getXRot()))
				.yRot(-(float) Math.toRadians(mc.gameRenderer.getMainCamera().getYRot()))
				.add(mc.gameRenderer.getMainCamera().getPosition());
	}
	private void bobView(PoseStack matrices, float partialTicks) {
		Entity cameraEntity = mc.getCameraEntity();

		if (cameraEntity instanceof Player playerEntity) {
			float g = playerEntity.walkDist - playerEntity.walkDistO;
			float h = -(playerEntity.walkDist + g * partialTicks);
			float i = Mth.lerp(partialTicks, playerEntity.oBob, playerEntity.bob);

			matrices.translate(-(Mth.sin(h * 3.1415927f) * i * 0.5), Math.abs(Mth.cos(h * 3.1415927f) * i), 0);
			matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(h * 3.1415927f) * i * 3));
			matrices.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(h * 3.1415927f - 0.2f) * i) * 5));
		}
	}


	@Override
	public void onEnable() {
		super.onEnable();
		if (mc.player == null || mc.level == null) return;
		checkOld(WorldUtils.getBlockEntities(false));
	}
	private Vec3 getTracerOffset(BlockState state, BlockPos pos) {
		double offsetX;
		double offsetY;
		double offsetZ;
		try {
			if (state.getBlock() instanceof WallSignBlock) {
				Direction facing = state.getValue(WallSignBlock.FACING);
				switch (facing) {
					case NORTH -> {
						offsetX = pos.getX() + .5;
						offsetY = pos.getY() + .5;
						offsetZ = pos.getZ() + .937;
					}
					case EAST -> {
						offsetX = pos.getX() + .1337;
						offsetY = pos.getY() + .5;
						offsetZ = pos.getZ() + .5;
					}
					case SOUTH -> {
						offsetX = pos.getX() + .5;
						offsetY = pos.getY() + .5;
						offsetZ = pos.getZ() + .1337;
					}
					case WEST -> {
						offsetX = pos.getX() + .937;
						offsetY = pos.getY() + .5;
						offsetZ = pos.getZ() + .5;
					}
					default -> {
						offsetX = pos.getX() + .5;
						offsetY = pos.getY() + .5;
						offsetZ = pos.getZ() + .5;
					}
				}
			} else return Vec3.atCenterOf(pos);
		} catch (Exception err) {
			return Vec3.atCenterOf(pos);
		}

		return new Vec3(offsetX, offsetY, offsetZ);
	}

	private void checkOld(List<BlockEntity> theseSigns) {
		if (mc.level == null) return;
		for(BlockEntity blockentity : theseSigns) {
			if (!(blockentity instanceof SignBlockEntity thing)) continue;
			if (signs.contains(thing)){
				continue;
			}
			WoodType woodType = WoodType.BAMBOO;
			Block block = blockentity.getBlockState().getBlock();
			if (block instanceof SignBlock signBlock) woodType = SignBlock.getWoodType(signBlock);

			if (woodType == WoodType.OAK) {
				CompoundTag metadata = blockentity.getUpdateTag();
				if (!metadata.toString().contains("{\"extra\":[")) {
					signs.add(thing);
				}
			}
		}

	}

}
