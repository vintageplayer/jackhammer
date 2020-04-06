package com.olacabs.jackhammer.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
public class GitLabGroup extends AbstractModel {
    @JsonIgnore
    private String links_avatar;
    @JsonIgnore
    private String description;

    private String links_repositories;
    List<BitBucketProject> bitBucketProjects = new ArrayList<BitBucketProject>();
}
