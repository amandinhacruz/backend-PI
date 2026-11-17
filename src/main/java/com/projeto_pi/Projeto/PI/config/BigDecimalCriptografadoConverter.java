package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.CriptografiaUtil;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class BigDecimalCriptografadoConverter
        implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal valor) {

        if (valor == null) {
            return null;
        }

        return CriptografiaUtil.criptografar(
                valor.toPlainString()
        );
    }

    @Override
    public BigDecimal convertToEntityAttribute(String valorBanco) {

        if (valorBanco == null) {
            return null;
        }

        return new BigDecimal(
                CriptografiaUtil.descriptografar(valorBanco)
        );
    }
}
