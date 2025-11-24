package br.edu.idp.mcdia.dl.judscraper.model;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import java.util.ArrayList;
import java.util.List;

class AssuntoListDeserializer extends ValueDeserializer<List<Assunto>> {

    @Override
    public List<Assunto> deserialize(JsonParser p, DeserializationContext ctxt) {
        try {
            JsonNode node = ctxt.readTree(p);
            List<Assunto> assuntos = new ArrayList<>();
            processNode(node, assuntos, ctxt);
            return assuntos;
        } catch (JacksonException e) {
            throw new RuntimeException("Falha ao desserializar assuntos.", e);
        }
    }

    private void processNode(JsonNode node, List<Assunto> assuntos, DeserializationContext ctxt) throws JacksonException {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            assuntos.add(ctxt.readTreeAsValue(node, Assunto.class));
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                processNode(child, assuntos, ctxt);
            }
        }
    }
}
