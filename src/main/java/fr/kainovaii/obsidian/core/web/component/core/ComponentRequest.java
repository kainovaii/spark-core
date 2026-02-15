package fr.kainovaii.obsidian.core.web.component.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentRequest
{
    private String componentId;
    private String action;
    private Map<String, Object> state = new HashMap<>();
    private List<Object> params = new ArrayList<>();

    public String getComponentId() { return componentId; }
    public void setComponentId(String componentId) { this.componentId = componentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getState() { return state; }
    public void setState(Map<String, Object> state) { this.state = state; }

    public List<Object> getParams() { return params; }
    public void setParams(List<Object> params) { this.params = params; }
}