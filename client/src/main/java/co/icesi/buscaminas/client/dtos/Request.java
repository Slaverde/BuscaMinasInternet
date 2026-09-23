package co.icesi.buscaminas.client.dtos;

import java.util.HashMap;
import java.util.Map;

public class Request {
    public String action;
    public Map<String, String> data;

    public Request(String action) {
        this.action = action;
        this.data = new HashMap<>();
    }

    public Request with(String key, int value) {
        data.put(key, String.valueOf(value));
        return this;
    }
}
