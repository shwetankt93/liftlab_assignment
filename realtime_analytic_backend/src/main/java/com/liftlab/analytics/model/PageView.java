package com.liftlab.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Page view metric
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageView {
    
    private String url;
    private Long viewCount;
}

