package com.ryzer.ryzergen.registry;

import com.mojang.serialization.Codec;
import com.ryzer.ryzergen.RyzerGen;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RyzerGen.MOD_ID);

    /** A player's absorbed radiation dose in mSv. Saved with the player; not kept through death. */
    public static final Supplier<AttachmentType<Float>> RADIATION_DOSE = ATTACHMENTS.register("radiation_dose",
            () -> AttachmentType.builder(() -> 0F).serialize(Codec.FLOAT).build());

    private ModAttachments() {}
}
