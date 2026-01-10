package io.carloship.jouwblog.response.impl;

import java.util.Map;

public interface HashResponse {

    Object fromMap(Map<String, String> map);

    Map<String, String> toMap();

}
