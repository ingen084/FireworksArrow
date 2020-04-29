package net.ingen084.fireworksarrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class FireworkMetaSerializer {
    public static byte[] serialize(FireworkMeta src) {
        try {
            var outputStream = new ByteArrayOutputStream();
            var dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(src);
            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("ItemStack serialize error.", e);
        }
    }
    public static FireworkMeta deserialize(byte[] source) {
        try {
            var inputStream = new ByteArrayInputStream(source);
            var dataInput = new BukkitObjectInputStream(inputStream);

            var result = (FireworkMeta)dataInput.readObject();
            dataInput.close();
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("ItemStack deserialize error.", e);
        }
    }
}