package org.example.model.request;


public class Message {

    private String role;
    private String content;

    public Message(){}


    public Message(String user, String prompt) {
        this.role = user;
        this.content = prompt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String sender) {
        this.role = sender;
    }
}
