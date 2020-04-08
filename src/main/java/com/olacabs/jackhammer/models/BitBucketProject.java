package com.olacabs.jackhammer.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BitBucketProject extends AbstractModel {

    @JsonIgnore
    private String slug;
    @JsonIgnore
    private boolean has_wiki;
    @JsonIgnore
    private String created_on;
    @JsonIgnore
    private String updated_on;
    @JsonIgnore
    private String description;
    @JsonProperty("public")
    private Boolean is_Private;

    private String links_clonse_ssh;
    private String links_clonse_https;
    private String links_html;
    private String web_url;
}
