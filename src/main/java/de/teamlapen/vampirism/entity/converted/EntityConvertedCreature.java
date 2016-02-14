package de.teamlapen.vampirism.entity.converted;

import de.teamlapen.lib.lib.network.ISyncable;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.api.entity.convertible.BiteableRegistry;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.convertible.IConvertingHandler;
import de.teamlapen.vampirism.api.entity.factions.Faction;
import de.teamlapen.vampirism.entity.EntityVampireBase;
import de.teamlapen.vampirism.entity.EntityVampirism;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 *  Converted creature class.
 * Contains (stores and syncs) a normal Entity for rendering purpose
 */
public class EntityConvertedCreature<T extends EntityCreature> extends EntityVampireBase implements IConvertedCreature<T> ,ISyncable{
    private T entityCreature;
    private boolean entityChanged = false;

    private final static String TAG="ConvCreature";
    public EntityConvertedCreature(World world) {
        super(world);
        //TODO make something that applies to all IHunter this.tasks.addTask(1, new EntityAIAvoidEntity(this, EntityHunterBase.class, BALANCE.MOBPROP.VAMPIRE_DISTANCE_HUNTER, 1.0, 1.05));

        //this.tasks.addTask(3, new VampireAIFleeSun(this, 1F));
        this.tasks.addTask(4, new EntityAIRestrictSun(this));
        tasks.addTask(5, new net.minecraft.entity.ai.EntityAIAttackOnCollide(this, EntityPlayer.class, 0.9D, false));
        //TODO make something that applies to all IHunter tasks.addTask(5, new net.minecraft.entity.ai.EntityAIAttackOnCollide(this, EntityHunterBase.class, 1.0D, true));


        this.tasks.addTask(11, new EntityAIWander(this, 0.7));
        this.tasks.addTask(13, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(15, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
    }

    @Override
    public void setEntityCreature(T creature) {
        if ((creature == null && entityCreature != null)) {
            entityChanged = true;
            entityCreature = null;
        } else if (creature != null) {
            if (!creature.equals(entityCreature)) {
                entityCreature = creature;
                entityChanged = true;
                this.setSize(creature.width, creature.height);
            }
        }
        if (entityCreature != null && getConvertedHelper() == null) {
            entityCreature = null;
            VampirismMod.log.w(TAG, "Cannot find converting handler for converted creature %s (%s)", this, entityCreature);
        }
    }


    public T getOldCreature(){
        return entityCreature;
    }
    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
            writeOldEntityToNBT(nbt);

    }

    /**
     * Write the old entity to nbt
     * @param nbt
     */
    private void writeOldEntityToNBT(NBTTagCompound nbt){
        if(!nil()){
            NBTTagCompound entity = new NBTTagCompound();
            entityCreature.isDead = false;
            entityCreature.writeToNBTOptional(entity);
            entityCreature.isDead = true;
            nbt.setTag("entity_old", entity);
        }

    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote && entityCreature == null) {
            VampirismMod.log.t("Setting dead");
            this.setDead();
        }
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        if (nbt.hasKey("entity_old")) {
            setEntityCreature((T) EntityList.createEntityFromNBT(nbt.getCompoundTag("entity_old"), worldObj));
            if(nil()){
                VampirismMod.log.w(TAG,"Failed to create old entity %s. Maybe the entity does not exist anymore",nbt.getCompoundTag("entity_old"));
            }
        } else {
            VampirismMod.log.w(TAG,"Saved entity did not have a old entity");
        }
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (!nil()) {
            entityCreature.copyLocationAndAnglesFrom(this);
            entityCreature.prevPosZ = this.prevPosZ;
            entityCreature.prevPosY = this.prevPosY;
            entityCreature.prevPosX = this.prevPosX;
            entityCreature.rotationYawHead = this.rotationYawHead;
            entityCreature.prevRotationPitch = this.prevRotationPitch;
            entityCreature.prevRotationYaw = this.prevRotationYaw;
            entityCreature.prevRotationYawHead = this.prevRotationYawHead;
            entityCreature.motionX = this.motionX;
            entityCreature.motionY = this.motionY;
            entityCreature.motionZ = this.motionZ;
            entityCreature.lastTickPosX = this.lastTickPosX;
            entityCreature.lastTickPosY = this.lastTickPosY;
            entityCreature.lastTickPosZ = this.lastTickPosZ;
            entityCreature.hurtTime = this.hurtTime;
            entityCreature.maxHurtTime = this.maxHurtTime;
            entityCreature.attackedAtYaw = this.attackedAtYaw;
            entityCreature.swingProgress = this.swingProgress;
            entityCreature.prevSwingProgress = this.prevSwingProgress;
            entityCreature.prevLimbSwingAmount = this.prevLimbSwingAmount;
            entityCreature.limbSwingAmount = this.limbSwingAmount;
            entityCreature.limbSwing = this.limbSwing;
            entityCreature.renderYawOffset = this.renderYawOffset;
            entityCreature.prevRenderYawOffset = this.prevRenderYawOffset;
            entityCreature.deathTime = this.deathTime;

            if (worldObj.isRemote) {
                entityCreature.serverPosX = this.serverPosX;
                entityCreature.serverPosY = this.serverPosY;
                entityCreature.serverPosZ = this.serverPosZ;

            }
        }
        if (entityChanged) {
            this.updateEntityAttributes();
        }
    }

    @Override
    public void loadUpdateFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("entity_old")) {
            setEntityCreature((T) EntityList.createEntityFromNBT(nbt.getCompoundTag("entity_old"), worldObj));
        }
    }


    @Override
    protected boolean canDespawn() {
        return true;//TODO maybe change to false
    }


    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.updateEntityAttributes();
    }

    @Override
    public void writeFullUpdateToNBT(NBTTagCompound nbt) {
        writeOldEntityToNBT(nbt);

    }
    protected void updateEntityAttributes() {
        if (!nil()) {
            IConvertingHandler.IDefaultHelper helper = getConvertedHelper();
            this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(helper.getConvertedDMG(entityCreature));
            this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(helper.getConvertedMaxHealth(entityCreature));
            this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(helper.getConvertedKnockbackResistance(entityCreature));
            this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(helper.getConvertedSpeed(entityCreature));
        } else {
            this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1000);
            this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(0);
            this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0);
        }

    }

    @Override
    public String getName() {
        return StatCollector.translateToLocal("entity.vampirism.vampire.name") + " " + (nil() ? super.getName() : entityCreature.getName());
    }



    @Override
    protected void dropFewItems(boolean p_70628_1_, int p_70628_2_) {
        getConvertedHelper().dropConvertedItems(entityCreature,p_70628_1_,p_70628_2_);
    }

    @Override
    public String toString() {
        return "[" + super.toString() + " representing " + entityCreature + "]";
    }

    @Override
    public void playLivingSound() {
        if (!nil()) {
            entityCreature.playLivingSound();
        }
    }

    /**
     * @return The {@link de.teamlapen.vampirism.api.entity.convertible.IConvertingHandler.IDefaultHelper} for this creature
     */
    protected IConvertingHandler.IDefaultHelper getConvertedHelper(){
        IConvertingHandler handler=BiteableRegistry.getEntry(entityCreature).convertingHandler;
        if(handler instanceof DefaultConvertingHandler){
            return ((DefaultConvertingHandler) handler).getHelper();
        }
        return null;
    }

    protected boolean nil() {
        return entityCreature == null;
    }
}