package com.jpigeon.ridebattlelib.common.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class PayloadUtils {
    /**
     * 编解码可为 null 的 ResourceLocation
     * 使用特殊字符串标记 null，避免 Optional 警告
     */
    public static StreamCodec<@NotNull FriendlyByteBuf, @NotNull ResourceLocation> nullableResourceLocation() {
        return StreamCodec.of(
                (buf, loc) -> buf.writeUtf(loc.toString()),
                buf -> {
                    String s = buf.readUtf();
                    if (RiderUtils.NULL_MARKER.equals(s)) {
                        return RiderUtils.NULL;
                    } else {
                        return ResourceLocation.parse(s);
                    }
                }
        );
    }
}
