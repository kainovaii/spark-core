package fr.kainovaii.obsidian.core.web.component.core;

import java.util.HashMap;
import java.util.Map;

public class ComponentResponse {
    private String html;
    private Map<String, Object> state = new HashMap<>();
    private boolean success = true;
    private String error;
    
    public static ComponentResponse success(String html, Map<String, Object> state) {
        ComponentResponse response = new ComponentResponse();
        response.html = html;
        response.state = state;
        return response;
    }
    
    public static ComponentResponse error(String message) {
        ComponentResponse response = new ComponentResponse();
        response.success = false;
        response.error = message;
        return response;
    }
    
    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
    
    public Map<String, Object> getState() { return state; }
    public void setState(Map<String, Object> state) { this.state = state; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
