package com.lagab.eventz.app.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailRequest {
    String to;
    String subject;
    String htmlContent;
}
