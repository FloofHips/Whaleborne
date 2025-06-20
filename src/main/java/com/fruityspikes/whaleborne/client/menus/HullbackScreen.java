package com.fruityspikes.whaleborne.client.menus;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;

public class HullbackScreen extends AbstractContainerScreen<HullbackMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/gui/hullback.png");
    private static final ResourceLocation STEEN = new ResourceLocation(Whaleborne.MODID, "textures/gui/steen.png");
    private static final ResourceLocation MOBIUS = new ResourceLocation(Whaleborne.MODID, "textures/gui/mobius.png");
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private float xMouse;
    private float yMouse;
    public HullbackScreen(HullbackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int eyeOffset = Mth.clamp((mouseX + (imageWidth / 2) - this.width / 2) / 10, 0, 17);

        guiGraphics.blit(getTextureLocation(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        guiGraphics.blit(getTextureLocation(), leftPos + 102 + eyeOffset, topPos + 46, imageWidth, 17, 17, 17);
        renderMultiplicativeQuad(guiGraphics, getTextureLocation(), leftPos + 102, topPos + 46, imageWidth, 0, 34, 17);

        //InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 51, j + 60, 5, (float)(i + 51) - this.xMouse, (float)(j + 75 - 50) - this.yMouse, this.menu.hullback);

        int x = leftPos + 10;
        int y = topPos + 20;

        renderVehicleHealth(guiGraphics);
        float speed = menu.getSpeedModifier();
        guiGraphics.drawString(this.font, String.format("+ " + speed), leftPos + 37, y + 20, 0xFFFFFF, false);
        renderVehicleArmor(guiGraphics);
    }

    private void renderVehicleHealth(GuiGraphics guiGraphics) {
        int x = leftPos + 84;
        int y = topPos + 22;

        if (menu.isVehicleAlive()) {
            int i = 6;
            int j = (int) ((menu.getHealth() / 10) * 12);

            int l = 91;
            int j1 = 0;

            for(boolean flag = false; i > 0; j1 += (int) 6) {
                int k1 = (int) 6;
                i -= k1;

                for(int l1 = 0; l1 < k1; ++l1) {
                    boolean i2 = true;
                    int j2 = 0;
                    int k2 = x - l1 * 8 - 9;
                    guiGraphics.blit(GUI_TEXTURE, k2, y, 52 + j2 * 9, 9, 9, 9);
                    if (l1 * 2 + 1 + j1 < j) {
                        guiGraphics.blit(GUI_TEXTURE, k2, y, 88, 9, 9, 9);
                    }

                    if (l1 * 2 + 1 + j1 == j) {
                        guiGraphics.blit(GUI_TEXTURE, k2, y, 97, 9, 9, 9);
                    }
                }
            }
        }

    }
    private void renderVehicleArmor(GuiGraphics guiGraphics) {
        int x = leftPos + 35;
        int y = topPos + 20 + 37;

        if (menu.isVehicleAlive()) {
            int i = 12;
            if (i != 0) {
                int i3 = (int)Math.ceil(menu.getArmorProgress() * 12);
                //this.minecraft.getProfiler().popPush("mountHealth");
                int k = 39;
                int l = 91;
                int i1 = k;
                int j1 = 0;
                int k2 = 39;
                for(boolean flag = false; i > 0; j1 += 20) {
                    int k1 = i;
                    i -= k1;

                    int l3;
                    for(int k3 = 0; k3 < 6; ++k3) {
                        //if (i3 > 0) {
                            l3 = x + k3 * 8;
                            if (k3 * 2 + 1 < i3) {
                                guiGraphics.blit(GUI_TEXTURE, l3, y, 34, 9, 9, 9);
                            }

                            if (k3 * 2 + 1 == i3) {
                                guiGraphics.blit(GUI_TEXTURE, l3, y, 25, 9, 9, 9);
                            }

                            if (k3 * 2 + 1 > i3) {
                                guiGraphics.blit(GUI_TEXTURE, l3, y, 16, 9, 9, 9);
                            }
                        //}
                    }

                    i1 -= 10;
                }
            }
        }

    }
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.xMouse = (float)mouseX;
        this.yMouse = (float)mouseY;
        renderTooltip(gui, mouseX, mouseY);
    }
    public void renderMultiplicativeQuad(GuiGraphics gui, ResourceLocation texture, int startx, int starty, int Uoffset, int Voffset, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.DST_COLOR,
                GlStateManager.DestFactor.ZERO,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );

        gui.blit(texture, startx, starty, Uoffset, Voffset, width, height);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    public ResourceLocation getTextureLocation() {
        if(menu.getName().equals("Mobius"))
            return MOBIUS;
        if(menu.getName().equals("Steen"))
            return STEEN;
        return TEXTURE;
    }
}
