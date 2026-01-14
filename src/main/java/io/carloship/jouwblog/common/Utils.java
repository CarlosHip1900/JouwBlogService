package io.carloship.jouwblog.common;

import java.util.Collections;
import java.util.List;

public class Utils {

    public static <T> List<T> getPage(List<T> sourceList, int pageNumber, int pageSize) {
        if (pageSize <= 0 || pageNumber <= 0) {
            throw new IllegalArgumentException("Invalid page size or page number");
        }

        int fromIndex = (pageNumber - 1) * pageSize;
        if (sourceList == null || fromIndex >= sourceList.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + pageSize, sourceList.size());

        return sourceList.subList(fromIndex, toIndex);
    }

}
