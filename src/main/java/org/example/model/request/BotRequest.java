package org.example.model.request;




import java.util.List;

public class BotRequest {

    private String model;
    private List<Message> messages;
    private int n;
    private double temperature;
    private int max_tokens;
    private String engine;

    public BotRequest(){

    }

    public BotRequest(String model, List<Message> user, int maxCompletions, double temperature, int maxTokens, String engine) {
            this.model = model;
            this.messages = user;
            this.n = maxCompletions;
            this.temperature = temperature;
            this.max_tokens = maxTokens;
            this.engine = engine;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }


}
