package io.carloship.jouwblog.response;

import java.util.Map;

public interface MappedResponse {

    Object fromMap(Map<String, String> map);

    Map<String, String> toMap();

}
