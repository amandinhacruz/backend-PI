package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.CriptografiaUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Converter
public class ArquivoCriptografadoConverter implements AttributeConverter<byte[], byte[]> {
    public byte[] convertToDatabaseColumn(byte[] value) {
        if (value == null) return null;
        return CriptografiaUtil.criptografar(Base64.getEncoder().encodeToString(value)).getBytes(StandardCharsets.UTF_8);
    }
    public byte[] convertToEntityAttribute(byte[] value) {
        if (value == null) return null;
        return Base64.getDecoder().decode(CriptografiaUtil.descriptografar(new String(value, StandardCharsets.UTF_8)));
    }
}
