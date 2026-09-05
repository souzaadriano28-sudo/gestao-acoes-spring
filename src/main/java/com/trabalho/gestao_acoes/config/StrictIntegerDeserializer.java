package com.trabalho.gestao_acoes.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.math.BigInteger;

public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw InvalidFormatException.from(parser, "O valor deve ser um número JSON inteiro.", parser.getText(), Integer.class);
        }
        BigInteger value = parser.getBigIntegerValue();
        try {
            return value.intValueExact();
        } catch (ArithmeticException ex) {
            throw InvalidFormatException.from(parser, "O número inteiro está fora do intervalo suportado.", value, Integer.class);
        }
    }
}
