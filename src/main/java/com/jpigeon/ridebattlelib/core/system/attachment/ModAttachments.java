package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RideBattleLib.MODID);

    public static final Supplier<AttachmentType<PlayerPersistentData>> PLAYER_DATA =
            ATTACHMENTS.register("player_data",
                    () -> AttachmentType.builder(() -> new PlayerPersistentData(
                            new HashMap<>(),
                                    null,
                                    HenshinState.IDLE,
                                    null))
                            .serialize(PlayerPersistentData.CODEC)
                            .build());
}
