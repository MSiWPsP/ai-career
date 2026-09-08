package com.xucheng.aicareer.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResultVO<T> {

    private List<T> records;
    private Long total;
}
