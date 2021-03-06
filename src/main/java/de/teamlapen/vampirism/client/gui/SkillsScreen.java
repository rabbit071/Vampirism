package de.teamlapen.vampirism.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.client.core.ModKeys;
import de.teamlapen.vampirism.core.ModBlocks;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.network.InputEventPacket;
import de.teamlapen.vampirism.player.skills.ActionSkill;
import de.teamlapen.vampirism.player.skills.SkillHandler;
import de.teamlapen.vampirism.player.skills.SkillNode;
import de.teamlapen.vampirism.util.Helper;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Gui screen which displays the skills available to the players and allows him to unlock some.
 * Inspired by Minecraft's old GuiAchievement
 */
@OnlyIn(Dist.CLIENT)
public class SkillsScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(REFERENCE.MODID, "textures/gui/skills_window.png");
    private final int area_min_y = -77;
    private final int skill_width = 24;
    private final List<SkillNode> skillNodes = new ArrayList<>();
    private final int display_width = 256;
    private final int display_height = 202;
    private int area_min_x = 0;
    private int area_max_x = 0;
    private int area_max_y;
    private float zoomOut = 1.0F;
    private double displayX;
    private double displayY;
    private double displayXNew;
    private double displayYNew;
    private SkillHandler<?> skillHandler;
    private boolean display;
    private ISkill selected;
    private SkillNode selectedNode;
    private int displayXWidth;
    private int displayYHeight;
    @Nullable
    private ITextComponent lordTitle;
    private int lordLevel;

    public SkillsScreen() {
        super(new TranslationTextComponent("screen.vampirism.skills"));
        this.width = display_width;
        this.height = display_height;
    }

    @Override
    public void init() {
        this.addButton(new Button(this.width / 2 + 24, this.height / 2 + 74, 80, 20, UtilLib.translate("gui.done"), (context) -> {
            this.minecraft.displayGuiScreen(null);
        }));
        FactionPlayerHandler.getOpt(minecraft.player).ifPresent(fph -> {
            lordTitle = fph.getLordTitle();
            lordLevel = fph.getLordLevel();
            fph.getCurrentFactionPlayer().ifPresent(factionPlayer -> {
                IPlayableFaction<?> faction = factionPlayer.getFaction();
                display = true;
                skillHandler = (SkillHandler<?>) factionPlayer.getSkillHandler();
                Integer[] info = VampirismMod.proxy.getSkillTree(true).getDisplayInfo(faction.getID());
                int w = info[0] * info[1] * skill_width * 2;
                area_max_x = w + 10 - display_width;
                area_min_x = -w - 10 - display_width;
                area_max_y = info[2] * skill_width * 2;
                this.displayX = displayXNew = -100;
                this.displayY = displayYNew = -10;
                skillNodes.clear();
                SkillNode root = VampirismMod.proxy.getSkillTree(true).getRootNodeForFaction(faction.getID());
                addToList(skillNodes, root);

                Button resetSkills = this.addButton(new Button((this.width - display_width) / 2 + 24 + 40, this.height / 2 + 74, 80, 20, UtilLib.translate("text.vampirism.skill.resetall"), (context) -> {
                    boolean test = VampirismMod.inDev || VampirismMod.instance.getVersionInfo().getCurrentVersion().isTestVersion();
                    ConfirmScreen resetGui = new ConfirmScreen((cxt) -> {
                        if (cxt) {
                            VampirismMod.dispatcher.sendToServer(new InputEventPacket(InputEventPacket.RESETSKILL, ""));
                            Minecraft.getInstance().displayGuiScreen(this);
                        } else {
                            Minecraft.getInstance().displayGuiScreen(this);
                        }
                    }, new TranslationTextComponent("gui.vampirism.reset_skills.title"), new TranslationTextComponent("gui.vampirism.reset_skills." + (test ? "desc_test" : "desc")));
                    Minecraft.getInstance().displayGuiScreen(resetGui);
                }));
                if(factionPlayer.getLevel() < 2) {
                    resetSkills.active = false;
                }
                if (Helper.isVampire(minecraft.player)) {
                    this.addButton(new ImageButton((this.width - display_width) / 2 + 10 + 22, this.height / 2 + 74, 20, 20, 72, 202, 20, BACKGROUND, 256, 256, (context) -> {
                        Minecraft.getInstance().displayGuiScreen(new VampirePlayerAppearanceScreen(this));
                    }));
                }
                this.addButton(new ImageButton((this.width - display_width) / 2 + 10, this.height / 2 + 74, 20, 20, 52, 202, 20, BACKGROUND, 256, 256, (context) -> {
                    IPlayableFaction<?> factionNew = FactionPlayerHandler.get(Minecraft.getInstance().player).getCurrentFaction();
                    Minecraft.getInstance().displayGuiScreen(new SelectActionScreen(factionNew.getColor(), true));
                }));
            });



        });

        this.displayXWidth = this.skillNodes.stream().flatMap(node -> Arrays.stream(node.getElements())).mapToInt(ISkill::getRenderColumn).max().orElse(0) * 25;
        this.displayYHeight = this.skillNodes.stream().flatMap(node -> Arrays.stream(node.getElements())).mapToInt(ISkill::getRenderRow).max().orElse(0) * 20;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        if (p_keyPressed_1_ == 256 || ModKeys.getKeyBinding(ModKeys.KEY.SKILL).getKey().getKeyCode() == p_keyPressed_1_) {
            this.minecraft.displayGuiScreen(null);
            this.minecraft.setGameFocused(true);
            return true;
        } else {
            super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean retur = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && selected != null) {
            if (skillHandler.canSkillBeEnabled(selected) == ISkillHandler.Result.OK) {
                VampirismMod.dispatcher.sendToServer(new InputEventPacket(InputEventPacket.UNLOCKSKILL, selected.getRegistryName().toString()));
                playSoundEffect(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7F);
                return true;
            } else {
                playSoundEffect(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.5F);
                return true;
            }
        }
        return retur;
    }

    @Override
    public boolean mouseDragged(double p_mouseDragged_1_, double p_mouseDragged_3_, int p_mouseDragged_5_, double p_mouseDragged_6_, double p_mouseDragged_8_) {
        displayY -= p_mouseDragged_8_;
        displayX -= p_mouseDragged_6_;
        checkDisplay();
        return true;
    }

    @Override
    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        zoomOut += p_mouseScrolled_5_ > 0 ? -0.25 : 0.25;
        zoomOut = MathHelper.clamp(this.zoomOut, 1.0F, 2.0F);
        checkDisplay();
        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if (!display) {
            super.render(mouseX, mouseY, partialTicks);
            return;
        }

        this.renderBackground();


        this.drawSkills(mouseX, mouseY, partialTicks);
        RenderSystem.disableLighting();
        RenderSystem.disableDepthTest();
        this.drawTitle();
        RenderSystem.enableLighting();
        RenderSystem.enableDepthTest();
    }

    @Override
    public void tick() {
        if (!this.minecraft.player.isAlive()) {
            this.minecraft.player.closeScreen();
        }
    }

    protected void drawTitle() {
        String title;
        if (lordTitle != null) {
            title = lordTitle.getFormattedText() + " (" + lordLevel + ")";
        } else {
            title = I18n.format("text.vampirism.skills.gui_title");
        }
        int x = (this.width - display_width) / 2;
        int y = (this.height - display_height) / 2;
        this.font.drawString(title, x + 15, y + 5, 0xFFFFFFFF);
        String points = I18n.format("text.vampirism.skills.points_left", skillHandler.getLeftSkillPoints());
        x = (this.width + display_width) / 2 - font.getStringWidth(points);
        this.font.drawString(points, x - 15, y + 5, 0xFFFFFFFF);
    }

    /**
     * Add the given node and all it's child nodes to the list
     *
     * @param list
     * @param root
     */
    private void addToList(List<SkillNode> list, SkillNode root) {
        list.add(root);
        for (SkillNode node : root.getChildren()) {
            addToList(list, node);
        }

    }

    private void checkDisplay() {
        displayY = MathHelper.clamp(displayY, -20 / zoomOut, (this.displayYHeight - 20)  / zoomOut);
        displayX = MathHelper.clamp(displayX, (-400 - displayXWidth) / zoomOut + (zoomOut - 2.0F) * (-1) * 250, (-400 + displayXWidth) / zoomOut + (zoomOut - 2.0F) * (-1) * 250);
        displayXNew = displayX;
        displayYNew = displayY;
    }

    private void drawSkills(int mouseX, int mouseY, float partialTicks) {
        int offsetX = MathHelper.floor(this.displayX + (this.displayXNew - this.displayX) * (double) partialTicks);
        int offsetY = MathHelper.floor(this.displayY + (this.displayYNew - this.displayY) * (double) partialTicks);

        if (offsetX < area_min_x) {
            offsetX = area_min_x;
        }

        if (offsetY < area_min_y) {
            offsetY = area_min_y;
        }

        if (offsetX >= area_max_x) {
            offsetX = area_max_x - 1;
        }

        if (offsetY >= area_max_y) {
            offsetY = area_max_y - 1;
        }
        int k = (this.width - this.display_width) / 2;
        int l = (this.height - this.display_height) / 2;
        int i1 = k + 16;
        int j1 = l + 17;

        //Limit render area
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        GL11.glScissor((int) (k * scale), (int) (l * scale),
                (int) (display_width * scale), (int) (display_height * scale));

        this.setBlitOffset(0);
        RenderSystem.depthFunc(518);
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) i1, (float) j1, -200.0F);
        RenderSystem.scalef(1.0F / this.zoomOut, 1.0F / this.zoomOut, 1.0F);
        RenderSystem.enableTexture();
        RenderSystem.disableLighting();
        RenderSystem.enableRescaleNormal();
        RenderSystem.enableColorMaterial();
        int k1 = offsetX + 288 >> 4;
        int l1 = offsetY + 288 >> 4;
        int i2 = (offsetX + display_width * 2) % 16;
        int j2 = (offsetY + 288) % 16;
        Random random = new Random();
        float f = 16.0F / this.zoomOut;
        float f1 = 16.0F / this.zoomOut;

        //Render background block textures
        for (int y = 0; (float) y * f - (float) j2 < 155.0F; ++y) {
            float f2 = 0.6F - (float) (l1 + y) / 25.0F * 0.3F;
            RenderSystem.color4f(f2, f2, f2, 1.0F);

            for (int x = 0; (float) x * f1 - (float) i2 < 224.0F; ++x) {
                random.setSeed(this.minecraft.getSession().getPlayerID().hashCode() + k1 + x + (l1 + y) * 16);
                int j4 = random.nextInt(1 + l1 + y) + (l1 + y) / 2;
                TextureAtlasSprite textureatlassprite = this.getTexture(Blocks.SAND);

                if (j4 <= 37 && l1 + y != 35) {
                    if (j4 == 22) {
                        if (random.nextInt(2) == 0) {
                            textureatlassprite = this.getTexture(Blocks.COAL_BLOCK);
                        } else {
                            textureatlassprite = this.getTexture(Blocks.REDSTONE_BLOCK);
                        }
                    } else if (j4 == 10) {
                        textureatlassprite = this.getTexture(ModBlocks.castle_block_dark_brick_bloody);
                    } else if (j4 == 8) {
                        textureatlassprite = this.getTexture(Blocks.STONE_BRICKS);
                    } else if (j4 > 4) {
                        textureatlassprite = this.getTexture(ModBlocks.castle_block_normal_brick);
                    } else if (j4 > 0) {
                        textureatlassprite = this.getTexture(Blocks.DIRT);

                    }
                } else {
                    Block block = Blocks.BEDROCK;
                    textureatlassprite = this.getTexture(block);
                }

                this.minecraft.getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
                blit(x * 16 - i2, y * 16 - j2, this.getBlitOffset(), 16, 16, textureatlassprite);
            }
        }

        //Draw lines/arrows
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        this.minecraft.getTextureManager().bindTexture(BACKGROUND);

        for (SkillNode node : skillNodes) {
            if (node.getParent() != null) {
                int xs = findHorizontalNodeCenter(node) - offsetX + 11;
                int ys = node.getElements()[0].getRenderRow() * skill_width - offsetY + 11;

                int xp = findHorizontalNodeCenter(node.getParent()) - offsetX + 11;
                int yp = node.getParent().getElements()[0].getRenderRow() * skill_width - offsetY + 11;

                int color = 0xff000000;
                if (skillHandler.isNodeEnabled(node)) {
                    color = 0xffa0a0a0;
                } else if (skillHandler.isSkillNodeLocked(node)) {
                    color = 0xff990000;
                } else if (skillHandler.isNodeEnabled(node.getParent())) {
                    color = 0xff009900;
                }
                this.hLine(xs, xp, yp, color);
                this.vLine(xs, ys - 11, yp, color);
                if (ys > yp) {
                    //Currently always like this. The other option are here in case this changes at some point
                    this.blit(xs - 5, ys - 11 - 7, 96, 234, 11, 7);
                } else if (ys < yp) {
                    this.blit(xs - 5, ys + 11, 96, 241, 11, 7);
                } else if (xs > xp) {
                    this.blit(xs - 11 - 7, ys - 5, 114, 234, 7, 11);
                } else if (xs < xp) {
                    this.blit(xs + 11, ys - 5, 107, 234, 7, 11);
                }
            }
        }

        float mMouseX = (float) (mouseX - i1) * this.zoomOut;
        float mMouseY = (float) (mouseY - j1) * this.zoomOut;
        RenderHelper.enableStandardItemLighting(); //enableStandardGUIItemLighting
        RenderSystem.disableLighting();
        RenderSystem.enableRescaleNormal();
        RenderSystem.enableColorMaterial();

        //Draw skills
        ISkill newSelected = null;//Not sure if mouse clicks can occur while this is running, so don't set #selected to null here but use a extra variable to be sure
        SkillNode newSelectedNode = null;
        for (SkillNode node : skillNodes) {
            ISkill[] elements = node.getElements();
            if (elements.length > 1) {
                int minX = elements[0].getRenderColumn() * skill_width - offsetX;
                int maxX = elements[elements.length - 1].getRenderColumn() * skill_width - offsetX;
                int y = elements[0].getRenderRow() * skill_width - offsetY;
                if (maxX >= -skill_width && y >= -skill_width && (float) minX <= 224.0F * this.zoomOut && (float) y <= 155.0F * this.zoomOut) {
                    RenderSystem.enableBlend();
                    this.fillGradient(minX - 1, y - 1, maxX + 23, y + 23, 0xFF9B9DA1, 0xFF9B9DA1);
                    RenderSystem.disableBlend();
                }

            }
            for (int i = 0; i < elements.length; i++) {
                ISkill skill = elements[i];
                int x = skill.getRenderColumn() * skill_width - offsetX;
                int y = skill.getRenderRow() * skill_width - offsetY;

                if (x >= -skill_width && y >= -skill_width && (float) x <= 224.0F * this.zoomOut && (float) y <= 155.0F * this.zoomOut) {
                    int unlockstate = skillHandler.isSkillEnabled(skill) ? 0 : skillHandler.isNodeEnabled(node) ? -1 : skillHandler.canSkillBeEnabled(skill) == ISkillHandler.Result.OK ? 1 : 2;

                    if (unlockstate == 0) {
                        float f5 = 1F;
                        RenderSystem.color4f(f5, f5, f5, 1.0F);
                    } else if (unlockstate == 1) {
                        float f6 = 0.6F;
                        RenderSystem.color4f(f6, f6, f6, 1.0F);
                    } else if (unlockstate == 2) {
                        float f7 = 0.3F;
                        RenderSystem.color4f(f7, f7, f7, 1.0F);
                    } else if (unlockstate == -1) {
                        float f8 = 0.2F;
                        RenderSystem.color4f(f8, f8, f8, 1.0F);
                    }

                    this.minecraft.getTextureManager().bindTexture(BACKGROUND);

                    RenderSystem.enableBlend();
                    this.blit(x - 2, y - 2, node.getLockingNodes().length == 0 ? 0 : 26, 202, 26, 26);
                    RenderSystem.disableBlend();

                    this.minecraft.getTextureManager().bindTexture(getIconLoc(skill));

                    RenderSystem.disableLighting();
                    //GlStateManager.enableCull();
                    RenderSystem.enableBlend();
                    UtilLib.drawTexturedModalRect(this.getBlitOffset(), x + 3, y + 3, 0, 0, 16, 16, 16, 16);
                    //GlStateManager.blendFunc(770, 771);
                    RenderSystem.disableLighting();


                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

                    if (mMouseX >= (float) x && mMouseX <= (float) (x + 22) && mMouseY >= (float) y && mMouseY <= (float) (y + 22)) {
                        newSelected = skill;
                        newSelectedNode = node;
                    }

                    if (i + 1 < elements.length) {
                        this.drawCenteredString(font, "OR", x + skill_width + skill_width / 2, y + 1 + (skill_width - font.FONT_HEIGHT) / 2, 0xFFFFFF);
                    }
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.popMatrix();

        //Draw "window" and buttons
        Color color = skillHandler.getPlayer().getFaction().getColor();
        RenderSystem.color4f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(BACKGROUND);
        this.blit(k, l, 0, 0, this.display_width, this.display_height);
        this.setBlitOffset(0);
        RenderSystem.depthFunc(515);
        RenderSystem.disableDepthTest();
        RenderSystem.enableTexture();
        super.render(mouseX, mouseY, partialTicks);

        //Draw information for selected skill
        selected = newSelected;
        selectedNode = newSelectedNode;
        if (selected != null) {
            RenderSystem.pushMatrix();
            RenderSystem.translated(0, 0, 1); //Render tooltips in front of buttons
            int m2MouseX = mouseX + 12;
            int m2MouseY = mouseY - 4;

            String name = I18n.format(selected.getTranslationKey());
            ITextComponent desc = selected.getDescription();
            ISkillHandler.Result result = skillHandler.canSkillBeEnabled(selected);

            int width_name = Math.max(this.font.getStringWidth(name), 110);
            int height_desc = desc == null ? 0 : font.getWordWrappedHeight(desc.getFormattedText(), width_name);

            List<ISkill> lockingSkills = null;
            int lockingColor = 0xFF000000;
            if (selectedNode.getLockingNodes().length != 0) {
                lockingSkills = skillHandler.getLockingSkills(selectedNode);
                height_desc += 12 * (lockingSkills.size() + 1);
                lockingColor = result == ISkillHandler.Result.ALREADY_ENABLED ? 0xff808080 : lockingSkills.stream().anyMatch(skill -> skillHandler.isSkillEnabled(skill)) ? 0xFFA32228 : 0xFFFBAE00;
            }
            int locking_desc = height_desc;
            if (result == ISkillHandler.Result.ALREADY_ENABLED || result == ISkillHandler.Result.PARENT_NOT_ENABLED || result == ISkillHandler.Result.LOCKED_BY_OTHER_NODE) {
                height_desc += 12;
            }
            this.fillGradient(m2MouseX - 3, m2MouseY - 3, m2MouseX + width_name + 3, m2MouseY + height_desc + 3 + 12, -1073741824, -1073741824);

            this.font.drawStringWithShadow(name, (float) m2MouseX, (float) m2MouseY, 0xff808080);
            if (desc != null)
                this.font.drawSplitString(desc.getFormattedText(), m2MouseX, m2MouseY + 12, width_name, 0xff505050);
            if (lockingSkills != null) {
                this.font.drawString(I18n.format("text.vampirism.skill.excluding"), m2MouseX, m2MouseY + locking_desc - 12 * lockingSkills.size() + 3, lockingColor);
                for (int i = 0; i < lockingSkills.size(); i++) {
                    this.font.drawString("  " + I18n.format(lockingSkills.get(i).getTranslationKey()), m2MouseX, m2MouseY + locking_desc - i * 12 + 3, lockingColor);
                }
            }
            if (result == ISkillHandler.Result.ALREADY_ENABLED) {
                this.font.drawStringWithShadow(I18n.format("text.vampirism.skill.unlocked"), m2MouseX, m2MouseY + height_desc + 3, 0xFFFBAE00);
            } else if (result == ISkillHandler.Result.PARENT_NOT_ENABLED) {
                this.font.drawStringWithShadow(I18n.format("text.vampirism.skill.unlock_parent_first"), m2MouseX, m2MouseY + height_desc + 3, 0xFFA32228);
            } else if (result == ISkillHandler.Result.LOCKED_BY_OTHER_NODE) {
                this.font.drawStringWithShadow(I18n.format("text.vampirism.skill.locked"), m2MouseX, m2MouseY + height_desc + 3, 0xFFA32228);
            }
            RenderSystem.popMatrix();
        }


        RenderSystem.enableDepthTest();
        RenderSystem.enableLighting();
        RenderHelper.disableStandardItemLighting();

    }

    private int findHorizontalNodeCenter(SkillNode node) {
        int width = (node.getElements().length - 1) * 2 * skill_width;
        return node.getElements()[0].getRenderColumn() * skill_width + width / 2;
    }

    private ResourceLocation getIconLoc(ISkill skill) {
        if (skill instanceof ActionSkill) {
            return new ResourceLocation(((ActionSkill) skill).getActionID().getNamespace(), "textures/actions/" + ((ActionSkill) skill).getActionID().getPath() + ".png");
        } else {
            return new ResourceLocation(skill.getRegistryName().getNamespace(), "textures/skills/" + skill.getRegistryName().getPath() + ".png");
        }
    }


    private TextureAtlasSprite getTexture(BlockState blockstate) {
        return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getTexture(blockstate);
    }

    private TextureAtlasSprite getTexture(Block block) {
        return getTexture(block.getDefaultState());
    }

    private void playSoundEffect(SoundEvent event, float pitch) {
        minecraft.getSoundHandler().play(SimpleSound.master(event, 1.0F));
    }

}
