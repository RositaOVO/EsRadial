package org.esradial;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("esradial")
public final class EsRadial {
    public EsRadial() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                org.esradial.client.RadialMenuClientApi.initialize());
    }
}
