package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.CriptografiaUtil;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringCriptografadoConverter
        implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String valor) {
        return CriptografiaUtil.criptografar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valorBanco) {
        return CriptografiaUtil.descriptografar(valorBanco);
    }
}