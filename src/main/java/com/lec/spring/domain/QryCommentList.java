package com.lec.spring.domain;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class QryCommentList extends QryResult{

    @ToString.Exclude
    @JsonProperty("data")
    List<Comment> list;
}








