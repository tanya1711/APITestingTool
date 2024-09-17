package org.example.model.response;

import org.example.model.request.Message;

import java.util.ArrayList;
import java.util.List;


public class BotResponse {

    private List<Choice> choices = new ArrayList<>();
    private Integer created;
    private String id;
    private String model;
    private String object;
    private Object systemFingerprint;
    private Usage usage;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Integer getCreated() {
        return created;
    }

    public void setCreated(Integer created) {
        this.created = created;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Object getSystemFingerprint() {
        return systemFingerprint;
    }

    public void setSystemFingerprint(Object systemFingerprint) {
        this.systemFingerprint = systemFingerprint;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }
}
