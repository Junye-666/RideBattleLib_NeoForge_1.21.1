package com.jpigeon.ridebattlelib.common.data;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class RiderAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RideBattleLib.MODID);

    public static final Supplier<AttachmentType<@NotNull RiderData>> RIDER_DATA =
            ATTACHMENTS.register(
                    "rider_data",
                    () -> AttachmentType.builder(() -> new RiderData())
                            .serialize(RiderData.CODEC)
                            .copyOnDeath()
                            .build()
            );
}
