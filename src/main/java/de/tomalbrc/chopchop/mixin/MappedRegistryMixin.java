package de.tomalbrc.chopchop.mixin;

import de.tomalbrc.chopchop.enchantment.Enchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin<T> {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private void cc$noEnchant(ResourceKey<T> resourceKey, T object, RegistrationInfo registrationInfo, CallbackInfoReturnable<Holder.Reference<T>> cir) {
        //if (resourceKey.equals(Enchantments.TREE_FELLER)) {
        //    cir.setReturnValue(null);
        //}
    }
}
