package com.example.examplemod.effectSet;

import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.print.attribute.Attribute;

public class MobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ExampleMod.MODID);

    public static final RegistryObject<MobEffect> BIND_EFFECT = MOB_EFFECTS.register("bind",
            () -> new BindEffect(MobEffectCategory.HARMFUL, 0x4B5320)
    );


}
