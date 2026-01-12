package io.carloship.jouwblog.response;

import java.util.Map;

public interface HashResponse {

    Object fromMap(Map<String, String> map);

    Map<String, String> toMap();

}
